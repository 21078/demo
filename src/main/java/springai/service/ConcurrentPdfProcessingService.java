package springai.service;

import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import springai.util.LocalCache;
import springai.util.UmlUtil;
import springai.work_const.Const;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ConcurrentPdfProcessingService {

    @Resource
    private ChatClient chatClient;

    // 线程池 - 静态内部变量（使用ThreadPoolExecutor更安全）
    private static final ExecutorService threadPool = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(), // 核心线程数
        Runtime.getRuntime().availableProcessors() * 2, // 最大线程数
        60L, // 空闲线程存活时间
        TimeUnit.SECONDS, // 时间单位
        new LinkedBlockingQueue<>(1000), // 有界任务队列
        new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "pdf-worker-" + threadNumber.getAndIncrement());
                t.setDaemon(false); // 非守护线程
                t.setPriority(Thread.NORM_PRIORITY); // 正常优先级
                return t;
            }
        },
        new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用线程执行
    );

    /**
     * 并发处理PDF页面组
     */
    public CompletableFuture<PdfProcessingResult> processPdfConcurrently(
            List<String> pageGroups, String sessionId) {

        try {
            // 初始化线程协调器（创建CountDownLatch并保存到LocalCache）
            initializeThreadCoordinator(pageGroups.size(), sessionId);

            // 存储每个线程的处理结果
            List<CompletableFuture<GroupResult>> futures = new ArrayList<>();

            // 为每个页面组创建处理任务
            for (int i = 0; i < pageGroups.size(); i++) {
                int groupIndex = i;
                String groupContent = pageGroups.get(i);

                CompletableFuture<GroupResult> future = CompletableFuture.supplyAsync(
                    () -> processGroup(groupContent, groupIndex, sessionId),
                    threadPool
                )
                .exceptionally(throwable -> {
                    throw new RuntimeException("处理页面组" + groupIndex + "时发生错误: " + throwable.getMessage(), throwable);
                });

                futures.add(future);
            }

            // 等待所有任务完成
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<GroupResult> results = futures.stream()
                                .map(CompletableFuture::join)
                                .toList();

                        return generateResult(results, sessionId);
                    });

        } catch (Exception e) {
            throw new RuntimeException("并发PDF处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化线程协调器 - 创建所有CountDownLatch并保存到LocalCache
     */
    private void initializeThreadCoordinator(int totalGroups, String sessionId) {
        // 为每个组创建CountDownLatch（包括第一个组，简化逻辑）
        for (int i = 0; i < totalGroups; i++) {
            CountDownLatch latch = new CountDownLatch(1);
            LocalCache.put(sessionId + Const.COUNTDOWN_PRE + i, latch);
        }
    }

    /**
     * 处理单个页面组
     */
    private GroupResult processGroup(String groupContent, int groupIndex, String sessionId) {
        try {
            // 如果不是第一个组，等待前一个组完成
            if (groupIndex > 0) {
                // 从LocalCache获取前一个组的CountDownLatch
                CountDownLatch previousLatch = (CountDownLatch) LocalCache.get(sessionId + Const.COUNTDOWN_PRE + (groupIndex - 1));
                if (previousLatch != null) {
                    previousLatch.await(); // 等待前一个组完成
                }
            }

            // 获取前文总结（除了第一个组）
            String previousSummary = "";
            if (groupIndex > 0) {
                Object prevSummaryObj = LocalCache.get(sessionId + Const.CONTEXT_PRE + (groupIndex - 1));
                previousSummary = prevSummaryObj != null ? prevSummaryObj.toString() : "";
            }

            // 生成当前组总结（只总结自己的内容，前文只作为参考）
            String groupSummary = generateGroupSummary(groupContent, previousSummary, groupIndex);

            // 保存当前组总结到LocalCache
            LocalCache.put(sessionId + Const.CONTEXT_PRE + groupIndex, groupSummary);

            // 标记当前组完成 - 释放当前组的CountDownLatch（下一个组只需要前文总结）
            CountDownLatch currentLatch = (CountDownLatch) LocalCache.get(sessionId + Const.COUNTDOWN_PRE + groupIndex);
            if (currentLatch != null) {
                currentLatch.countDown(); // 释放锁，让下一个组可以继续
            }

            // 生成PlantUML代码
            String plantUmlCode = UmlUtil.convertTextToPlantUml(chatClient, groupSummary);

            // 保存PlantUML代码到LocalCache
            LocalCache.put(sessionId + Const.UMLCODE_PRE + groupIndex, plantUmlCode);

            // 生成图片
            byte[] imageData = UmlUtil.convertPlantUmlToImage(plantUmlCode);

            // 保存图片到LocalCache
            LocalCache.put(sessionId + Const.IMAGE_PRE + groupIndex, imageData);

            return new GroupResult(groupIndex, groupSummary, plantUmlCode, imageData);

        } catch (Exception e) {
            throw new RuntimeException("处理页面组" + groupIndex + "失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成页面组总结
     */
    private String generateGroupSummary(String groupContent, String previousSummary, int groupIndex) {
        String promptText;
        if (previousSummary.isEmpty()) {
            // 第一个组：直接总结自己的内容
            promptText = "请总结以下内容：\n" + groupContent;
        } else {
            // 后续组：参考前文总结，但只总结自己的内容
            promptText = "前文总结供参考：" + previousSummary + "\n\n" +
                        "现在请总结以下内容（只需总结这部分内容，不要重复前文）：\n" + groupContent;
        }

        Prompt prompt = new Prompt(promptText);
        return chatClient.prompt(prompt).call().content();
    }

    /**
     * 生成结果（不需要最终汇总，直接返回各组结果）
     */
    private PdfProcessingResult generateResult(List<GroupResult> groupResults, String sessionId) {
        try {
            // 收集所有图片
            List<byte[]> allImages = new ArrayList<>();

            for (GroupResult result : groupResults) {
                if (result.imageData != null && result.imageData.length > 0) {
                    allImages.add(result.imageData);
                }
            }

            // 注意：这里不清理缓存，由控制器在PDF生成完成后统一清理
            // 这样可以确保在处理过程中缓存数据可用

            // 返回第一个组的结果作为代表（或者可以返回组合的PlantUML）
            String representativeSummary = groupResults.isEmpty() ? "" : groupResults.get(0).summary;
            String representativePlantUml = groupResults.isEmpty() ? "" : groupResults.get(0).plantUmlCode;

            return new PdfProcessingResult(representativeSummary, representativePlantUml, allImages);

        } catch (Exception e) {
            throw new RuntimeException("生成结果失败: " + e.getMessage(), e);
        }
    }


    
    /**
     * 清理会话缓存（公共方法，供控制器调用）
     * 在PDF转换完成后立即清理临时缓存，保留最终结果
     */
    public void cleanupSessionCache(String sessionId) {
        // 清理所有以sessionId开头的缓存项
        int clearedCount = LocalCache.removeByPattern(sessionId + "*");
        System.out.println("已清理会话缓存: " + sessionId + ", 清理条目数: " + clearedCount);
    }

    
    /**
     * 组处理结果
     */
    @Data
    @AllArgsConstructor
    @Builder
    public static class GroupResult {
        public final int groupIndex;
        public final String summary;
        public final String plantUmlCode;
        public final byte[] imageData;
    }

    /**
     * PDF处理最终结果
     */
    @Data
    @AllArgsConstructor
    @Builder
    public static class PdfProcessingResult {
        public final String finalSummary;
        public final String finalPlantUmlCode;
        public final List<byte[]> allImages;
    }

    /**
     * 关闭线程池
     */
    public static void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
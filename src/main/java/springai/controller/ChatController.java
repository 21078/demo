package springai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
// import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import springai.entity.Session;
import springai.service.SessionService;
import springai.util.PdfUtil;
import springai.util.UmlUtil;

@Tag(name = "聊天API")
@RestController
@RequestMapping("/ai")
public class ChatController {
    @Resource
    private SessionService sessionService;
    @Resource
    private ChatClient chatClient;

    // @Resource 
    // private StringRedisTemplate stringRedisTemplate;





    /**
     * 普通响应
     * 
     * @param message
     * @return
     */
    @Operation(description = "普通响应")
    @GetMapping("/call")
    public String generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return chatClient.prompt("你是ai助理小红").user(message).call().content();

        // var prompt = new Prompt(new UserMessage(message));
        // ChatResponse response = chatModel.call(prompt);
        // return Map.of("generation", response.getResult().getOutput().getText());
    }

    /**
     * 流式响应
     * 
     * @param message
     * @return
     */
    @Operation(description = "流式响应")
    @GetMapping(value = "/stream", produces = "text/html;charset=UTF-8")
    public Flux<String> generateStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message,
            @RequestParam(value = "sessionId", defaultValue = "0") Long sessionId) {

        // 从历史会话提取信息
        List<Session> sessions = sessionService.list(
                new LambdaQueryWrapper<Session>().eq(Session::getSessionId, sessionId)
                        .orderByAsc(Session::getCreateTime));
        List<Message> messages = sessions.stream()
                .map(session -> session.getRole() == 0 ? (Message) new UserMessage(session.getContent())
                        : (Message) new AssistantMessage(session.getContent()))
                .toList();

        // 保存当前用户聊天记录
        Session session = Session.builder().sessionId(sessionId).role(0).content(message)
                .createTime(LocalDateTime.now()).build();
        sessionService.save(session);

        // 拼接AI助理聊天记录
        StringBuilder assistantContent = new StringBuilder();

        // 返回流式响应
        Flux<String> stream = chatClient.prompt("你是ai助理小红").user(message).messages(messages).stream().content();

        return stream.doOnNext(s -> assistantContent.append(s)).doOnTerminate(() -> {
            // 保存AI助理聊天记录
            Session assistantSession = Session.builder().sessionId(sessionId).role(1)
                    .content(assistantContent.toString()).createTime(LocalDateTime.now()).build();
            sessionService.save(assistantSession);

        });
    }

    /**
     * PDF智能处理 - 提取文本、AI总结、生成PlantUML并创建新PDF
     * @param pdfFile 上传的PDF文件
     * @return 处理结果信息
     */
    @Operation(description = "PDF智能处理")
    @PostMapping("/pdf")
    public Map<String, Object> processPdf(@RequestPart("file") MultipartFile pdfFile) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 验证文件
            if (pdfFile == null || pdfFile.isEmpty()) {
                throw new IllegalArgumentException("请上传PDF文件");
            }

            if (!pdfFile.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                throw new IllegalArgumentException("请上传PDF格式的文件");
            }

            // 2. 创建临时文件
            Path tempFile = Files.createTempFile("upload-", ".pdf");
            Files.copy(pdfFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            // 3. 提取PDF文本内容
            List<String> pageTexts = PdfUtil.extractTextFromPdf(tempFile.toFile());

            // 4. 合并所有页面文本
            StringBuilder allText = new StringBuilder();
            for (int i = 0; i < pageTexts.size(); i++) {
                allText.append("第").append(i + 1).append("页内容：").append(pageTexts.get(i)).append("\n\n");
            }

            // 5. 使用AI总结文本
            String summary = generateSummary(allText.toString(), "请总结以下内容");

            // 6. 生成PlantUML
            String plantUmlCode = UmlUtil.convertTextToPlantUml(chatClient, summary);

            // 7. 创建新的PDF，包含PlantUML图片
            byte[] plantUmlImage = UmlUtil.convertPlantUmlToImage(plantUmlCode);

            // 8. 生成包含图片的新PDF
            List<byte[]> images = new ArrayList<>();
            if (plantUmlImage != null) {
                images.add(plantUmlImage);
            }

            byte[] newPdfBytes = PdfUtil.createPdfFromImagesToBytes(images);

            // 9. 清理临时文件
            Files.deleteIfExists(tempFile);

            // 10. 返回结果
            result.put("success", true);
            result.put("pageCount", pageTexts.size());
            result.put("summary", summary);
            result.put("plantUmlCode", plantUmlCode);

            // 将PDF字节数组转换为Base64编码的字符串
            if (newPdfBytes != null && newPdfBytes.length > 0) {
                String base64Pdf = java.util.Base64.getEncoder().encodeToString(newPdfBytes);
                result.put("processedPdf", base64Pdf);
            } else {
                result.put("processedPdf", null);
            }

            result.put("message", "PDF处理完成");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }


    //文字总结(可携带上文)：
    private String generateSummary(String text,String message) {
        Prompt prompt = new Prompt("前文："+message+"，请帮我总结");
        return chatClient.prompt(prompt).user(text).call().content();
    }

}

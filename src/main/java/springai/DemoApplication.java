package springai;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springai.service.ConcurrentPdfProcessingService;

@SpringBootApplication(exclude = {
    com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAudioSpeechAutoConfiguration.class,
    com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAudioTranscriptionAutoConfiguration.class
})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @PreDestroy
    public void cleanup() {
        // 关闭线程池
        ConcurrentPdfProcessingService.shutdown();
    }

}

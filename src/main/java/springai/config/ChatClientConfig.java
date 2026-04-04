package springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
@Configuration
public class ChatClientConfig {
    @Bean  
    public ChatClient chatClient(DashScopeChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}

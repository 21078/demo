package springai.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import springai.service.SessionService;

@Tag(name ="聊天API")
@RestController
public class ChatController {
    @Resource
    private SessionService sessionService;
    @Resource
    private DashScopeChatModel chatModel;
    @Resource
    private ChatClient chatClient;

    
    /**
     * 普通响应
     * @param message
     * @return
     */
    @Operation  (description = "普通响应")
    @GetMapping("/ai/generate")
    public String generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        System.out.println(sessionService.getById(1));
        return chatClient.prompt("你是ai助理小红").user(message).call().content();


        // var prompt = new Prompt(new UserMessage(message));
        // ChatResponse response = chatModel.call(prompt);
        // return Map.of("generation", response.getResult().getOutput().getText());
    }

    /**
     * 流式响应
     * @param message
     * @return
     */
    @Operation  (description = "流式响应")
    @GetMapping(value =  "/ai/generateStream",produces = "text/html;charset=UTF-8")
	public Flux<String> generateStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {


        return chatClient.prompt("你是ai助理小红").user(message).stream().content();        
    }
}

package springai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import springai.entity.Session;
import springai.service.SessionService;

@Tag(name = "聊天API")
@RestController( value = "/ai")
public class ChatController {
    @Resource
    private SessionService sessionService;
    @Resource
    private ChatClient chatClient;

    @Resource 
    private StringRedisTemplate stringRedisTemplate;





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
     * 功能测试响应
     */
    @Operation(description = "功能测试响应")
    @GetMapping("/test")
    public String test() {

        stringRedisTemplate.opsForValue().set("test", "test");
        
        return "success";
    }
}

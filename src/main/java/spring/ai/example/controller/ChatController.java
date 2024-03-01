package spring.ai.example.controller;

import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
public class ChatController {

    private final OpenAiChatClient chatClient;

    @Autowired
    public ChatController(OpenAiChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/ai/chat")
    public Map chat(@RequestParam(value = "message", defaultValue = "给我讲一个中文笑话") String message) {
        return Map.of("generation", chatClient.call(message));
    }

    @GetMapping("/ai/stream_chat")
    public Flux<ChatResponse> chatStream(@RequestParam(value = "message", defaultValue = "给我讲一个中文笑话") String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        return chatClient.stream(prompt);
    }
}

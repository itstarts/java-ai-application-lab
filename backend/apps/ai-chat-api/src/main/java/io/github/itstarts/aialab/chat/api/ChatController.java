package io.github.itstarts.aialab.chat.api;

import io.github.itstarts.aialab.chat.api.dto.ChatRequest;
import io.github.itstarts.aialab.chat.api.dto.ChatResponse;
import io.github.itstarts.aialab.chat.api.dto.HealthResponse;
import io.github.itstarts.aialab.chat.application.ChatResult;
import io.github.itstarts.aialab.chat.application.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", Instant.now());
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String normalizedMessage = ChatMessageNormalizer.trim(request.message());
        ChatResult result = chatService.chat(normalizedMessage);
        return new ChatResponse(result.provider(), result.model(), result.content(), result.traceId());
    }
}

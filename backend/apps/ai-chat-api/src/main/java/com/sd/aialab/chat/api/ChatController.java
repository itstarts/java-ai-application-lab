package com.sd.aialab.chat.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class ChatController {

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", Instant.now());
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String normalizedMessage = request.message().trim();
        return new ChatResponse("stub", "Echo: " + normalizedMessage);
    }

    public record ChatRequest(
            @NotBlank(message = "message must not be blank")
            @Size(max = 4000, message = "message must be at most 4000 characters")
            String message
    ) {
    }

    public record ChatResponse(String provider, String content) {
    }

    public record HealthResponse(String status, Instant timestamp) {
    }
}

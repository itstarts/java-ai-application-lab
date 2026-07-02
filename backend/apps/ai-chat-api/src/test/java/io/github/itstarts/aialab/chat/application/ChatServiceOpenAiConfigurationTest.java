package io.github.itstarts.aialab.chat.application;

import io.github.itstarts.aialab.chat.provider.config.ChatProviderProperties;
import io.github.itstarts.aialab.chat.provider.openai.OpenAiChatProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatServiceOpenAiConfigurationTest {

    @Test
    void mapsOpenAiMissingApiKeyToProviderError() {
        ChatProviderProperties properties = new ChatProviderProperties(
                "openai",
                "gpt-test",
                "https://api.example.test/v1",
                null,
                Duration.ofSeconds(3)
        );
        ChatService chatService = new ChatService(
                properties,
                List.of(new OpenAiChatProvider(properties)),
                new TraceIdGenerator()
        );

        ApiException exception = assertThrows(ApiException.class, () -> chatService.chat("hello"));

        assertEquals("AI_PROVIDER_ERROR", exception.getCode());
        assertEquals("模型服务返回错误，请稍后重试", exception.getUserMessage());
        assertEquals(502, exception.getStatus().value());
        assertNotNull(exception.getTraceId());
    }
}

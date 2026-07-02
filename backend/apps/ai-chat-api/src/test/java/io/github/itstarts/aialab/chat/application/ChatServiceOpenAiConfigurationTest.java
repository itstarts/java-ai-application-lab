package io.github.itstarts.aialab.chat.application;

import io.github.itstarts.aialab.chat.provider.config.ChatProviderProperties;
import io.github.itstarts.aialab.chat.provider.openai.OpenAiChatCompletionRequestFactory;
import io.github.itstarts.aialab.chat.provider.openai.OpenAiChatCompletionResponseMapper;
import io.github.itstarts.aialab.chat.provider.openai.OpenAiChatHttpClient;
import io.github.itstarts.aialab.chat.provider.openai.OpenAiChatProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        ObjectMapper objectMapper = new ObjectMapper();
        ChatService chatService = new ChatService(
                properties,
                List.of(new OpenAiChatProvider(
                        properties,
                        unusedHttpClient(),
                        new OpenAiChatCompletionRequestFactory(properties, objectMapper),
                        new OpenAiChatCompletionResponseMapper(objectMapper)
                )),
                new TraceIdGenerator()
        );

        ApiException exception = assertThrows(ApiException.class, () -> chatService.chat("hello"));

        assertEquals("AI_PROVIDER_ERROR", exception.getCode());
        assertEquals("模型服务返回错误，请稍后重试", exception.getUserMessage());
        assertEquals(502, exception.getStatus().value());
        assertNotNull(exception.getTraceId());
    }

    private OpenAiChatHttpClient unusedHttpClient() {
        return request -> {
            throw new AssertionError("openai http client must not be called when api key is missing");
        };
    }
}

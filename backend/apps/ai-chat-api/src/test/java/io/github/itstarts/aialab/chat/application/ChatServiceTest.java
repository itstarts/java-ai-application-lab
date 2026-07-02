package io.github.itstarts.aialab.chat.application;

import io.github.itstarts.aialab.chat.provider.ChatProvider;
import io.github.itstarts.aialab.chat.provider.ProviderChatRequest;
import io.github.itstarts.aialab.chat.provider.ProviderChatResponse;
import io.github.itstarts.aialab.chat.provider.config.ChatProviderProperties;
import io.github.itstarts.aialab.chat.provider.error.ChatProviderErrorType;
import io.github.itstarts.aialab.chat.provider.error.ChatProviderException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatServiceTest {

    @Test
    void mapsProviderExceptionAndPreservesCause() {
        ChatProviderException providerException = new ChatProviderException("provider failed");
        ChatService chatService = chatServiceThrowing(providerException);

        ApiException exception = assertThrows(ApiException.class, () -> chatService.chat("hello"));

        assertEquals("AI_PROVIDER_ERROR", exception.getCode());
        assertEquals("模型服务返回错误，请稍后重试", exception.getUserMessage());
        assertEquals(502, exception.getStatus().value());
        assertNotNull(exception.getTraceId());
        assertSame(providerException, exception.getCause());
    }

    @Test
    void mapsTypedProviderExceptionToSpecificError() {
        ChatProviderException providerException = new ChatProviderException(ChatProviderErrorType.REQUEST_TIMEOUT, "provider timeout");
        ChatService chatService = chatServiceThrowing(providerException);

        ApiException exception = assertThrows(ApiException.class, () -> chatService.chat("hello"));

        assertEquals("AI_REQUEST_TIMEOUT", exception.getCode());
        assertEquals("模型服务响应超时", exception.getUserMessage());
        assertEquals(504, exception.getStatus().value());
        assertNotNull(exception.getTraceId());
        assertSame(providerException, exception.getCause());
    }

    @Test
    void mapsUnexpectedRuntimeExceptionWithTraceAndCause() {
        IllegalStateException providerException = new IllegalStateException("unexpected provider failure");
        ChatService chatService = chatServiceThrowing(providerException);

        ApiException exception = assertThrows(ApiException.class, () -> chatService.chat("hello"));

        assertEquals("INTERNAL_SERVER_ERROR", exception.getCode());
        assertEquals("服务暂时不可用，请稍后重试", exception.getUserMessage());
        assertEquals(500, exception.getStatus().value());
        assertNotNull(exception.getTraceId());
        assertSame(providerException, exception.getCause());
    }

    private ChatService chatServiceThrowing(RuntimeException exception) {
        return new ChatService(
                new ChatProviderProperties("failing-provider", "mock-chat", null, null, Duration.ofSeconds(3)),
                List.of(new ChatProvider() {
                    @Override
                    public String providerName() {
                        return "failing-provider";
                    }

                    @Override
                    public ProviderChatResponse chat(ProviderChatRequest request) {
                        throw exception;
                    }
                }),
                new TraceIdGenerator()
        );
    }
}

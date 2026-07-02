package io.github.itstarts.aialab.chat.provider.openai;

import io.github.itstarts.aialab.chat.provider.ProviderChatRequest;
import io.github.itstarts.aialab.chat.provider.config.ChatProviderProperties;
import io.github.itstarts.aialab.chat.provider.error.ChatProviderException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiChatProviderTest {

    @Test
    void providerNameIsOpenAi() {
        OpenAiChatProvider provider = new OpenAiChatProvider(properties("https://api.example.test/v1", "local-test-key"));

        assertEquals("openai", provider.providerName());
    }

    @Test
    void rejectsMissingBaseUrlBeforeAnyExternalCall() {
        OpenAiChatProvider provider = new OpenAiChatProvider(properties(null, "local-test-key"));

        ChatProviderException exception = assertThrows(ChatProviderException.class, () -> provider.chat(request()));

        assertEquals("AI_PROVIDER_ERROR", exception.getErrorType().getApiErrorType().getCode());
        assertThat(exception.getMessage())
                .contains("ai.base-url")
                .doesNotContain("local-test-key");
    }

    @Test
    void rejectsBlankBaseUrlBeforeAnyExternalCall() {
        OpenAiChatProvider provider = new OpenAiChatProvider(properties("   ", "local-test-key"));

        ChatProviderException exception = assertThrows(ChatProviderException.class, () -> provider.chat(request()));

        assertEquals("AI_PROVIDER_ERROR", exception.getErrorType().getApiErrorType().getCode());
        assertThat(exception.getMessage())
                .contains("ai.base-url")
                .doesNotContain("local-test-key");
    }

    @Test
    void rejectsMissingApiKeyBeforeAnyExternalCall() {
        OpenAiChatProvider provider = new OpenAiChatProvider(properties("https://api.example.test/v1", null));

        ChatProviderException exception = assertThrows(ChatProviderException.class, () -> provider.chat(request()));

        assertEquals("AI_PROVIDER_ERROR", exception.getErrorType().getApiErrorType().getCode());
        assertThat(exception.getMessage())
                .contains("ai.api-key")
                .doesNotContain("https://api.example.test/v1");
    }

    @Test
    void rejectsBlankApiKeyBeforeAnyExternalCall() {
        OpenAiChatProvider provider = new OpenAiChatProvider(properties("https://api.example.test/v1", "   "));

        ChatProviderException exception = assertThrows(ChatProviderException.class, () -> provider.chat(request()));

        assertEquals("AI_PROVIDER_ERROR", exception.getErrorType().getApiErrorType().getCode());
        assertThat(exception.getMessage())
                .contains("ai.api-key")
                .doesNotContain("https://api.example.test/v1");
    }

    private ChatProviderProperties properties(String baseUrl, String apiKey) {
        return new ChatProviderProperties("openai", "gpt-test", baseUrl, apiKey, Duration.ofSeconds(3));
    }

    private ProviderChatRequest request() {
        return new ProviderChatRequest("trace_test", "gpt-test", "hello", Duration.ofSeconds(3));
    }
}

package io.github.itstarts.aialab.chat.provider.openai;

import io.github.itstarts.aialab.chat.provider.ProviderChatRequest;
import io.github.itstarts.aialab.chat.provider.config.ChatProviderProperties;
import io.github.itstarts.aialab.chat.provider.error.ChatProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiChatProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void providerNameIsOpenAi() {
        OpenAiChatProvider provider = provider(properties("https://api.example.test/v1", "local-test-key"), successfulClient());

        assertEquals("openai", provider.providerName());
    }

    @Test
    void sendsChatCompletionRequestAndMapsFirstChoiceContent() throws Exception {
        RecordingOpenAiChatHttpClient httpClient = successfulClient();
        OpenAiChatProvider provider = provider(
                new ChatProviderProperties(
                        "openai",
                        "gpt-test",
                        "https://api.example.test/v1",
                        "local-test-key",
                        Duration.ofSeconds(7)
                ),
                httpClient
        );

        var response = provider.chat(new ProviderChatRequest("trace_test", "gpt-test", "hello model", Duration.ofSeconds(7)));

        assertEquals("model answer", response.content());
        assertEquals("https://api.example.test/v1/chat/completions", httpClient.request().url());
        assertTrue(
                httpClient.request().hasHeader("Content-Type", "application/json"),
                "content type header must be application/json"
        );
        assertTrue(
                httpClient.request().hasHeader("Authorization", "Bearer local-test-key"),
                "authorization header must use bearer api key"
        );
        assertEquals(Duration.ofSeconds(7), httpClient.request().timeout());

        JsonNode body = objectMapper.readTree(httpClient.request().body());
        assertEquals("gpt-test", body.get("model").asText());
        assertEquals("user", body.get("messages").get(0).get("role").asText());
        assertEquals("hello model", body.get("messages").get(0).get("content").asText());
    }

    @Test
    void normalizesTrailingSlashWhenBuildingChatCompletionsUrl() {
        RecordingOpenAiChatHttpClient httpClient = successfulClient();
        OpenAiChatProvider provider = provider(
                properties("https://api.example.test/v1/", "local-test-key"),
                httpClient
        );

        provider.chat(request());

        assertEquals("https://api.example.test/v1/chat/completions", httpClient.request().url());
    }

    @Test
    void rejectsMissingBaseUrlBeforeAnyExternalCall() {
        RecordingOpenAiChatHttpClient httpClient = successfulClient();
        OpenAiChatProvider provider = provider(properties(null, "local-test-key"), httpClient);

        ChatProviderException exception = assertThrows(ChatProviderException.class, () -> provider.chat(request()));

        assertEquals("AI_PROVIDER_ERROR", exception.getErrorType().getApiErrorType().getCode());
        assertThat(exception.getMessage())
                .contains("ai.base-url")
                .doesNotContain("local-test-key");
        assertThat(httpClient.called()).isFalse();
    }

    @Test
    void rejectsBlankBaseUrlBeforeAnyExternalCall() {
        RecordingOpenAiChatHttpClient httpClient = successfulClient();
        OpenAiChatProvider provider = provider(properties("   ", "local-test-key"), httpClient);

        ChatProviderException exception = assertThrows(ChatProviderException.class, () -> provider.chat(request()));

        assertEquals("AI_PROVIDER_ERROR", exception.getErrorType().getApiErrorType().getCode());
        assertThat(exception.getMessage())
                .contains("ai.base-url")
                .doesNotContain("local-test-key");
        assertThat(httpClient.called()).isFalse();
    }

    @Test
    void rejectsMissingApiKeyBeforeAnyExternalCall() {
        RecordingOpenAiChatHttpClient httpClient = successfulClient();
        OpenAiChatProvider provider = provider(properties("https://api.example.test/v1", null), httpClient);

        ChatProviderException exception = assertThrows(ChatProviderException.class, () -> provider.chat(request()));

        assertEquals("AI_PROVIDER_ERROR", exception.getErrorType().getApiErrorType().getCode());
        assertThat(exception.getMessage())
                .contains("ai.api-key")
                .doesNotContain("https://api.example.test/v1");
        assertThat(httpClient.called()).isFalse();
    }

    @Test
    void rejectsBlankApiKeyBeforeAnyExternalCall() {
        RecordingOpenAiChatHttpClient httpClient = successfulClient();
        OpenAiChatProvider provider = provider(properties("https://api.example.test/v1", "   "), httpClient);

        ChatProviderException exception = assertThrows(ChatProviderException.class, () -> provider.chat(request()));

        assertEquals("AI_PROVIDER_ERROR", exception.getErrorType().getApiErrorType().getCode());
        assertThat(exception.getMessage())
                .contains("ai.api-key")
                .doesNotContain("https://api.example.test/v1");
        assertThat(httpClient.called()).isFalse();
    }

    private ChatProviderProperties properties(String baseUrl, String apiKey) {
        return new ChatProviderProperties("openai", "gpt-test", baseUrl, apiKey, Duration.ofSeconds(3));
    }

    private ProviderChatRequest request() {
        return new ProviderChatRequest("trace_test", "gpt-test", "hello", Duration.ofSeconds(3));
    }

    private OpenAiChatProvider provider(ChatProviderProperties properties, OpenAiChatHttpClient httpClient) {
        return new OpenAiChatProvider(
                properties,
                httpClient,
                new OpenAiChatCompletionRequestFactory(properties, objectMapper),
                new OpenAiChatCompletionResponseMapper(objectMapper)
        );
    }

    private RecordingOpenAiChatHttpClient successfulClient() {
        return new RecordingOpenAiChatHttpClient("""
                {"choices":[{"message":{"content":"model answer"}}]}
                """);
    }

    private static final class RecordingOpenAiChatHttpClient implements OpenAiChatHttpClient {

        private final String responseBody;
        private OpenAiChatHttpRequest request;

        private RecordingOpenAiChatHttpClient(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public OpenAiChatHttpResponse post(OpenAiChatHttpRequest request) {
            this.request = request;
            return new OpenAiChatHttpResponse(200, responseBody);
        }

        private OpenAiChatHttpRequest request() {
            return request;
        }

        private boolean called() {
            return request != null;
        }
    }
}

package io.github.itstarts.aialab.chat.provider.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.itstarts.aialab.chat.provider.ProviderChatRequest;
import io.github.itstarts.aialab.chat.provider.config.ChatProviderProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiChatCompletionRequestFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildsChatCompletionsHttpRequest() throws Exception {
        OpenAiChatCompletionRequestFactory factory = new OpenAiChatCompletionRequestFactory(
                properties("https://api.example.test/v1", "local-test-key"),
                objectMapper
        );

        OpenAiChatHttpRequest request = factory.build(new ProviderChatRequest(
                "trace_test",
                "gpt-test",
                "hello model",
                Duration.ofSeconds(7)
        ));

        assertEquals("https://api.example.test/v1/chat/completions", request.url());
        assertTrue(request.hasHeader("Content-Type", "application/json"));
        assertTrue(request.hasHeader("Authorization", "Bearer local-test-key"));
        assertEquals(Duration.ofSeconds(7), request.timeout());

        JsonNode body = objectMapper.readTree(request.body());
        assertEquals("gpt-test", body.get("model").asText());
        assertEquals("user", body.get("messages").get(0).get("role").asText());
        assertEquals("hello model", body.get("messages").get(0).get("content").asText());
    }

    @Test
    void normalizesBaseUrlTrailingSlash() throws Exception {
        OpenAiChatCompletionRequestFactory factory = new OpenAiChatCompletionRequestFactory(
                properties("https://api.example.test/v1/", "local-test-key"),
                objectMapper
        );

        OpenAiChatHttpRequest request = factory.build(new ProviderChatRequest(
                "trace_test",
                "gpt-test",
                "hello",
                Duration.ofSeconds(3)
        ));

        assertEquals("https://api.example.test/v1/chat/completions", request.url());
    }

    @Test
    void stringRepresentationDoesNotExposeSensitiveRequestData() throws Exception {
        OpenAiChatCompletionRequestFactory factory = new OpenAiChatCompletionRequestFactory(
                properties("https://api.example.test/v1", "local-test-key"),
                objectMapper
        );

        OpenAiChatHttpRequest request = factory.build(new ProviderChatRequest(
                "trace_test",
                "gpt-test",
                "hello model",
                Duration.ofSeconds(7)
        ));

        assertThat(request.toString())
                .doesNotContain("https://api.example.test/v1")
                .doesNotContain("Authorization")
                .doesNotContain("Bearer")
                .doesNotContain("local-test-key")
                .doesNotContain("hello model");
    }

    private ChatProviderProperties properties(String baseUrl, String apiKey) {
        return new ChatProviderProperties("openai", "gpt-test", baseUrl, apiKey, Duration.ofSeconds(3));
    }
}

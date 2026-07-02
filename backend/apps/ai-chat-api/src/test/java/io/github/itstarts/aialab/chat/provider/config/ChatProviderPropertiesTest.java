package io.github.itstarts.aialab.chat.provider.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatProviderPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void defaultsNullProviderToMock() {
        ChatProviderProperties properties = new ChatProviderProperties(null, "mock-chat", null, null, Duration.ofSeconds(3));

        assertEquals("mock", properties.provider());
    }

    @Test
    void defaultsBlankProviderToMock() {
        ChatProviderProperties properties = new ChatProviderProperties("   ", "mock-chat", null, null, Duration.ofSeconds(3));

        assertEquals("mock", properties.provider());
    }

    @Test
    void defaultsNullRequestTimeoutToThirtySeconds() {
        ChatProviderProperties properties = new ChatProviderProperties("mock", "mock-chat", null, null, null);

        assertEquals(Duration.ofSeconds(30), properties.requestTimeout());
    }

    @Test
    void keepsNullChatModelAsMissingConfiguration() {
        ChatProviderProperties properties = new ChatProviderProperties("mock", null, null, null, Duration.ofSeconds(3));

        assertNull(properties.chatModel());
    }

    @Test
    void trimsOpenAiConnectionSettingsWithoutDefaultingThem() {
        ChatProviderProperties properties = new ChatProviderProperties(
                " openai ",
                " gpt-test ",
                " https://api.example.test/v1/ ",
                " local-test-key ",
                Duration.ofSeconds(3)
        );

        assertEquals("openai", properties.provider());
        assertEquals("gpt-test", properties.chatModel());
        assertEquals("https://api.example.test/v1/", properties.baseUrl());
        assertEquals("local-test-key", properties.apiKey());
    }

    @Test
    void redactsSensitiveConnectionSettingsFromStringRepresentation() {
        ChatProviderProperties properties = new ChatProviderProperties(
                "openai",
                "gpt-test",
                "https://api.example.test/v1",
                "local-test-secret",
                Duration.ofSeconds(3)
        );

        String output = properties.toString();

        org.assertj.core.api.Assertions.assertThat(output)
                .contains("baseUrl=<redacted>")
                .contains("apiKey=<redacted>")
                .doesNotContain("https://api.example.test/v1")
                .doesNotContain("local-test-secret");
    }

    @Test
    void rejectsZeroRequestTimeout() {
        assertRequestTimeoutViolation(Duration.ZERO);
    }

    @Test
    void rejectsNegativeRequestTimeout() {
        assertRequestTimeoutViolation(Duration.ofSeconds(-1));
    }

    private void assertRequestTimeoutViolation(Duration requestTimeout) {
        ChatProviderProperties properties = new ChatProviderProperties("mock", "mock-chat", null, null, requestTimeout);

        Set<ConstraintViolation<ChatProviderProperties>> violations = validator.validate(properties);

        assertEquals(1, violations.size());
        assertEquals("ai.request-timeout must be positive", violations.iterator().next().getMessage());
    }
}

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
        ChatProviderProperties properties = new ChatProviderProperties(null, "mock-chat", Duration.ofSeconds(3));

        assertEquals("mock", properties.provider());
    }

    @Test
    void defaultsBlankProviderToMock() {
        ChatProviderProperties properties = new ChatProviderProperties("   ", "mock-chat", Duration.ofSeconds(3));

        assertEquals("mock", properties.provider());
    }

    @Test
    void defaultsNullRequestTimeoutToThirtySeconds() {
        ChatProviderProperties properties = new ChatProviderProperties("mock", "mock-chat", null);

        assertEquals(Duration.ofSeconds(30), properties.requestTimeout());
    }

    @Test
    void keepsNullChatModelAsMissingConfiguration() {
        ChatProviderProperties properties = new ChatProviderProperties("mock", null, Duration.ofSeconds(3));

        assertNull(properties.chatModel());
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
        ChatProviderProperties properties = new ChatProviderProperties("mock", "mock-chat", requestTimeout);

        Set<ConstraintViolation<ChatProviderProperties>> violations = validator.validate(properties);

        assertEquals(1, violations.size());
        assertEquals("ai.request-timeout must be positive", violations.iterator().next().getMessage());
    }
}

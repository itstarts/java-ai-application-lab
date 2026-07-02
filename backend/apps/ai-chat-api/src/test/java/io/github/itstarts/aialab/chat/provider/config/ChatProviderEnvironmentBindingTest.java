package io.github.itstarts.aialab.chat.provider.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "AI_PROVIDER=openai",
        "AI_CHAT_MODEL=gpt-test",
        "AI_BASE_URL=https://api.example.test/v1",
        "AI_API_KEY=local-test-key",
        "AI_REQUEST_TIMEOUT=7s"
})
class ChatProviderEnvironmentBindingTest {

    @Autowired
    private ChatProviderProperties properties;

    @Test
    void bindsOpenAiSettingsFromEnvironmentStyleProperties() {
        assertEquals("openai", properties.provider());
        assertEquals("gpt-test", properties.chatModel());
        assertEquals("https://api.example.test/v1", properties.baseUrl());
        assertEquals("local-test-key", properties.apiKey());
        assertEquals(Duration.ofSeconds(7), properties.requestTimeout());
    }
}

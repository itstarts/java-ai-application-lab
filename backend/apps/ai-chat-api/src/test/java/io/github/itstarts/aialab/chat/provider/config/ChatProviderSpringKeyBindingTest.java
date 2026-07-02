package io.github.itstarts.aialab.chat.provider.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ChatProviderSpringKeyBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsOpenAiSettingsFromSpringConfigurationKeys() {
        contextRunner
                .withPropertyValues(
                        "ai.provider=openai",
                        "ai.chat-model=gpt-test",
                        "ai.base-url=https://api.example.test/v1",
                        "ai.api-key=local-test-key",
                        "ai.request-timeout=9s"
                )
                .run(context -> {
                    ChatProviderProperties properties = context.getBean(ChatProviderProperties.class);

                    assertThat(properties.provider()).isEqualTo("openai");
                    assertThat(properties.chatModel()).isEqualTo("gpt-test");
                    assertThat(properties.baseUrl()).isEqualTo("https://api.example.test/v1");
                    assertThat(properties.apiKey()).isEqualTo("local-test-key");
                    assertThat(properties.requestTimeout()).isEqualTo(Duration.ofSeconds(9));
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ChatProviderProperties.class)
    static class PropertiesConfiguration {
    }
}

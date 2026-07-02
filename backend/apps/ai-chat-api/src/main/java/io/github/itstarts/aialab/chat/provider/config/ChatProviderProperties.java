package io.github.itstarts.aialab.chat.provider.config;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "ai")
@Validated
public record ChatProviderProperties(
        String provider,
        String chatModel,
        String baseUrl,
        String apiKey,
        @DurationMin(nanos = 1, message = "ai.request-timeout must be positive")
        Duration requestTimeout
) {

    public ChatProviderProperties {
        if (StringUtils.isBlank(provider)) {
            provider = "mock";
        } else {
            provider = StringUtils.trim(provider);
        }
        chatModel = StringUtils.trim(chatModel);
        baseUrl = StringUtils.trim(baseUrl);
        apiKey = StringUtils.trim(apiKey);
        if (requestTimeout == null) {
            requestTimeout = Duration.ofSeconds(30);
        }
    }

    @Override
    public String toString() {
        String redactedBaseUrl = StringUtils.isBlank(baseUrl) ? "missing" : "<redacted>";
        String redactedApiKey = StringUtils.isBlank(apiKey) ? "missing" : "<redacted>";
        return "ChatProviderProperties[provider=%s, chatModel=%s, baseUrl=%s, apiKey=%s, requestTimeout=%s]"
                .formatted(provider, chatModel, redactedBaseUrl, redactedApiKey, requestTimeout);
    }
}

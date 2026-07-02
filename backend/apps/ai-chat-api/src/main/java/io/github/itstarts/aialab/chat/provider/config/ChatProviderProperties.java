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
        if (requestTimeout == null) {
            requestTimeout = Duration.ofSeconds(30);
        }
    }
}

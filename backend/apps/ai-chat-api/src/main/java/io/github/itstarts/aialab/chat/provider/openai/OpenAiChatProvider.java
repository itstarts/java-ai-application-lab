package io.github.itstarts.aialab.chat.provider.openai;

import io.github.itstarts.aialab.chat.provider.ChatProvider;
import io.github.itstarts.aialab.chat.provider.ProviderChatRequest;
import io.github.itstarts.aialab.chat.provider.ProviderChatResponse;
import io.github.itstarts.aialab.chat.provider.config.ChatProviderProperties;
import io.github.itstarts.aialab.chat.provider.error.ChatProviderErrorType;
import io.github.itstarts.aialab.chat.provider.error.ChatProviderException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiChatProvider implements ChatProvider {

    private static final String PROVIDER_NAME = "openai";

    private final ChatProviderProperties properties;

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public ProviderChatResponse chat(ProviderChatRequest request) {
        validateRequiredConfiguration();
        throw new ChatProviderException(ChatProviderErrorType.PROVIDER_ERROR, "openai provider http call is not implemented");
    }

    private void validateRequiredConfiguration() {
        if (StringUtils.isBlank(properties.baseUrl())) {
            throw new ChatProviderException(
                    ChatProviderErrorType.PROVIDER_ERROR,
                    "openai provider missing required configuration: ai.base-url"
            );
        }
        if (StringUtils.isBlank(properties.apiKey())) {
            throw new ChatProviderException(
                    ChatProviderErrorType.PROVIDER_ERROR,
                    "openai provider missing required configuration: ai.api-key"
            );
        }
    }
}

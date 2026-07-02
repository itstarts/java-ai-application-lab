package io.github.itstarts.aialab.chat.provider.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.itstarts.aialab.chat.provider.ProviderChatRequest;
import io.github.itstarts.aialab.chat.provider.config.ChatProviderProperties;
import io.github.itstarts.aialab.chat.provider.openai.dto.OpenAiChatCompletionRequest;
import io.github.itstarts.aialab.chat.provider.openai.dto.OpenAiChatMessage;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiChatCompletionRequestFactory {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final ChatProviderProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiChatHttpRequest build(ProviderChatRequest request) throws JsonProcessingException {
        String body = objectMapper.writeValueAsString(new OpenAiChatCompletionRequest(
                request.model(),
                List.of(new OpenAiChatMessage("user", request.message()))
        ));
        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer " + properties.apiKey()
        );
        return new OpenAiChatHttpRequest(
                chatCompletionsUrl(),
                headers,
                body,
                request.requestTimeout()
        );
    }

    private String chatCompletionsUrl() {
        return StringUtils.stripEnd(properties.baseUrl(), "/") + CHAT_COMPLETIONS_PATH;
    }
}

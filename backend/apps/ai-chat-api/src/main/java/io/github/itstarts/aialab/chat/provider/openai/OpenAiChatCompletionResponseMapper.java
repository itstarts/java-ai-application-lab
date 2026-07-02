package io.github.itstarts.aialab.chat.provider.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.itstarts.aialab.chat.provider.ProviderChatResponse;
import io.github.itstarts.aialab.chat.provider.error.ChatProviderErrorType;
import io.github.itstarts.aialab.chat.provider.error.ChatProviderException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiChatCompletionResponseMapper {

    private final ObjectMapper objectMapper;

    public ProviderChatResponse map(OpenAiChatHttpResponse response) throws JsonProcessingException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ChatProviderException(ChatProviderErrorType.PROVIDER_ERROR, "openai provider returned non-success status");
        }

        JsonNode contentNode = objectMapper.readTree(response.body())
                .path("choices")
                .path(0)
                .path("message")
                .path("content");
        String content = contentNode.isTextual() ? contentNode.asText() : null;
        if (StringUtils.isBlank(content)) {
            throw new ChatProviderException(ChatProviderErrorType.EMPTY_RESPONSE, "openai provider returned empty content");
        }
        return new ProviderChatResponse(content);
    }
}

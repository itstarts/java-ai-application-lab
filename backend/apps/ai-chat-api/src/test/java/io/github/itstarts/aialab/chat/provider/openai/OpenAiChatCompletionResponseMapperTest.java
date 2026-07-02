package io.github.itstarts.aialab.chat.provider.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.itstarts.aialab.chat.provider.ProviderChatResponse;
import io.github.itstarts.aialab.chat.provider.error.ChatProviderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiChatCompletionResponseMapperTest {

    private final OpenAiChatCompletionResponseMapper mapper = new OpenAiChatCompletionResponseMapper(new ObjectMapper());

    @Test
    void mapsFirstChoiceMessageContent() throws Exception {
        ProviderChatResponse response = mapper.map(new OpenAiChatHttpResponse(
                200,
                "{\"choices\":[{\"message\":{\"content\":\"model answer\"}}]}"
        ));

        assertEquals("model answer", response.content());
    }

    @Test
    void mapsBlankContentToEmptyResponseError() {
        ChatProviderException exception = assertThrows(ChatProviderException.class, () -> mapper.map(new OpenAiChatHttpResponse(
                200,
                "{\"choices\":[{\"message\":{\"content\":\"   \"}}]}"
        )));

        assertEquals("AI_EMPTY_RESPONSE", exception.getErrorType().getApiErrorType().getCode());
    }
}

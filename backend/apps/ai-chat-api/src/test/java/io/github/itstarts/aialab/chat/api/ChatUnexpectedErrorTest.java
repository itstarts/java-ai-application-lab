package io.github.itstarts.aialab.chat.api;

import io.github.itstarts.aialab.chat.provider.ChatProvider;
import io.github.itstarts.aialab.chat.provider.ProviderChatRequest;
import io.github.itstarts.aialab.chat.provider.ProviderChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ai.provider=unexpected-error",
        "ai.chat-model=mock-chat"
})
@AutoConfigureMockMvc
class ChatUnexpectedErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chatMapsUnexpectedExceptionToUnifiedErrorResponse() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("服务暂时不可用，请稍后重试"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));
    }

    @TestConfiguration
    static class UnexpectedErrorProviderConfiguration {

        @Bean
        ChatProvider unexpectedErrorProvider() {
            return new ChatProvider() {
                @Override
                public String providerName() {
                    return "unexpected-error";
                }

                @Override
                public ProviderChatResponse chat(ProviderChatRequest request) {
                    throw new IllegalStateException("unexpected provider failure");
                }
            };
        }
    }
}

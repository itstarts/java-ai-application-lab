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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ai.provider=timeout-capture",
        "ai.chat-model=mock-chat",
        "ai.request-timeout=3s"
})
@AutoConfigureMockMvc
class ChatProviderTimeoutTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chatPassesConfiguredTimeoutToProvider() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("timeout=PT3S"));
    }

    @TestConfiguration
    static class TimeoutCaptureProviderConfiguration {

        @Bean
        ChatProvider timeoutCaptureProvider() {
            return new ChatProvider() {
                @Override
                public String providerName() {
                    return "timeout-capture";
                }

                @Override
                public ProviderChatResponse chat(ProviderChatRequest request) {
                    return new ProviderChatResponse("timeout=" + request.requestTimeout());
                }
            };
        }
    }
}

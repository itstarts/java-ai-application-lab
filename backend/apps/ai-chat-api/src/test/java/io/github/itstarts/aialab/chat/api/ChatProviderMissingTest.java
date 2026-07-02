package io.github.itstarts.aialab.chat.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ai.provider=missing-provider",
        "ai.chat-model=mock-chat"
})
@AutoConfigureMockMvc
class ChatProviderMissingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chatReturnsProviderNotFoundWhenProviderBeanIsMissing() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_PROVIDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("未找到模型服务"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));
    }
}

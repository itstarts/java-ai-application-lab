package io.github.itstarts.aialab.chat.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "AI_PROVIDER=openai",
        "AI_CHAT_MODEL=gpt-test",
        "AI_API_KEY=local-test-secret"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class OpenAiProviderMissingBaseUrlTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chatReturnsUnifiedProviderErrorWhenOpenAiBaseUrlIsMissing(CapturedOutput output) throws Exception {
        String userMessage = "do not log this full user input";

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + userMessage + "\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_PROVIDER_ERROR"))
                .andExpect(jsonPath("$.message").value("模型服务返回错误，请稍后重试"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));

        assertThat(output).doesNotContain("local-test-secret", userMessage);
    }
}

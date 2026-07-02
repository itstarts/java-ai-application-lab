package io.github.itstarts.aialab.chat.api;

import io.github.itstarts.aialab.chat.provider.openai.OpenAiChatHttpClient;
import io.github.itstarts.aialab.chat.provider.openai.OpenAiChatHttpRequest;
import io.github.itstarts.aialab.chat.provider.openai.OpenAiChatHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "AI_PROVIDER=openai",
        "AI_BASE_URL=https://api.example.test/v1",
        "AI_API_KEY=local-controller-key",
        "AI_CHAT_MODEL=gpt-controller-test",
        "AI_REQUEST_TIMEOUT=5s"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class OpenAiProviderSuccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chatReturnsOpenAiProviderResponse(CapturedOutput output) throws Exception {
        String userMessage = "controller success message";

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + userMessage + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("openai"))
                .andExpect(jsonPath("$.model").value("gpt-controller-test"))
                .andExpect(jsonPath("$.content").value("controller model answer"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));

        assertThat(output).doesNotContain("local-controller-key", "Authorization", "Bearer", userMessage);
    }

    @TestConfiguration
    static class OpenAiHttpClientStubConfiguration {

        @Bean
        @Primary
        OpenAiChatHttpClient openAiChatHttpClientStub() {
            return new OpenAiChatHttpClient() {
                @Override
                public OpenAiChatHttpResponse post(OpenAiChatHttpRequest request) {
                    return new OpenAiChatHttpResponse(
                            200,
                            "{\"choices\":[{\"message\":{\"content\":\"controller model answer\"}}]}"
                    );
                }
            };
        }
    }
}

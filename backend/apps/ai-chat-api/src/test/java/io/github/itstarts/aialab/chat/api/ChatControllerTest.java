package io.github.itstarts.aialab.chat.api;

import io.github.itstarts.aialab.chat.provider.mock.MockChatProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ai.provider=mock",
        "ai.chat-model=mock-chat"
})
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.timestamp", not(blankOrNullString())));
    }

    @Test
    void unknownPathReturnsNotFoundWithUnifiedErrorResponse() throws Exception {
        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HTTP_RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("资源不存在"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));
    }

    @Test
    void unsupportedMethodReturnsMethodNotAllowedWithUnifiedErrorResponse() throws Exception {
        mockMvc.perform(get("/api/chat"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("HTTP_METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("请求方法不支持"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));
    }

    @Test
    void unsupportedContentTypeKeepsFrameworkStatusWithUnifiedErrorResponse() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("HTTP_415"))
                .andExpect(jsonPath("$.message").value("请求不正确"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));
    }

    @Test
    void chatEchoesMessageInMockMode() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" hello ai \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.model").value("mock-chat"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")))
                .andExpect(jsonPath("$.content").value("Echo: hello ai"));
    }

    @Test
    void chatTrimsUnicodeSpacesAroundMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\\u00A0hello ai\\u00A0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.model").value("mock-chat"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")))
                .andExpect(jsonPath("$.content").value("Echo: hello ai"));
    }

    @Test
    void chatRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAT_MESSAGE_INVALID"))
                .andExpect(jsonPath("$.message").value("message 不能为空"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));
    }

    @Test
    void chatRejectsUnicodeBlankMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\\u00A0\\u2007\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAT_MESSAGE_INVALID"))
                .andExpect(jsonPath("$.message").value("message 不能为空"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));
    }

    @Test
    void chatRejectsMessageLongerThanLimit() throws Exception {
        String oversizedMessage = "a".repeat(4001);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + oversizedMessage + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAT_MESSAGE_INVALID"))
                .andExpect(jsonPath("$.message").value("message 最多 4000 个字符"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));
    }

    @Test
    void chatAcceptsMessageAtLengthLimit() throws Exception {
        String maxLengthMessage = "a".repeat(4000);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + maxLengthMessage + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.model").value("mock-chat"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")))
                .andExpect(jsonPath("$.content").value("Echo: " + maxLengthMessage));
    }

    @Test
    void chatRejectsMalformedJsonWithUnifiedErrorResponse() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAT_MESSAGE_INVALID"))
                .andExpect(jsonPath("$.message").value("请求体格式不正确"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));
    }

    @Test
    void chatMapsProviderErrorToUnifiedErrorResponse() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + MockChatProvider.SIMULATED_PROVIDER_ERROR_MESSAGE + "\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_PROVIDER_ERROR"))
                .andExpect(jsonPath("$.message").value("模型服务返回错误，请稍后重试"))
                .andExpect(jsonPath("$.traceId", startsWith("trace_")));
    }
}

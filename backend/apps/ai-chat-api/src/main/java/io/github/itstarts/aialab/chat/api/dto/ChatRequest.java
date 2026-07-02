package io.github.itstarts.aialab.chat.api.dto;

import io.github.itstarts.aialab.chat.api.validation.MessageText;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        // 阶段 1 只拒绝空白和超长输入，短消息作为有效提问保留。
        @MessageText
        @Size(max = 4000, message = "message 最多 4000 个字符")
        String message
) {
}

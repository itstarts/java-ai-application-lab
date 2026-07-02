package io.github.itstarts.aialab.chat.provider;

import java.time.Duration;

// 应用层 Provider 契约入参，厂商协议字段保留在具体 Provider 子包内转换。
public record ProviderChatRequest(String traceId, String model, String message, Duration requestTimeout) {
}

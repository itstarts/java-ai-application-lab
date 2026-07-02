package io.github.itstarts.aialab.chat.provider;

// 应用层 Provider 契约出参，只暴露 ChatService 当前需要的模型结果。
public record ProviderChatResponse(String content) {
}

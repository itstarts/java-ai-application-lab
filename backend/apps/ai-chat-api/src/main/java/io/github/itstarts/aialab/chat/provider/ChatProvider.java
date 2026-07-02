package io.github.itstarts.aialab.chat.provider;

public interface ChatProvider {

    // 应用层只依赖该接口，真实模型 SDK 和协议细节留在具体 Provider 实现内。
    String providerName();

    ProviderChatResponse chat(ProviderChatRequest request);
}

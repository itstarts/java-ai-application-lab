package io.github.itstarts.aialab.chat.provider.mock;

import io.github.itstarts.aialab.chat.provider.ChatProvider;
import io.github.itstarts.aialab.chat.provider.ProviderChatRequest;
import io.github.itstarts.aialab.chat.provider.ProviderChatResponse;
import io.github.itstarts.aialab.chat.provider.error.ChatProviderException;
import org.springframework.stereotype.Component;

@Component
public class MockChatProvider implements ChatProvider {

    // 测试专用输入，用于在不访问外部模型的情况下覆盖 Provider 错误映射。
    public static final String SIMULATED_PROVIDER_ERROR_MESSAGE = "mock:provider-error";

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public ProviderChatResponse chat(ProviderChatRequest request) {
        if (SIMULATED_PROVIDER_ERROR_MESSAGE.equals(request.message())) {
            throw new ChatProviderException("mock provider simulated error");
        }

        return new ProviderChatResponse("Echo: " + request.message());
    }
}

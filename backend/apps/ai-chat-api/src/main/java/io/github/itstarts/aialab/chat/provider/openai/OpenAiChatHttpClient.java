package io.github.itstarts.aialab.chat.provider.openai;

import java.io.IOException;

public interface OpenAiChatHttpClient {

    OpenAiChatHttpResponse post(OpenAiChatHttpRequest request) throws IOException, InterruptedException;
}

package io.github.itstarts.aialab.chat.provider.openai;

import java.time.Duration;
import java.util.Map;

public record OpenAiChatHttpRequest(String url, Map<String, String> headers, String body, Duration timeout) {

    public OpenAiChatHttpRequest {
        headers = Map.copyOf(headers);
    }

    public boolean hasHeader(String name, String value) {
        return value.equals(headers.get(name));
    }

    @Override
    public String toString() {
        return "OpenAiChatHttpRequest[url=<redacted>, headers=<redacted>, body=<redacted>, timeout=" + timeout + "]";
    }
}

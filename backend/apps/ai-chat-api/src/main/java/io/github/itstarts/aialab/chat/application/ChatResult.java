package io.github.itstarts.aialab.chat.application;

public record ChatResult(String provider, String model, String content, String traceId) {
}

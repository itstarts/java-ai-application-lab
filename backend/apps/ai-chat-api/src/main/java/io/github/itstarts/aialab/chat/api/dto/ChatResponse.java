package io.github.itstarts.aialab.chat.api.dto;

public record ChatResponse(String provider, String model, String content, String traceId) {
}

package io.github.itstarts.aialab.chat.api.dto;

public record ApiErrorResponse(String code, String message, String traceId) {
}

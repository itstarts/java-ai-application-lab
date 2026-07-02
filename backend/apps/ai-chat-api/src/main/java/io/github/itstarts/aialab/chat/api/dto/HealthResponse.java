package io.github.itstarts.aialab.chat.api.dto;

import java.time.Instant;

public record HealthResponse(String status, Instant timestamp) {
}

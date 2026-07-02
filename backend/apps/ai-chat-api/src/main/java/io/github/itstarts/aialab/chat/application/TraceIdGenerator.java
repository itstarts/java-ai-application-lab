package io.github.itstarts.aialab.chat.application;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TraceIdGenerator {

    public String nextTraceId() {
        return "trace_" + UUID.randomUUID().toString().replace("-", "");
    }
}

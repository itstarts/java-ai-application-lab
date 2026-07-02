package io.github.itstarts.aialab.chat.provider.openai.dto;

import java.util.List;

public record OpenAiChatCompletionRequest(String model, List<OpenAiChatMessage> messages) {
}

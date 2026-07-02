package io.github.itstarts.aialab.chat.api.validation;

import io.github.itstarts.aialab.chat.api.ChatMessageNormalizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MessageTextValidator implements ConstraintValidator<MessageText, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return ChatMessageNormalizer.hasText(value);
    }
}

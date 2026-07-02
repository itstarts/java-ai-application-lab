package io.github.itstarts.aialab.chat.provider.error;

import lombok.Getter;

@Getter
public class ChatProviderException extends RuntimeException {

    private final ChatProviderErrorType errorType;

    public ChatProviderException(String message) {
        this(ChatProviderErrorType.PROVIDER_ERROR, message, null);
    }

    public ChatProviderException(String message, Throwable cause) {
        this(ChatProviderErrorType.PROVIDER_ERROR, message, cause);
    }

    public ChatProviderException(ChatProviderErrorType errorType, String message) {
        this(errorType, message, null);
    }

    public ChatProviderException(ChatProviderErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType == null ? ChatProviderErrorType.PROVIDER_ERROR : errorType;
    }
}

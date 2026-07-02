package io.github.itstarts.aialab.chat.provider.error;

import io.github.itstarts.aialab.chat.error.ApiErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatProviderErrorType {

    REQUEST_TIMEOUT(ApiErrorType.AI_REQUEST_TIMEOUT),
    RATE_LIMITED(ApiErrorType.AI_RATE_LIMITED),
    EMPTY_RESPONSE(ApiErrorType.AI_EMPTY_RESPONSE),
    PROVIDER_ERROR(ApiErrorType.AI_PROVIDER_ERROR);

    private final ApiErrorType apiErrorType;
}

package io.github.itstarts.aialab.chat.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ApiErrorType {

    CHAT_MESSAGE_INVALID("CHAT_MESSAGE_INVALID", "请求参数不正确", HttpStatus.BAD_REQUEST),
    AI_PROVIDER_NOT_FOUND("AI_PROVIDER_NOT_FOUND", "未找到模型服务", HttpStatus.SERVICE_UNAVAILABLE),
    AI_MODEL_NOT_CONFIGURED("AI_MODEL_NOT_CONFIGURED", "模型名称未配置", HttpStatus.SERVICE_UNAVAILABLE),
    AI_REQUEST_TIMEOUT("AI_REQUEST_TIMEOUT", "模型服务响应超时", HttpStatus.GATEWAY_TIMEOUT),
    AI_RATE_LIMITED("AI_RATE_LIMITED", "模型服务请求过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS),
    AI_EMPTY_RESPONSE("AI_EMPTY_RESPONSE", "模型服务返回空结果", HttpStatus.BAD_GATEWAY),
    AI_PROVIDER_ERROR("AI_PROVIDER_ERROR", "模型服务返回错误，请稍后重试", HttpStatus.BAD_GATEWAY),
    HTTP_REQUEST_INVALID("HTTP_REQUEST_INVALID", "请求不正确", HttpStatus.BAD_REQUEST),
    HTTP_RESOURCE_NOT_FOUND("HTTP_RESOURCE_NOT_FOUND", "资源不存在", HttpStatus.NOT_FOUND),
    HTTP_METHOD_NOT_ALLOWED("HTTP_METHOD_NOT_ALLOWED", "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "服务暂时不可用，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String userMessage;
    private final HttpStatus status;
}

package io.github.itstarts.aialab.chat.api;

import io.github.itstarts.aialab.chat.api.dto.ApiErrorResponse;
import io.github.itstarts.aialab.chat.application.ApiException;
import io.github.itstarts.aialab.chat.application.TraceIdGenerator;
import io.github.itstarts.aialab.chat.error.ApiErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ApiExceptionHandler {

    private final TraceIdGenerator traceIdGenerator;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String traceId = traceIdGenerator.nextTraceId();
        String message = exception.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("; "));
        if (StringUtils.isBlank(message)) {
            message = ApiErrorType.CHAT_MESSAGE_INVALID.getUserMessage();
        }

        // 校验异常可能包含完整用户输入，普通日志只记录 trace 和错误码。
        log.info("api_error traceId={} code={} status={}",
                traceId, ApiErrorType.CHAT_MESSAGE_INVALID.getCode(), ApiErrorType.CHAT_MESSAGE_INVALID.getStatus().value());
        return ResponseEntity.status(ApiErrorType.CHAT_MESSAGE_INVALID.getStatus())
                .body(new ApiErrorResponse(ApiErrorType.CHAT_MESSAGE_INVALID.getCode(), message, traceId));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadableException(HttpMessageNotReadableException exception) {
        String traceId = traceIdGenerator.nextTraceId();
        // 请求体解析异常可能包含原始片段，普通日志只记录 trace 和错误码。
        log.info("api_error traceId={} code={} status={}",
                traceId, ApiErrorType.CHAT_MESSAGE_INVALID.getCode(), ApiErrorType.CHAT_MESSAGE_INVALID.getStatus().value());
        return ResponseEntity.status(ApiErrorType.CHAT_MESSAGE_INVALID.getStatus())
                .body(new ApiErrorResponse(ApiErrorType.CHAT_MESSAGE_INVALID.getCode(), "请求体格式不正确", traceId));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        log.debug("api_error traceId={} code={} status={}",
                exception.getTraceId(), exception.getCode(), exception.getStatus().value());
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(exception.getCode(), exception.getUserMessage(), exception.getTraceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception exception) {
        String traceId = traceIdGenerator.nextTraceId();
        if (exception instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            return handleFrameworkException(errorResponse, traceId);
        }

        log.error("api_error traceId={} code={} status={}",
                traceId, ApiErrorType.INTERNAL_SERVER_ERROR.getCode(), ApiErrorType.INTERNAL_SERVER_ERROR.getStatus().value(), exception);
        return ResponseEntity.status(ApiErrorType.INTERNAL_SERVER_ERROR.getStatus())
                .body(new ApiErrorResponse(
                        ApiErrorType.INTERNAL_SERVER_ERROR.getCode(),
                        ApiErrorType.INTERNAL_SERVER_ERROR.getUserMessage(),
                        traceId
                ));
    }

    private ResponseEntity<ApiErrorResponse> handleFrameworkException(ErrorResponse errorResponse, String traceId) {
        HttpStatusCode statusCode = errorResponse.getStatusCode();
        ApiErrorType errorType = resolveFrameworkErrorType(statusCode);
        String code = frameworkErrorCode(statusCode, errorType);
        String message = errorType == null ? ApiErrorType.HTTP_REQUEST_INVALID.getUserMessage() : errorType.getUserMessage();
        log.info("api_error traceId={} code={} status={}", traceId, code, statusCode.value());
        return ResponseEntity.status(statusCode)
                .body(new ApiErrorResponse(code, message, traceId));
    }

    private ApiErrorType resolveFrameworkErrorType(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 404 -> ApiErrorType.HTTP_RESOURCE_NOT_FOUND;
            case 405 -> ApiErrorType.HTTP_METHOD_NOT_ALLOWED;
            default -> null;
        };
    }

    private String frameworkErrorCode(HttpStatusCode statusCode, ApiErrorType errorType) {
        return errorType == null ? "HTTP_" + statusCode.value() : errorType.getCode();
    }
}

package io.github.itstarts.aialab.chat.application;

import io.github.itstarts.aialab.chat.error.ApiErrorType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final String code;
    // 当前阶段对外错误消息与 Throwable message 保持一致，日志关联依赖 errorCode 和 traceId。
    private final String userMessage;
    private final HttpStatus status;
    private final String traceId;

    public ApiException(ApiErrorType errorType, String traceId) {
        this(errorType, errorType.getUserMessage(), traceId, null);
    }

    public ApiException(ApiErrorType errorType, String traceId, Throwable cause) {
        this(errorType, errorType.getUserMessage(), traceId, cause);
    }

    public ApiException(ApiErrorType errorType, String userMessage, String traceId, Throwable cause) {
        super(userMessage, cause);
        this.code = errorType.getCode();
        this.userMessage = userMessage;
        this.status = errorType.getStatus();
        this.traceId = traceId;
    }

}

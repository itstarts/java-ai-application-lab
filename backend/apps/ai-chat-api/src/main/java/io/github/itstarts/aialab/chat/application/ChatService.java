package io.github.itstarts.aialab.chat.application;

import io.github.itstarts.aialab.chat.provider.ChatProvider;
import io.github.itstarts.aialab.chat.provider.ProviderChatRequest;
import io.github.itstarts.aialab.chat.provider.ProviderChatResponse;
import io.github.itstarts.aialab.chat.provider.config.ChatProviderProperties;
import io.github.itstarts.aialab.chat.provider.error.ChatProviderException;
import io.github.itstarts.aialab.chat.error.ApiErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final String FEATURE = "chat";
    private static final String PROMPT_VERSION = "system-v1";

    private final ChatProviderProperties properties;
    private final List<ChatProvider> providers;
    private final TraceIdGenerator traceIdGenerator;

    public ChatResult chat(String message) {
        String traceId = traceIdGenerator.nextTraceId();
        String providerName = properties.provider();
        String model = properties.chatModel();
        long startedAt = System.nanoTime();

        try {
            ChatProvider provider = resolveProvider(providerName, traceId);
            validateModel(model, traceId);

            ProviderChatRequest providerRequest = new ProviderChatRequest(traceId, model, message, properties.requestTimeout());
            ProviderChatResponse providerResponse = provider.chat(providerRequest);
            long latencyMs = elapsedMillis(startedAt);

            log.info(
                    "model_call traceId={} feature={} provider={} model={} promptVersion={} latencyMs={} status=success",
                    traceId, FEATURE, provider.providerName(), model, PROMPT_VERSION, latencyMs
            );
            return new ChatResult(provider.providerName(), model, providerResponse.content(), traceId);
        } catch (ApiException exception) {
            logChatRequestFailure(traceId, providerName, model, startedAt, exception.getCode());
            throw exception;
        } catch (ChatProviderException exception) {
            ApiException mappedException = new ApiException(
                    exception.getErrorType().getApiErrorType(),
                    traceId,
                    exception
            );
            logModelCallFailure(traceId, providerName, model, startedAt, mappedException.getCode(), exception);
            throw mappedException;
        } catch (RuntimeException exception) {
            ApiException mappedException = new ApiException(
                    ApiErrorType.INTERNAL_SERVER_ERROR,
                    traceId,
                    exception
            );
            logModelCallFailure(traceId, providerName, model, startedAt, mappedException.getCode(), exception);
            throw mappedException;
        }
    }

    private ChatProvider resolveProvider(String providerName, String traceId) {
        return providers.stream()
                .filter(provider -> provider.providerName().equals(providerName))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        ApiErrorType.AI_PROVIDER_NOT_FOUND,
                        traceId
                ));
    }

    private void validateModel(String model, String traceId) {
        if (StringUtils.isBlank(model)) {
            throw new ApiException(
                    ApiErrorType.AI_MODEL_NOT_CONFIGURED,
                    traceId
            );
        }
    }

    private void logModelCallFailure(
            String traceId,
            String providerName,
            String model,
            long startedAt,
            String errorCode,
            Throwable exception
    ) {
        LoggingEventBuilder builder = ApiErrorType.INTERNAL_SERVER_ERROR.getCode().equals(errorCode)
                ? log.atError()
                : log.atWarn();

        // 普通 Provider 错误只输出结构化字段，未知异常保留 cause 便于排查。
        if (ApiErrorType.INTERNAL_SERVER_ERROR.getCode().equals(errorCode) && exception != null) {
            builder.setCause(exception);
        }

        builder.setMessage("model_call traceId={} feature={} provider={} model={} promptVersion={} latencyMs={} status=failed errorCode={}")
                .addArgument(traceId)
                .addArgument(FEATURE)
                .addArgument(providerName)
                .addArgument(safeModel(model))
                .addArgument(PROMPT_VERSION)
                .addArgument(elapsedMillis(startedAt))
                .addArgument(errorCode)
                .log();
    }

    private void logChatRequestFailure(
            String traceId,
            String providerName,
            String model,
            long startedAt,
            String errorCode
    ) {
        log.warn(
                "chat_request traceId={} feature={} provider={} model={} promptVersion={} latencyMs={} status=failed errorCode={}",
                traceId, FEATURE, providerName, safeModel(model), PROMPT_VERSION, elapsedMillis(startedAt), errorCode
        );
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String safeModel(String model) {
        return StringUtils.isNotBlank(model) ? model : "missing";
    }
}

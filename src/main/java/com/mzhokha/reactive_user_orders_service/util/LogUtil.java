package com.mzhokha.reactive_user_orders_service.util;

import org.slf4j.MDC;
import reactor.core.publisher.Signal;

import java.util.Optional;
import java.util.function.Consumer;

public class LogUtil {

    public static final String REQUEST_ID = "requestId";

    public static final String CONTEXT_REQUEST_ID = "contextRequestId";

    public static void putRequestIdIntoMdc(String requestId) {
        MDC.put(REQUEST_ID, requestId);
    }

    public static String getRequestIdFromMdc() {
        return MDC.get(REQUEST_ID);
    }

    public static <T> Consumer<Signal<T>> logOnError(Consumer<Throwable> logStatement) {
        return signal -> {
            if (!signal.isOnError()) return;
            Optional<String> contextualRequestIdOptional = signal.getContextView().getOrEmpty(CONTEXT_REQUEST_ID);

            contextualRequestIdOptional.ifPresentOrElse(contextualRequestId -> {
                        try (MDC.MDCCloseable cMdc = MDC.putCloseable(REQUEST_ID, contextualRequestId)) {
                            logStatement.accept(signal.getThrowable());
                        }
                    },
                    () -> logStatement.accept(signal.getThrowable()));
        };
    }

    public static <T> Consumer<Signal<T>> logOnNext(Consumer<T> logStatement) {
        return signal -> {
            if (!signal.isOnNext()) return;
            Optional<String> contextualRequestIdOptional = signal.getContextView().getOrEmpty(CONTEXT_REQUEST_ID);

            contextualRequestIdOptional.ifPresentOrElse(contextualRequestId -> {
                        try (MDC.MDCCloseable cMdc = MDC.putCloseable(REQUEST_ID, contextualRequestId)) {
                            logStatement.accept(signal.get());
                        }
                    },
                    () -> logStatement.accept(signal.get()));
        };
    }
}

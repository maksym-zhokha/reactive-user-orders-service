package com.mzhokha.reactive_user_orders_service.util;

import org.slf4j.MDC;
import reactor.core.publisher.Signal;

import java.util.Optional;
import java.util.function.Consumer;

public class LogUtil {

    public static <T> Consumer<Signal<T>> logOnNext(Consumer<T> logStatement) {
        return signal -> {
            if (!signal.isOnNext()) return;
            logStatement(logStatement, signal);
        };
    }

    public static Consumer<Signal<Throwable>> logOnError(Consumer<Throwable> errorLogStatement) {
        return signal -> {
            if (!signal.isOnError()) return;
            logStatement(errorLogStatement, signal);
        };
    }

    private static <T> void logStatement(Consumer<T> logStatement, Signal<T> signal) {
        Optional<String> toPutInMdc = signal.getContextView().getOrEmpty("contextRequestId");

        toPutInMdc.ifPresentOrElse(tpim -> {
                    try (MDC.MDCCloseable cMdc = MDC.putCloseable("requestId", tpim)) {
                        logStatement.accept(signal.get());
                    }
                },
                () -> logStatement.accept(signal.get()));
    }
}

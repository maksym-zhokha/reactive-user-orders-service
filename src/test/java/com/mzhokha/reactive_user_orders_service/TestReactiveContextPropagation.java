package com.mzhokha.reactive_user_orders_service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class TestReactiveContextPropagation {

    @Test
    public void testContext() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
                .flatMap(s -> Mono.deferContextual(ctx ->
                        Mono.just(s + "1 " + ctx.get(key))))
                .flatMap(s -> Mono.deferContextual(ctx ->
                        Mono.just(s + "2 " + ctx.get(key))))
                .contextWrite(ctx -> ctx.put(key, "World"));

        StepVerifier.create(r)
                .expectNext("Hello World")
                .verifyComplete();
    }
}

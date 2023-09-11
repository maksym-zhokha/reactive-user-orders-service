package com.mzhokha.reactive_user_orders_service;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class TestGeneralApi {

    private static Logger log = LoggerFactory.getLogger(TestGeneralApi.class);

    @Test
    void exception_flux() {
        // when
        var flux = Flux.just("a", "b", "c")
                .concatWith(Flux.error(new RuntimeException("Exception occurred")))
                .concatWith(Flux.just("d"))
                .log();

        // then
        StepVerifier.create(flux)
                .expectNext("a", "b", "c")
                .expectError()
                .verify();
    }

    @Test
    void explore_Mono_onErrorContinue() {
        // when
        var mono = explore_Mono_onErrorContinue("abc");

        // then
        StepVerifier.create(mono)
                .verifyComplete();
    }

    private Mono<String> explore_Mono_onErrorContinue(String input) {
        return Mono.just(input)
                .map(s -> {
                    if ("abc".equals(s)) {
                        throw new RuntimeException("Exception Occurred");
                    } else {
                        return s;
                    }
                })
                .onErrorContinue((ex, s) -> {
                    System.out.println("ex: " + ex + " , s: " + s);
                })
                .log();
    }

    @Test
    void explore_Mono_onErrorContinue_1() {
        // when
        var mono = explore_Mono_onErrorContinue("reactor");

        // then
        StepVerifier.create(mono)
                .expectNext("reactor")
                .verifyComplete();
    }
}

package com.mzhokha.reactive_user_orders_service;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Random;

public class TestFlatMapMergeConcatZip {

    private static Logger log = LoggerFactory.getLogger(TestFlatMapMergeConcatZip.class);

    @Test
    void namesFlux_flatmap() {
        // when
        var namesFlux = Flux.fromIterable(List.of("alex", "chloe"))
                // everything happens in single thread
                .flatMap(s -> splitString(s))
                .map(String::toUpperCase)
                .log();

        // then
        StepVerifier.create(namesFlux)
                .expectNextCount(9)
                .verifyComplete();
    }

    private Flux<String> splitString(String input) {
        var strArr = input.split("");
        return Flux.fromArray(strArr);
    }

    @Test
    void namesFlux_flatmap_async() {
        // when
        var namesFlux = Flux.fromIterable(List.of("alex", "chloe"))
                .map(s -> {
                    log.info("map 1: " + s);
                    return s.toUpperCase();
                })
                // flatMap is executed in same thread as subscriber, sequentially
                // but next map() ops and onNext() for each Flux element is called in separate thread
                .flatMap(s -> {
                    log.info("flatMap: " + s);
                    return splitStringAsync(s);
                })
                // this map is called in separate thread for each Flux's element
                .map(s -> {
                    log.info("map 2: " + s);
                    return s.toLowerCase();
                })
                .log();

        // then
        StepVerifier.create(namesFlux)
                .expectNextCount(9)
                .verifyComplete();
    }

    private Flux<String> splitStringAsync(String input) {
        var strArr = input.split("");
        int delay = new Random().nextInt(1000);
        // Signals are delayed and continue on the parallel default Scheduler
        return Flux.fromArray(strArr).delayElements(Duration.ofMillis(delay));
    }

    @Test
    void concatWith() {
        // given
        Flux<String> abcFlux = Flux.just("A", "B", "C");
        Flux<String> defFlux = Flux.just("D", "E", "F");

        // when
        var flux = abcFlux
                // onNext() for each element is called sequentially in one thread
                // first absFlux's elements, then defFlux's elements
                .concatWith(defFlux).log();

        // then
        StepVerifier.create(flux)
                .expectNext("A", "B", "C", "D", "E", "F")
                .verifyComplete();
    }

    @Test
    void mergeWith_withoutDelays() {
        // given
        Flux<String> abcFlux = Flux.just("A", "B", "C");
        Flux<String> defFlux = Flux.just("D", "E", "F");

        // when
        var flux = abcFlux
                // Looks like concatWith, but per documentation each publisher subscribed eagerly
                .mergeWith(defFlux).log();

        // then
        StepVerifier.create(flux)
                .expectNextCount(6)
                .verifyComplete();
    }

    @Test
    void mergeWith_withDelayInEachFluxForEachElement() {
        // given
        // onNext() for each element is invoked in different thread
        // Signals are delayed and continue on the parallel default Scheduler
        // i.e. each signal continued on new thread from Scheduler
        Flux<String> abcFlux = Flux.just("A", "B", "C").delayElements(Duration.ofMillis(100));
        Flux<String> defFlux = Flux.just("D", "E", "F").delayElements(Duration.ofMillis(150));

        // when
        var flux = abcFlux.mergeWith(defFlux).log();

        // then
        StepVerifier.create(flux)
                .expectNextCount(6)
                .verifyComplete();
    }

    @Test
    void explore_zipWith() {
        // given
        Flux<String> abcFlux = Flux.just("A", "B", "C");
        Flux<String> defFlux = Flux.just("D", "E", "F");

        // when
        var flux = abcFlux.zipWith(defFlux, (f1, f2) -> f1 + f2).log();

        // then
        StepVerifier.create(flux)
                .expectNext("AD", "BE", "CF")
                .verifyComplete();
    }
}

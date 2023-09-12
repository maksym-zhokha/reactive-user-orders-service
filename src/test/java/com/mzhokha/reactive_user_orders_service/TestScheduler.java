package com.mzhokha.reactive_user_orders_service;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.function.Consumer;

import static com.mzhokha.reactive_user_orders_service.util.DelayUtil.delay;

class TestScheduler {

    private static Logger log = LoggerFactory.getLogger(TestScheduler.class);

    static List<String> namesList = List.of("alex", "ben", "chloe");
    static List<String> namesList1 = List.of("adam", "jill", "jack");

    @Test
    void subscribeOn() {
        // when
        var flux = Flux.fromIterable(namesList)
                // Run subscribe, onSubscribe and request on a specified Scheduler's Scheduler.Worker.
                // CAN'T CONFIRM THIS WITH CURRENT TEST
                .subscribeOn(Schedulers.parallel())
                .doOnSubscribe(s -> log.info("Subscribed"))
                .map(s -> {
                    log.info("Name is: " + s);
                    return s;
                })
                .map(this::upperCaseWithDelay)
                .doOnNext(s -> {
                    log.info("Name is: " + s);
                })
                .log();

        // then
        StepVerifier.create(flux)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void publishOn() {
        // when
        var flux = Flux.fromIterable(namesList)
                .publishOn(Schedulers.parallel())
                .doOnSubscribe(s -> log.info("Subscribed"))
                .map(s -> {
                    log.info("Name is: " + s);
                    return s;
                })
                .map(this::upperCaseWithDelay)
                .doOnNext(System.out::println)
                .log();

        // then
        StepVerifier.create(flux)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void subscribeOn_and_publishOn() {
        Consumer<Integer> consumer = s -> System.out.println(s + " : " + Thread.currentThread().getName());

        Flux.range(1, 5)
                .doOnNext(consumer)
                .map(i -> {
                    System.out.println("Inside map the thread is " + Thread.currentThread().getName());
                    return i * 10;
                })
                // While one element is not finished processing chain, other element already stated in other thread
                .publishOn(Schedulers.newParallel("First_PublishOn()_thread"))
                .doOnNext(consumer)
                .publishOn(Schedulers.newParallel("Second_PublishOn()_thread"))
                .doOnNext(consumer)
                .subscribeOn(Schedulers.newParallel("subscribeOn_thread"))
                .subscribe();
    }

    @Test
    void nestedSubscribeOns() {
        Scheduler schedulerA = Schedulers.newParallel("scheduler-a", 4);
        Scheduler schedulerB = Schedulers.newParallel("scheduler-b", 4);

        Flux.range(1, 3)
                .map(i -> {
                    System.out.println(String.format("Outer First map - (%s), Thread: %s", i, Thread.currentThread().getName()));
                    return i;
                })
                .subscribeOn(schedulerA)
                .map(i -> {
                    System.out.println(String.format("Outer Second map - (%s), Thread: %s", i, Thread.currentThread().getName()));

                    return Flux
                            .range(1, 2)
                            .map(j -> {
                                System.out.println(String.format("Innter First map - (%s.%s), Thread: %s", i, j, Thread.currentThread().getName()));
                                return j;
                            })
                            .subscribeOn(schedulerB)
                            .map(j -> {
                                System.out.println(String.format("Inner Second map - (%s.%s), Thread: %s", i, j, Thread.currentThread().getName()));
                                return "value " + j;
                            }).subscribe();
                })
                .blockLast();
    }

    @Test
    // Two streams each one in own thread
    // But each stream is processed sequentially in one thread
    void merge_when_each_publishOn() {
        // when
        var namesFlux = Flux.fromIterable(namesList)
                .map(s -> {
                    delay(1000);
                    log.info("Name is: " + s);
                    return s;
                })
                .publishOn(Schedulers.parallel())
                .map(this::upperCaseWithDelay)
                .log();

        var namesFlux1 = Flux.fromIterable(namesList1)
                .map(s -> {
                    delay(1000);
                    log.info("Name is: " + s);
                    return s;
                })
                .publishOn(Schedulers.parallel())
                .map(this::upperCaseWithDelay)
                .log();

        var flux = namesFlux.mergeWith(namesFlux1);
                // almost the same as doOnNext on each stream separately
                // the difference: bellow method will be called after onNext() of stream
                //.doOnNext(System.out::println);

        // then
        StepVerifier.create(flux)
                .expectNextCount(6)
                .verifyComplete();
    }

    @Test
    void merge_when_each_subscribeOn() {
        // when
        var namesFlux = Flux.fromIterable(namesList)
                .map(s -> {
                    delay(1000);
                    log.info("Name is: " + s);
                    return s;
                })
                .map(this::upperCaseWithDelay)
                .subscribeOn(Schedulers.parallel())
                .log();

        var namesFlux1 = Flux.fromIterable(namesList1)
                .map(s -> {
                    delay(1000);
                    log.info("Name is: " + s);
                    return s;
                })
                .map(this::upperCaseWithDelay)
                .subscribeOn(Schedulers.parallel())
                .log();

        var flux = namesFlux.mergeWith(namesFlux1);

        // then
        StepVerifier.create(flux)
                .expectNextCount(6)
                .verifyComplete();
    }

    @Test
    void parallel() {
        // when
        var flux = Flux.fromIterable(namesList)
                // each element of a stream will be processed in separate thread
                .parallel()
                .runOn(Schedulers.parallel())
                .map(this::upperCaseWithDelay)
                .map(s -> {
                    log.info("Name is: " + s);
                    return s;
                })
                .log();

        // then
        StepVerifier.create(flux)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void flatMap_each_element_to_subscribed_Mono() {
        // when
        var flux = Flux.just("alex", "ben", "chloe", "daemon")
                .flatMap(name -> {
                    return Mono.just(name)
                            .subscribeOn(Schedulers.parallel())
                            // For each element map is called in own thread (almost).
                            // For some elements one thread is used.
                            .map(s -> {
                                log.info("Name is: {}", s);
                                return s;
                            })
                            .map(this::upperCaseWithDelay);
                })
                .log();

        // then
        StepVerifier.create(flux)
                .expectNextCount(4)
                .verifyComplete();
    }

    @Test
    void flatMap_each_element_to_subscribed_Flux() {
        // when
        var flux = Flux.just("alex", "ben", "chloe", "daemon")
                .flatMap(name -> {
                    return Mono.just(name)
                            .subscribeOn(Schedulers.parallel())
                            .map(s -> {
                                log.info("Name is: {}", s);
                                return s;
                            })
                            .map(this::upperCaseWithDelay);
                })
                .log();

        // then
        StepVerifier.create(flux)
                .expectNextCount(4)
                .verifyComplete();
    }

    @Test
    void parallel_usingFlatmap_1() {
        // given
        var namesFlux = Flux.fromIterable(namesList)
                .flatMap(name -> {
                    return Mono.just(name)
                            .map(this::upperCaseWithDelay)
                            .subscribeOn(Schedulers.parallel());
                })
                .log();

        var namesFlux1 = Flux.fromIterable(namesList1)
                .flatMap(name -> {
                    return Mono.just(name)
                            .map(this::upperCaseWithDelay)
                            .subscribeOn(Schedulers.parallel());
                })
                .log();

        // when
        var flux = namesFlux.mergeWith(namesFlux1);

        // then
        StepVerifier.create(flux)
                .expectNextCount(6)
                .verifyComplete();
    }

    @Test
    void parallel_using_flatMapSequential() {
        // given
        var noOfCores = Runtime.getRuntime().availableProcessors();
        log.info("number of cores: {}", noOfCores);

        // when
        var flux = Flux.fromIterable(namesList)
                .flatMapSequential(name -> {
                    return Mono.just(name)
                            .map(this::upperCaseWithDelay)
                            .subscribeOn(Schedulers.parallel());
                })
                .log();

        // then
        StepVerifier.create(flux)
                .expectNext("ALEX", "BEN", "CHLOE")
                .verifyComplete();
    }

    // one call to onSubscribe(), request(), onComplete()
    @Test
    void concatWith_whenEachFlux_subscribeOn_parallel() {
        // given
        Flux<String> abcFlux = Flux.just("A", "B", "C")
                .map(String::toUpperCase)
                .subscribeOn(Schedulers.parallel());

        Flux<String> defFlux = Flux.just("D", "E", "F")
                .map(String::toUpperCase)
                .subscribeOn(Schedulers.parallel());

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

    private String upperCaseWithDelay(String name) {
        delay(1000);
        return name.toUpperCase();
    }
}
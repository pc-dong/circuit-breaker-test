package com.example.server;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TestService {

    private final WebClient webClient;
//    private final ReactiveCircuitBreakerFactory cbFactory;

//    @CircuitBreaker(name = "backendA", fallbackMethod = "fallBackTest1")
//    @TimeLimiter(name = "backendA", fallbackMethod = "fallBackTest1")
    @CircuitBreak(value = "test", group = "oms", fallback = "testFallback")
    public Mono<String> test(int seconds) {
        return webClient.get()
                .uri("/test/" + seconds)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> fallBackTest1(int seconds, Throwable throwable) {
        return Mono.just("testFallback1");
    }

    @CircuitBreak(value = "test2", group = "oms", fallback = "[{\"message\":\"testFallback2\"}]")
//    @CircuitBreaker(name = "backendA", fallbackMethod = "fallBackTest2")
//    @TimeLimiter(name = "backendA", fallbackMethod = "fallBackTest2")
    public Flux<Response> test2(int seconds) {
        return webClient.get()
                .uri("/test2/" + seconds)
                .retrieve()
                .bodyToFlux(Response.class);
    }

    public Flux<Response> fallBackTest2(int seconds, Throwable throwable) {
        return Flux.just(new Response("testFallback2"));
    }


//    private <T> Mono<T> circuitBreak(Mono<T> toRun) {
//        return cbFactory.create("test").run(toRun);
//    }
//
//    private <T> Flux<T> circuitBreak(Flux<T> toRun) {
//        return cbFactory.create("test").run(toRun);
//    }
}

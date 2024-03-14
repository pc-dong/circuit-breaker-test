package com.example.server;

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

    @CircuitBreak(value = "test", group = "oms", fallback = "testFallback")
    public Mono<String> test(int seconds) {
        return webClient.get()
                .uri("/test/" + seconds)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("Fallback");
    }

    @CircuitBreak(value = "test2", group = "oms")
    public Mono<String> test2(int seconds) {
        return webClient.get()
                .uri("/test2/" + seconds)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("Fallback");
    }


//    private <T> Mono<T> circuitBreak(Mono<T> toRun) {
//        return cbFactory.create("test").run(toRun);
//    }
//
//    private <T> Flux<T> circuitBreak(Flux<T> toRun) {
//        return cbFactory.create("test").run(toRun);
//    }
}

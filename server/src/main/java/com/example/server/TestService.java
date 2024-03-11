package com.example.server;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TestService {

    private final WebClient webClient;
    private final ReactiveCircuitBreakerFactory cbFactory;

    public Mono<String> test(int seconds) {
        return webClient.get()
                .uri("/test/" + seconds)
                .retrieve()
                .bodyToMono(String.class)
                .transform(it -> cbFactory.create("test").run(it, throwable -> Mono.just("fallback")));
    }

}

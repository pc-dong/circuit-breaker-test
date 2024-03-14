package com.example.targetserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class TestController {
    @GetMapping("/test/{seconds}")
    public Mono<String> test(@PathVariable int seconds) {
        return Mono.just("Hello, World!")
                .delayElement(java.time.Duration.ofSeconds(seconds));
    }

    @GetMapping("/test2/{seconds}")
    public Mono<String> test2(@PathVariable int seconds) {
        return Mono.just("Hello, World 2!")
                .delayElement(java.time.Duration.ofSeconds(seconds));
    }
}

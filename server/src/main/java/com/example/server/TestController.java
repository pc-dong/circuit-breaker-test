package com.example.server;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final TestService testService;

    @GetMapping("/test/{seconds}")
    public Mono<String> test(@PathVariable int seconds) {
        return testService.test(seconds);
    }

    @GetMapping("/test2/{seconds}")
    public Flux<Response> test2(@PathVariable int seconds) {
        return testService.test2(seconds);
    }
}

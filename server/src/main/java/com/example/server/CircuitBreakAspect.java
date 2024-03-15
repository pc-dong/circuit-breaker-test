package com.example.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class CircuitBreakAspect {
    private final ReactiveCircuitBreakerFactory cbFactory;
    private final ObjectMapper objectMapper;
    private final CircuitBreakProperties circuitBreakProperties;
    private final RegistryStore<ReactiveCircuitBreaker> registryStore = new InMemoryRegistryStore<>();

    @Around(value = "annotationOfAnyCircuitBreak(circuitBreak) && executionOfAnyMonoMethod()", argNames = "joinPoint,circuitBreak")
    final Object aroundMono(final ProceedingJoinPoint joinPoint, CircuitBreak circuitBreak) throws Throwable {
        Function<Throwable, Mono<Object>> callback = getMonoCallback(joinPoint, circuitBreak);
        if (circuitBreakProperties.inEffect()
                &&
                (circuitBreakProperties.getDegradeGroups().contains(circuitBreak.group())
                        || circuitBreakProperties.getDegradeGroups().contains(circuitBreak.value()))) {
            log.info("Degraded: group=" + circuitBreak.group() + ", value=" + circuitBreak.value());
            return null == callback ? Mono.error(new RuntimeException("Degrade"))
                    : callback.apply(new RuntimeException("Degrade"));
        }

        if (!circuitBreakProperties.isEnabled()) {
            return joinPoint.proceed();
        }

        ReactiveCircuitBreaker breaker = getReactiveCircuitBreaker(circuitBreak);
        return callback == null ? breaker.run((Mono) joinPoint.proceed())
                : breaker.run((Mono) joinPoint.proceed(), callback);
    }

    private Function<Throwable, Mono<Object>> getMonoCallback(ProceedingJoinPoint joinPoint, CircuitBreak circuitBreak) throws ClassNotFoundException {
        Function<Throwable, Mono<Object>> callback = null;
        if (StringUtils.hasLength(circuitBreak.fallback())) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Type type = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
            Class<?> aClass = Class.forName(type.getTypeName());

            callback = (throwable) -> {
                try {
                    return switch (aClass.getSimpleName()) {
                        case "String" -> Mono.just(circuitBreak.fallback());
                        case "Integer" -> Mono.just(Integer.valueOf(circuitBreak.fallback()));
                        case "Boolean" -> Mono.just(Boolean.valueOf(circuitBreak.fallback()));
                        default -> Mono.just(objectMapper.readValue(circuitBreak.fallback(), aClass));
                    };
                } catch (JsonProcessingException e) {
                    return Mono.error(e);
                }
            };
        }
        return callback;
    }

    private ReactiveCircuitBreaker getReactiveCircuitBreaker(CircuitBreak circuitBreak) {
        return registryStore.computeIfAbsent(circuitBreak.value() + "#" + circuitBreak.group(),
                key -> {
                    log.info(circuitBreak.value() + "#" + circuitBreak.group());
                    if (StringUtils.hasLength(circuitBreak.group())) {
                        return cbFactory.create(circuitBreak.value(), circuitBreak.group());
                    }

                    return cbFactory.create(circuitBreak.value());
                });
    }

    @Around(value = "annotationOfAnyCircuitBreak(circuitBreak) && executionOfAnyFluxMethod()", argNames = "joinPoint,circuitBreak")
    final Object aroundFlux(final ProceedingJoinPoint joinPoint, CircuitBreak circuitBreak) throws Throwable {
        Function<Throwable, Flux<Object>> callback = getFluxCallback(circuitBreak);
        if (circuitBreakProperties.inEffect()
                &&
                (circuitBreakProperties.getDegradeGroups().contains(circuitBreak.group())
                        || circuitBreakProperties.getDegradeGroups().contains(circuitBreak.value()))) {
            log.info("Degraded: group=" + circuitBreak.group() + ", value=" + circuitBreak.value());
            return null == callback ? Flux.error(new RuntimeException("Degrade"))
                    : callback.apply(new RuntimeException("Degrade"));
        }

        if (!circuitBreakProperties.isEnabled()) {
            return joinPoint.proceed();
        }

        ReactiveCircuitBreaker breaker = getReactiveCircuitBreaker(circuitBreak);
        return null == callback ? breaker.run((Flux) joinPoint.proceed())
                : breaker.run((Flux) joinPoint.proceed(), callback);
    }

    private Function<Throwable, Flux<Object>> getFluxCallback(CircuitBreak circuitBreak) {
        Function<Throwable, Flux<Object>> callback = null;
        if (StringUtils.hasLength(circuitBreak.fallback())) {
            callback = (throwable) -> {
                try {
                    return Flux.fromIterable(objectMapper.readValue(circuitBreak.fallback(), new TypeReference<List<Object>>() {
                    }));
                } catch (JsonProcessingException e) {
                    return Flux.error(e);
                }
            };
        }
        return callback;
    }

    @Pointcut("@annotation(circuitBreak)")
    public void annotationOfAnyCircuitBreak(CircuitBreak circuitBreak) {
    }

    @Pointcut(value = "execution(public reactor.core.publisher.Mono *(..))")
    private void executionOfAnyMonoMethod() {
    }

    @Pointcut(value = "execution(public reactor.core.publisher.Flux *(..))")
    private void executionOfAnyFluxMethod() {
    }
}

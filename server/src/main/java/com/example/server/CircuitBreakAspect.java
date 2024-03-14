package com.example.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class CircuitBreakAspect {
    private final ReactiveCircuitBreakerFactory cbFactory;
    private final CircuitBreakProperties circuitBreakProperties;


    @Around(value = "annotationOfAnyCircuitBreak(circuitBreak) && executionOfAnyMonoMethod()", argNames = "joinPoint,circuitBreak")
    final Object aroundMono(final ProceedingJoinPoint joinPoint, CircuitBreak circuitBreak) throws Throwable {

        if (circuitBreakProperties.getDegradeGroups().contains(circuitBreak.group())
                || circuitBreakProperties.getDegradeGroups().contains(circuitBreak.value())) {
            log.info("Degraded: group=" + circuitBreak.group() + ", value=" + circuitBreak.value());
            return Mono.error(new RuntimeException("Degrade"));
        }

        if (!circuitBreakProperties.isEnabled()) {
            return joinPoint.proceed();
        }


        ReactiveCircuitBreaker breaker;
        if (StringUtils.hasLength(circuitBreak.group())) {
            breaker = cbFactory.create(circuitBreak.value(), circuitBreak.group());
        }

        breaker = cbFactory.create(circuitBreak.value());

        if (StringUtils.hasLength(circuitBreak.fallback())) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Type type = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
            Class<?> aClass = Class.forName(type.getTypeName());

            return breaker.run((Mono) joinPoint.proceed(), (throwable) -> {
                try {
                    return switch (aClass.getSimpleName()) {
                        case "String" -> Mono.just(circuitBreak.fallback());
                        case "Integer" -> Mono.just(Integer.valueOf(circuitBreak.fallback()));
                        case "Boolean" -> Mono.just(Boolean.valueOf(circuitBreak.fallback()));
                        default -> Mono.just(new ObjectMapper().readValue(circuitBreak.fallback(), aClass));
                    };
                } catch (JsonProcessingException e) {
                    return Mono.error(e);
                }
            });
        }
        return breaker.run((Mono) joinPoint.proceed());
    }

    @Around(value = "annotationOfAnyCircuitBreak(circuitBreak) && executionOfAnyFluxMethod()", argNames = "joinPoint,circuitBreak")
    final Object aroundFlux(final ProceedingJoinPoint joinPoint, CircuitBreak circuitBreak) throws Throwable {
        if (circuitBreakProperties.getDegradeGroups().contains(circuitBreak.group())
                || circuitBreakProperties.getDegradeGroups().contains(circuitBreak.value())) {
            log.info("Degraded: group=" + circuitBreak.group() + ", value=" + circuitBreak.value());
            return Flux.error(new RuntimeException("Degrade"));
        }

        if (!circuitBreakProperties.isEnabled()) {
            return joinPoint.proceed();
        }

        if (StringUtils.hasLength(circuitBreak.group())) {
            return cbFactory.create(circuitBreak.value(), circuitBreak.group()).run((Flux) joinPoint.proceed());
        }

        return cbFactory.create(circuitBreak.value()).run((Flux) joinPoint.proceed());
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

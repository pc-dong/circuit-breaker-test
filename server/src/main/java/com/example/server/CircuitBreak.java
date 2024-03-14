package com.example.server;

import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.lang.annotation.*;
import java.util.function.Function;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@RefreshScope
public @interface CircuitBreak {
    String value() default "";

    String group() default "";

    String fallback() default "";
}

package com.example.server;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "circuit-breaker")
@Component
@Data
public class CircuitBreakProperties {

    private boolean enabled = true;

    private Set<String> degradeGroups = new HashSet<>();
}

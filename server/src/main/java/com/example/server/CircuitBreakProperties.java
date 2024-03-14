package com.example.server;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ConfigurationProperties(prefix = "circuit-breaker")
@Component
@Data
public class CircuitBreakProperties {

    private boolean enabled = true;

    private Set<String> degradeGroups = new HashSet<>();

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    public boolean inEffect() {
        return Optional.ofNullable(startTime).map(start -> start.isBefore(LocalDateTime.now())).orElse(true)
                && Optional.ofNullable(endTime).map(end -> end.isAfter(LocalDateTime.now())).orElse(true);
    }
}

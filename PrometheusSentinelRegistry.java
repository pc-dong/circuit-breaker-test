package com.example.sentinelserver;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

public class PrometheusSentinelRegistry {

    private Counter passRequests;
    private Counter blockRequests;
    private Counter successRequests;
    private Counter exceptionRequests;
    private Histogram rtHist;
    private Gauge currentThreads;

    public PrometheusSentinelRegistry(CollectorRegistry registry) {
        passRequests = Counter.build()
                .name("sentinel_pass_requests_total")
                .help("total pass requests.")
                .labelNames("resource")
                .register(registry);
        blockRequests = Counter.build()
                .name("sentinel_block_requests_total")
                .help("total block requests.")
                .labelNames("resource", "type", "ruleLimitApp", "limitApp")
                .register(registry);
        successRequests = Counter.build()
                .name("sentinel_success_requests_total")
                .help("total success requests.")
                .labelNames("resource")
                .register(registry);
        exceptionRequests = Counter.build()
                .name("sentinel_exception_requests_total")
                .help("total exception requests.")
                .labelNames("resource")
                .register(registry);
        currentThreads = Gauge.build()
                .name("sentinel_current_threads")
                .help("current thread count.")
                .labelNames("resource")
                .register(registry);
        rtHist = Histogram.build()
                .name("sentinel_requests_latency_seconds")
                .help("request latency in seconds.")
                .labelNames("resource")
                .register(registry);
    }

    public Counter getPassRequests() {
        return passRequests;
    }

    public Counter getBlockRequests() {
        return blockRequests;
    }

    public Counter getSuccessRequests() {
        return successRequests;
    }

    public Counter getExceptionRequests() {
        return exceptionRequests;
    }

    public Histogram getRtHist() {
        return rtHist;
    }

    public Gauge getCurrentThreads() {
        return currentThreads;
    }
}

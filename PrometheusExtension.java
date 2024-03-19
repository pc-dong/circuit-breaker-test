package com.example.sentinelserver;

import com.alibaba.csp.sentinel.metric.extension.MetricExtension;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.context.ApplicationContext;

public class PrometheusExtension implements MetricExtension {

    private PrometheusSentinelRegistry prometheusSenRegistry;

    private PrometheusSentinelRegistry getRegistry() {
        if (prometheusSenRegistry != null) {
            return prometheusSenRegistry;
        }
        this.prometheusSenRegistry = SpringUtil.getBean(PrometheusSentinelRegistry.class);
        return prometheusSenRegistry;
    }

    @Override
    public void addPass(String resource, int n, Object... args) {
        getRegistry().getPassRequests().labels(resource).inc(n);
    }

    @Override
    public void addBlock(String resource, int n, String origin, BlockException ex, Object... args) {
        getRegistry().getBlockRequests().labels(resource, ex.getClass().getSimpleName(), ex.getRuleLimitApp(), origin).inc(n);
    }

    @Override
    public void addSuccess(String resource, int n, Object... args) {
        getRegistry().getSuccessRequests().labels(resource).inc(n);
    }

    @Override
    public void addException(String resource, int n, Throwable throwable) {
        getRegistry().getExceptionRequests().labels(resource).inc(n);
    }

    @Override
    public void addRt(String resource, long rt, Object... args) {
        // convert millisecond to second
        getRegistry().getRtHist().labels(resource).observe(((double) rt) / 1000);
    }

    @Override
    public void increaseThreadNum(String resource, Object... args) {
        getRegistry().getCurrentThreads().labels(resource).inc();
    }

    @Override
    public void decreaseThreadNum(String resource, Object... args) {
        getRegistry().getCurrentThreads().labels(resource).dec();
    }
}


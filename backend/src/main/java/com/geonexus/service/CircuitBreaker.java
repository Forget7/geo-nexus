package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 断路器模式实现 - 防止级联故障
 */
@Slf4j
public class CircuitBreaker {
    
    public enum State { CLOSED, OPEN, HALF_OPEN }
    
    private final String name;
    private final int failureThreshold;
    private final long timeout;
    private final long recoveryTimeout;
    
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile long lastFailureTime = 0;
    
    public CircuitBreaker(String name, int failureThreshold, long timeout, long recoveryTimeout) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.timeout = timeout;
        this.recoveryTimeout = recoveryTimeout;
    }
    
    public CircuitBreaker(String name) {
        this(name, 5, 60000, 30000);
    }
    
    /**
     * 执行请求
     */
    public <T> T execute(Supplier<T> supplier) throws CircuitBreakerOpenException {
        if (!allowRequest()) {
            throw new CircuitBreakerOpenException("Circuit breaker is OPEN for: " + name);
        }
        
        try {
            T result = supplier.get();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure(e);
            throw e;
        }
    }
    
    /**
     * 执行请求（无返回）
     */
    public void executeVoid(Runnable runnable) throws CircuitBreakerOpenException {
        if (!allowRequest()) {
            throw new CircuitBreakerOpenException("Circuit breaker is OPEN for: " + name);
        }
        
        try {
            runnable.run();
            recordSuccess();
        } catch (Exception e) {
            recordFailure(e);
            throw e;
        }
    }
    
    private boolean allowRequest() {
        State currentState = state.get();
        
        if (currentState == State.CLOSED) {
            return true;
        }
        
        if (currentState == State.OPEN) {
            // 检查超时
            if (System.currentTimeMillis() - lastFailureTime > recoveryTimeout) {
                // 转为半开
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    log.info("Circuit breaker {} transitioning to HALF_OPEN", name);
                }
                return true;
            }
            return false;
        }
        
        // HALF_OPEN - 允许有限请求
        return true;
    }
    
    private void recordSuccess() {
        if (state.get() == State.HALF_OPEN) {
            // 成功则关闭
            state.set(State.CLOSED);
            failureCount.set(0);
            log.info("Circuit breaker {} transitioned to CLOSED", name);
        } else {
            failureCount.set(0);
        }
    }
    
    private void recordFailure(Exception e) {
        lastFailureTime = System.currentTimeMillis();
        int failures = failureCount.incrementAndGet();
        
        if (state.get() == State.HALF_OPEN) {
            // 失败则重新打开
            state.set(State.OPEN);
            log.warn("Circuit breaker {} reopened due to failure in HALF_OPEN", name);
        } else if (failures >= failureThreshold) {
            state.set(State.OPEN);
            log.warn("Circuit breaker {} opened after {} failures", name, failures);
        }
    }
    
    public State getState() {
        return state.get();
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
    
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}

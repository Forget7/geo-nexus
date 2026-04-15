package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import java.util.function.Supplier;

/**
 * 重试工具 - 支持指数退避
 */
@Slf4j
public class RetryUtil {
    
    /**
     * 带重试的执行
     */
    public static <T> T execute(Supplier<T> supplier, int maxAttempts, long baseDelayMs) 
            throws RetryExhaustedException {
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxAttempts) {
                    // 指数退避
                    long delay = baseDelayMs * (long) Math.pow(2, attempt - 1);
                    // 添加 jitter
                    delay = delay / 2 + (long) (Math.random() * delay);
                    
                    log.warn("Attempt {}/{} failed, retrying in {}ms: {}", 
                            attempt, maxAttempts, delay, e.getMessage());
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RetryExhaustedException("Interrupted", ie);
                    }
                }
            }
        }
        
        throw new RetryExhaustedException(
                "Retry exhausted after " + maxAttempts + " attempts", lastException);
    }
    
    /**
     * 简单重试（无返回）
     */
    public static void executeVoid(Runnable runnable, int maxAttempts, long baseDelayMs) 
            throws RetryExhaustedException {
        
        execute(() -> {
            runnable.run();
            return null;
        }, maxAttempts, baseDelayMs);
    }
    
    /**
     * 带条件重试
     */
    public static <T> T executeWithCondition(
            Supplier<T> supplier, 
            int maxAttempts, 
            long baseDelayMs,
            RetryCondition<T> condition) throws RetryExhaustedException {
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = supplier.get();
                
                if (condition.shouldRetry(result)) {
                    throw new RetryConditionException("Condition not met");
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxAttempts) {
                    long delay = baseDelayMs * (long) Math.pow(2, attempt - 1);
                    log.warn("Attempt {}/{} failed, retrying in {}ms", 
                            attempt, maxAttempts, delay);
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RetryExhaustedException("Interrupted", ie);
                    }
                }
            }
        }
        
        throw new RetryExhaustedException(
                "Retry exhausted after " + maxAttempts + " attempts", lastException);
    }
    
    @FunctionalInterface
    public interface RetryCondition<T> {
        boolean shouldRetry(T result);
    }
    
    public static class RetryExhaustedException extends RuntimeException {
        public RetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class RetryConditionException extends RuntimeException {
        public RetryConditionException(String message) {
            super(message);
        }
    }
}

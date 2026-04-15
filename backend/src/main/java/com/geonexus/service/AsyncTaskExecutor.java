package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 高级异步任务执行器
 * 支持任务队列、优先级、超时控制、进度报告
 */
@Slf4j
@Service
public class AsyncTaskExecutor {
    
    // 核心线程池配置
    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 16;
    private static final int QUEUE_CAPACITY = 100;
    private static final long KEEP_ALIVE_TIME = 60L;
    
    // 任务超时默认5分钟
    private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000L;
    
    // 线程池
    private ThreadPoolTaskExecutor taskExecutor;
    private ScheduledExecutorService scheduledExecutor;
    
    // 任务存储
    private final Map<String, TaskContext> tasks = new ConcurrentHashMap<>();
    
    // 任务监听器
    private final List<TaskListener> listeners = new CopyOnWriteArrayList<>();
    
    @PostConstruct
    public void init() {
        // 创建任务执行器
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(CORE_POOL_SIZE);
        taskExecutor.setMaxPoolSize(MAX_POOL_SIZE);
        taskExecutor.setQueueCapacity(QUEUE_CAPACITY);
        taskExecutor.setThreadNamePrefix("geonexus-task-");
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(30);
        
        // 拒绝策略：调用者运行
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        taskExecutor.initialize();
        
        // 创建调度器（用于超时检查）
        scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setName("geonexus-scheduler-" + t.getId());
            t.setDaemon(true);
            return t;
        });
        
        // 启动超时检查器
        startTimeoutChecker();
        
        log.info("AsyncTaskExecutor initialized: core={}, max={}, queue={}", 
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down AsyncTaskExecutor...");
        
        // 停止接受新任务
        taskExecutor.shutdown();
        
        // 停止调度器
        scheduledExecutor.shutdown();
        
        try {
            // 等待现有任务完成
            if (!taskExecutor.getThreadPoolExecutor().awaitTermination(30, TimeUnit.SECONDS)) {
                taskExecutor.getThreadPoolExecutor().shutdownNow();
            }
            
            if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            taskExecutor.getThreadPoolExecutor().shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("AsyncTaskExecutor shutdown complete");
    }
    
    /**
     * 提交异步任务
     */
    public <T> TaskFuture<T> submit(String taskName, Callable<T> callable) {
        return submit(taskName, callable, TaskPriority.NORMAL, DEFAULT_TIMEOUT, null);
    }
    
    /**
     * 提交异步任务（带优先级）
     */
    public <T> TaskFuture<T> submit(String taskName, Callable<T> callable, TaskPriority priority) {
        return submit(taskName, callable, priority, DEFAULT_TIMEOUT, null);
    }
    
    /**
     * 提交异步任务（完整参数）
     */
    public <T> TaskFuture<T> submit(
            String taskName,
            Callable<T> callable,
            TaskPriority priority,
            long timeoutMs,
            Consumer<Progress> progressCallback) {
        
        String taskId = UUID.randomUUID().toString();
        
        TaskContext context = new TaskContext();
        context.id = taskId;
        context.name = taskName;
        context.priority = priority;
        context.status = TaskStatus.PENDING;
        context.createdAt = System.currentTimeMillis();
        context.progressCallback = progressCallback;
        context.timeoutMs = timeoutMs;
        
        // 包装Callable以跟踪进度
        Callable<T> wrappedCallable = () -> {
            context.status = TaskStatus.RUNNING;
            context.startedAt = System.currentTimeMillis();
            notifyListeners(TaskEvent.RUNNING, context);
            
            try {
                T result = callable.call();
                context.status = TaskStatus.COMPLETED;
                context.completedAt = System.currentTimeMillis();
                context.result = result;
                notifyListeners(TaskEvent.COMPLETED, context);
                return result;
            } catch (Exception e) {
                context.status = TaskStatus.FAILED;
                context.completedAt = System.currentTimeMillis();
                context.error = e;
                notifyListeners(TaskEvent.FAILED, context);
                throw e;
            }
        };
        
        tasks.put(taskId, context);
        
        // 根据优先级选择执行队列
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                return wrappedCallable.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, taskExecutor);
        
        // 设置超时
        if (timeoutMs > 0) {
            future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        }
        
        // 处理完成
        future.whenComplete((result, error) -> {
            if (error != null) {
                if (error instanceof TimeoutException) {
                    context.status = TaskStatus.TIMEOUT;
                    context.completedAt = System.currentTimeMillis();
                    notifyListeners(TaskEvent.TIMEOUT, context);
                }
            }
            
            // 清理任务（保留一段时间以便查询）
            scheduleCleanup(taskId);
        });
        
        notifyListeners(TaskEvent.SUBMITTED, context);
        
        log.debug("Task submitted: id={}, name={}, priority={}", taskId, taskName, priority);
        
        return new TaskFuture<>(taskId, future, context);
    }
    
    /**
     * 提交无返回值的任务
     */
    public TaskFuture<Void> submitVoid(String taskName, Runnable runnable) {
        return submit(taskName, () -> {
            runnable.run();
            return null;
        });
    }
    
    /**
     * 提交GIS批量任务
     */
    public <T, R> TaskFuture<List<R>> submitBatch(
            String taskName,
            List<T> items,
            int batchSize,
            FunctionWithException<T, R> processor) {
        
        int totalBatches = (int) Math.ceil((double) items.size() / batchSize);
        
        return submit(taskName + " (batch)", () -> {
            List<R> results = new ArrayList<>(items.size());
            
            for (int i = 0; i < totalBatches; i++) {
                int fromIndex = i * batchSize;
                int toIndex = Math.min(fromIndex + batchSize, items.size());
                List<T> batch = items.subList(fromIndex, toIndex);
                
                for (T item : batch) {
                    results.add(processor.apply(item));
                }
                
                // 更新进度
                int progress = (int) ((i + 1) * 100.0 / totalBatches);
                updateProgress(taskName, progress, 
                        String.format("Batch %d/%d", i + 1, totalBatches));
            }
            
            return results;
        }, TaskPriority.NORMAL, DEFAULT_TIMEOUT, null);
    }
    
    /**
     * 批量提交任务（并行）
     */
    public <T> List<TaskFuture<T>> submitParallel(
            List<String> taskNames,
            List<Callable<T>> callables) {
        
        List<TaskFuture<T>> futures = new ArrayList<>();
        
        for (int i = 0; i < taskNames.size(); i++) {
            String name = i < taskNames.size() ? taskNames.get(i) : "task-" + i;
            Callable<T> callable = i < callables.size() ? callables.get(i) : () -> null;
            futures.add(submit(name, callable));
        }
        
        return futures;
    }
    
    /**
     * 更新任务进度
     */
    public void updateProgress(String taskId, int progress, String message) {
        TaskContext context = tasks.get(taskId);
        if (context != null) {
            context.progress = progress;
            context.progressMessage = message;
            
            if (context.progressCallback != null) {
                context.progressCallback.accept(new Progress(progress, message));
            }
            
            notifyListeners(TaskEvent.PROGRESS, context);
        }
    }
    
    /**
     * 取消任务
     */
    public boolean cancel(String taskId) {
        TaskContext context = tasks.get(taskId);
        if (context == null) {
            return false;
        }
        
        if (context.status == TaskStatus.PENDING || context.status == TaskStatus.RUNNING) {
            context.status = TaskStatus.CANCELLED;
            context.completedAt = System.currentTimeMillis();
            notifyListeners(TaskEvent.CANCELLED, context);
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取任务状态
     */
    public TaskContext getTaskStatus(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * 获取所有任务
     */
    public List<TaskContext> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    /**
     * 获取运行中的任务
     */
    public List<TaskContext> getRunningTasks() {
        return tasks.values().stream()
                .filter(t -> t.status == TaskStatus.RUNNING)
                .toList();
    }
    
    /**
     * 添加任务监听器
     */
    public void addListener(TaskListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除任务监听器
     */
    public void removeListener(TaskListener listener) {
        listeners.remove(listener);
    }
    
    // ==================== 内部方法 ====================
    
    private void notifyListeners(TaskEvent event, TaskContext context) {
        for (TaskListener listener : listeners) {
            try {
                listener.onTaskEvent(event, context);
            } catch (Exception e) {
                log.warn("Task listener error: {}", e.getMessage());
            }
        }
    }
    
    private void scheduleCleanup(String taskId) {
        scheduledExecutor.schedule(() -> {
            tasks.remove(taskId);
        }, 5, TimeUnit.MINUTES);
    }
    
    private void startTimeoutChecker() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            
            for (TaskContext context : tasks.values()) {
                if (context.status == TaskStatus.RUNNING && context.timeoutMs > 0) {
                    long elapsed = now - context.startedAt;
                    if (elapsed > context.timeoutMs) {
                        context.status = TaskStatus.TIMEOUT;
                        context.completedAt = now;
                        notifyListeners(TaskEvent.TIMEOUT, context);
                    }
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    // ==================== 内部类 ====================
    
    public enum TaskPriority {
        LOW(1),
        NORMAL(5),
        HIGH(10),
        CRITICAL(20);
        
        public final int value;
        
        TaskPriority(int value) {
            this.value = value;
        }
    }
    
    public enum TaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED,
        TIMEOUT
    }
    
    public enum TaskEvent {
        SUBMITTED,
        RUNNING,
        PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED,
        TIMEOUT
    }
    
    @lombok.Data
    public static class TaskContext {
        String id;
        String name;
        TaskPriority priority;
        TaskStatus status;
        long createdAt;
        long startedAt;
        long completedAt;
        long timeoutMs;
        int progress = 0;
        String progressMessage;
        Object result;
        Throwable error;
        Consumer<Progress> progressCallback;
    }
    
    public record Progress(int percent, String message) {}
    
    @FunctionalInterface
    public interface FunctionWithException<T, R> {
        R apply(T t) throws Exception;
    }
    
    public interface TaskListener {
        void onTaskEvent(TaskEvent event, TaskContext context);
    }
    
    // ==================== TaskFuture ====================
    
    public class TaskFuture<T> {
        private final String taskId;
        private final CompletableFuture<T> future;
        private final TaskContext context;
        
        TaskFuture(String taskId, CompletableFuture<T> future, TaskContext context) {
            this.taskId = taskId;
            this.future = future;
            this.context = context;
        }
        
        public String getTaskId() {
            return taskId;
        }
        
        public TaskContext getContext() {
            return context;
        }
        
        public boolean isDone() {
            return future.isDone();
        }
        
        public boolean isCancelled() {
            return future.isCancelled();
        }
        
        public T get() throws InterruptedException, ExecutionException {
            return future.get();
        }
        
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }
        
        public void thenAccept(Consumer<T> consumer) {
            future.thenAccept(consumer);
        }
        
        public void exceptionally(Function<Throwable, T> handler) {
            future.exceptionally(handler);
        }
    }
}

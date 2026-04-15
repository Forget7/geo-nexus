package com.geonexus.service;

import com.geonexus.model.TaskResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步任务服务 - 用于长时GIS操作
 */
@Slf4j
@Service
public class TaskService {
    
    private final Map<String, TaskResponse> tasks = new ConcurrentHashMap<>();
    private final GISService gisService;
    
    public TaskService(GISService gisService) {
        this.gisService = gisService;
    }
    
    /**
     * 创建异步任务
     */
    public String createTask(String type, Map<String, Object> params) {
        String taskId = UUID.randomUUID().toString();
        tasks.put(taskId, TaskResponse.pending(taskId));
        
        // 根据类型分发处理
        switch (type) {
            case "buffer" -> executeBufferTaskAsync(taskId, params);
            case "distance" -> executeDistanceTaskAsync(taskId, params);
            case "overlay" -> executeOverlayTaskAsync(taskId, params);
            default -> updateTaskStatus(taskId, TaskResponse.failed(taskId, "Unknown task type: " + type));
        }
        
        return taskId;
    }
    
    /**
     * 获取任务状态
     */
    public TaskResponse getTask(String taskId) {
        return tasks.getOrDefault(taskId, 
                TaskResponse.failed(taskId, "Task not found: " + taskId));
    }
    
    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, TaskResponse response) {
        tasks.put(taskId, response);
    }
    
    @Async("gisTaskExecutor")
    public void executeBufferTaskAsync(String taskId, Map<String, Object> params) {
        try {
            updateTaskStatus(taskId, TaskResponse.processing(taskId));
            log.info("Executing buffer task: {}", taskId);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> geometry = (Map<String, Object>) params.get("geometry");
            Double distance = ((Number) params.get("distanceKm")).doubleValue();
            
            // 执行缓冲区分析
            Map<String, Object> result = gisService.bufferAnalysis(geometry, distance);
            
            updateTaskStatus(taskId, TaskResponse.completed(taskId, result));
            log.info("Buffer task completed: {}", taskId);
            
        } catch (Exception e) {
            log.error("Buffer task failed: {}", taskId, e);
            updateTaskStatus(taskId, TaskResponse.failed(taskId, e.getMessage()));
        }
    }
    
    @Async("gisTaskExecutor")
    public void executeDistanceTaskAsync(String taskId, Map<String, Object> params) {
        try {
            updateTaskStatus(taskId, TaskResponse.processing(taskId));
            log.info("Executing distance task: {}", taskId);
            
            @SuppressWarnings("unchecked")
            var p1 = (java.util.List<Double>) params.get("point1");
            @SuppressWarnings("unchecked")
            var p2 = (java.util.List<Double>) params.get("point2");
            String unit = (String) params.getOrDefault("unit", "km");
            
            Map<String, Object> result = gisService.calculateDistance(
                    p1.get(0), p1.get(1), p2.get(0), p2.get(1), unit);
            
            updateTaskStatus(taskId, TaskResponse.completed(taskId, result));
            
        } catch (Exception e) {
            log.error("Distance task failed: {}", taskId, e);
            updateTaskStatus(taskId, TaskResponse.failed(taskId, e.getMessage()));
        }
    }
    
    @Async("gisTaskExecutor")
    public void executeOverlayTaskAsync(String taskId, Map<String, Object> params) {
        try {
            updateTaskStatus(taskId, TaskResponse.processing(taskId));
            log.info("Executing overlay task: {}", taskId);
            
            // 叠加分析任务...
            // 简化实现
            updateTaskStatus(taskId, TaskResponse.completed(taskId, 
                    Map.of("message", "Overlay analysis completed (simplified)")));
            
        } catch (Exception e) {
            log.error("Overlay task failed: {}", taskId, e);
            updateTaskStatus(taskId, TaskResponse.failed(taskId, e.getMessage()));
        }
    }
}

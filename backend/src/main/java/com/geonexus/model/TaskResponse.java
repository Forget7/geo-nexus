package com.geonexus.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 异步任务响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "异步任务响应")
public class TaskResponse {
    @Schema(description = "任务ID")
    private String taskId;
    
    @Schema(description = "任务状态")
    private TaskStatus status;
    
    @Schema(description = "状态消息")
    private String message;
    
    @Schema(description = "任务结果")
    private Object result;
    
    @Schema(description = "错误信息")
    private String error;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;
    
    @Schema(description = "任务状态枚举")
    public enum TaskStatus {
        PENDING,    // 等待中
        PROCESSING, // 处理中
        COMPLETED,  // 已完成
        FAILED      // 失败
    }
    
    public static TaskResponse pending(String taskId) {
        return TaskResponse.builder()
                .taskId(taskId)
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    public static TaskResponse processing(String taskId) {
        return TaskResponse.builder()
                .taskId(taskId)
                .status(TaskStatus.PROCESSING)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    public static TaskResponse completed(String taskId, Object result) {
        return TaskResponse.builder()
                .taskId(taskId)
                .status(TaskStatus.COMPLETED)
                .result(result)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }
    
    public static TaskResponse failed(String taskId, String error) {
        return TaskResponse.builder()
                .taskId(taskId)
                .status(TaskStatus.FAILED)
                .error(error)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }
}

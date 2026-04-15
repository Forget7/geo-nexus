package com.geonexus.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 异步任务实体
 */
@Entity
@Table(name = "async_tasks", indexes = {
        @Index(name = "idx_task_user", columnList = "userId"),
        @Index(name = "idx_task_status", columnList = "status"),
        @Index(name = "idx_task_created", columnList = "createdAt")
})
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "异步任务实体")
public class AsyncTaskEntity extends BaseEntity {
    
    @Column(nullable = false)
    @Schema(description = "用户ID")
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "任务类型")
    private TaskType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    @Schema(description = "任务状态")
    private TaskStatus status = TaskStatus.PENDING;
    
    @Column(columnDefinition = "JSONB")
    @Schema(description = "任务参数字典")
    private String paramsJson;
    
    @Column(columnDefinition = "JSONB")
    @Schema(description = "任务结果JSON")
    private String resultJson;
    
    @Column(columnDefinition = "TEXT")
    @Schema(description = "错误信息")
    private String error;
    
    @Schema(description = "开始时间")
    private LocalDateTime startedAt;
    
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;
    
    @Schema(description = "任务类型枚举")
    public enum TaskType {
        BUFFER, DISTANCE, OVERLAY, INTERSECT, UNION, CLIP, SIMPLIFY, REPROJECT
    }
    
    @Schema(description = "任务状态枚举")
    public enum TaskStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    }
    
    public void markStarted() {
        this.status = TaskStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
    }
    
    public void markCompleted(String result) {
        this.status = TaskStatus.COMPLETED;
        this.resultJson = result;
        this.completedAt = LocalDateTime.now();
    }
    
    public void markFailed(String error) {
        this.status = TaskStatus.FAILED;
        this.error = error;
        this.completedAt = LocalDateTime.now();
    }
}

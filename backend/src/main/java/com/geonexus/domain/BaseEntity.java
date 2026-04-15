package com.geonexus.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类 - 包含审计字段、乐观锁、软删除、多租户支持
 *
 * 使用方式：让所有实体继承此类
 * @EntityListeners(AuditingEntityListener.class) 必须在子类上声明
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 - UUID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // ==================== 审计字段 ====================

    /**
     * 创建时间（自动填充，创建时设置，不可更新）
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间（自动填充，每次保存时更新）
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 创建人（通过 AuditorAware 自动填充）
     */
    @CreatedBy
    @Column(nullable = false, updatable = false, length = 50)
    private String createdBy;

    /**
     * 更新人（通过 AuditorAware 自动填充）
     */
    @LastModifiedBy
    @Column(nullable = false, length = 50)
    private String updatedBy;

    // ==================== 乐观锁 ====================

    /**
     * 乐观锁版本号
     */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    // ==================== 软删除 ====================

    /**
     * 删除时间（null = 未删除）
     */
    @Column
    private LocalDateTime deletedAt;

    /**
     * 删除人
     */
    @Column(length = 50)
    private String deletedBy;

    // ==================== 多租户 ====================

    /**
     * 租户 ID（用于数据隔离）
     */
    @Column(nullable = false, length = 50)
    private String tenantId = "default";

    // ==================== 辅助方法 ====================

    /**
     * 判断是否已删除
     */
    @JsonIgnore
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 标记为删除（软删除）
     */
    public void markAsDeleted(String deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    /**
     * 恢复已删除的记录
     */
    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
    }
}

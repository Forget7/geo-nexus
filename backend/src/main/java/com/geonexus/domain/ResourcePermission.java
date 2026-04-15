package com.geonexus.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 资源权限实体
 */
@Entity
@Table(name = "resource_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "资源权限记录")
public class ResourcePermission {

    @Id
    @Schema(description = "权限记录ID")
    private String id;

    /**用户ID*/
    @Column(nullable = false)
    @Schema(description = "用户ID")
    private String userId;

    /**资源ID*/
    @Column(nullable = false)
    @Schema(description = "资源ID")
    private String resourceId;

    /**权限类型：READ/WRITE/ADMIN/DELETE*/
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "权限类型")
    private Permission permission;

    /**行级过滤条件(JSON)*/
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Schema(description = "行级过滤条件")
    private Map<String, Object> rowFilter;

    /**创建时间*/
    @Column(nullable = false)
    @Schema(description = "创建时间")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    @Schema(description = "权限类型枚举")
    public enum Permission {
        READ, WRITE, ADMIN, DELETE
    }
}

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
 * 审计日志实体
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "审计日志记录")
public class AuditLog {

    @Id
    @Schema(description = "日志ID")
    private String id;

    /**操作用户ID*/
    @Schema(description = "操作用户ID")
    private String userId;

    /**操作类型：CREATE/UPDATE/DELETE/READ/EXPORT/LOGIN/LOGOUT*/
    @Column(nullable = false)
    @Schema(description = "操作类型", example = "CREATE")
    private String action;

    /**资源类型：GIS_DATA/MAP_DOCUMENT/USER/SESSION*/
    @Schema(description = "资源类型", example = "GIS_DATA")
    private String resourceType;

    /**资源ID*/
    @Schema(description = "资源ID")
    private String resourceId;

    /**详细信息(JSON)*/
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Schema(description = "详细信息")
    private Map<String, Object> details;

    /**客户端IP*/
    @Schema(description = "客户端IP")
    private String ipAddress;

    /**User-Agent*/
    @Schema(description = "客户端User-Agent")
    private String userAgent;

    /**时间戳*/
    @Column(nullable = false)
    @Schema(description = "操作时间戳")
    private Instant timestamp;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }
}

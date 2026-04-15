package com.geonexus.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 4D场景模板实体
 * 保存地图场景的完整状态：相机位置、时间范围、图层配置等
 */
@Entity
@Table(name = "scene_template", indexes = {
        @Index(name = "idx_scene_name", columnList = "name"),
        @Index(name = "idx_scene_tenant", columnList = "tenantId")
})
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "4D场景模板")
public class SceneTemplate extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板名称
     */
    @Column(nullable = false, length = 200)
    @Schema(description = "模板名称")
    private String name;

    /**
     * 模板描述
     */
    @Column(length = 1000)
    @Schema(description = "模板描述")
    private String description;

    // ==================== 相机状态 ====================

    /**
     * 相机经度
     */
    @Column
    @Schema(description = "相机经度")
    private Double cameraLon;

    /**
     * 相机纬度
     */
    @Column
    @Schema(description = "相机纬度")
    private Double cameraLat;

    /**
     * 相机高度（米，用于3D模式）
     */
    @Column
    @Schema(description = "相机高度（米）")
    private Double cameraHeight;

    /**
     * 相机方位角（0-360度）
     */
    @Column
    @Schema(description = "相机方位角（度）")
    private Double cameraHeading;

    /**
     * 相机俯仰角（0-90度）
     */
    @Column
    @Schema(description = "相机俯仰角（度）")
    private Double cameraPitch;

    // ==================== 时间范围 ====================

    /**
     * 时间范围开始（ISO 8601格式）
     */
    @Column(length = 50)
    @Schema(description = "时间范围开始")
    private String timeRangeStart;

    /**
     * 时间范围结束（ISO 8601格式）
     */
    @Column(length = 50)
    @Schema(description = "时间范围结束")
    private String timeRangeEnd;

    // ==================== 图层配置 ====================

    /**
     * 图层配置列表（JSON字符串存储）
     */
    @Column(columnDefinition = "TEXT")
    @Schema(description = "图层配置JSON")
    private String layersJson;

    // ==================== 缩略图 ====================

    /**
     * 缩略图Base64或URL
     */
    @Column(columnDefinition = "TEXT")
    @Schema(description = "缩略图URL或Base64")
    private String thumbnail;

    /**
     * 模板类型：default/custom
     */
    @Column(length = 50)
    @Schema(description = "模板类型")
    private String templateType;
}

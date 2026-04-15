package com.geonexus.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 3D Tiles 倾斜摄影数据集实体
 */
@Entity
@Table(name = "tilesets")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TilesetInfo extends BaseEntity {

    /**
     * 数据集名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 数据集描述
     */
    private String description;

    /**
     * tileset URL 或 Cesium ion asset ID
     */
    @Column(nullable = false)
    private String url;

    /**
     * 数据类型: CESIUM_ION | URL
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TilesetType type;

    /**
     * Cesium ion asset ID（仅 ion 类型）
     */
    private Integer ionAssetId;

    /**
     * 地图位置 - 经度
     */
    private Double positionLon;

    /**
     * 地图位置 - 纬度
     */
    private Double positionLat;

    /**
     * 地图位置 - 高度
     */
    private Double positionHeight;

    /**
     * 最大屏幕空间误差
     */
    private Integer maxScreenSpaceError;

    /**
     * 是否可见
     */
    @Builder.Default
    private Boolean visible = true;

    /**
     * 创建者
     */
    @Column(updatable = false)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    public enum TilesetType {
        CESIUM_ION,
        URL
    }
}

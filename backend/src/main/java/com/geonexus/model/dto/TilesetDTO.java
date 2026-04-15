package com.geonexus.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 3D Tileset 元数据 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "3D Tileset元数据")
public class TilesetDTO {
    @Schema(description = "Tileset唯一标识")
    private String id;
    
    @Schema(description = "Tileset名称")
    private String name;
    
    @Schema(description = "Tileset描述")
    private String description;
    
    @Schema(description = "Tileset资源URL")
    private String url;
    
    @Schema(description = "Tileset类型")
    private TilesetType type;
    
    @Schema(description = "Cesium ion asset ID（仅CESIUM_ION类型）")
    private Long ionAssetId;        // Cesium ion asset ID（可选）
    
    @Schema(description = "Cesium ion访问令牌（仅CESIUM_ION类型）")
    private String accessToken;     // Cesium ion token（可选）
    
    @Schema(description = "模型经度位置")
    private Double longitude;
    
    @Schema(description = "模型纬度位置")
    private Double latitude;
    
    @Schema(description = "模型高度")
    private Double height;
    
    @Schema(description = "航向角（度）")
    private Double heading;
    
    @Schema(description = "俯仰角（度）")
    private Double pitch;
    
    @Schema(description = "翻滚角（度）")
    private Double roll;
    
    @Schema(description = "最大屏幕空间误差", example = "16")
    private Integer maxScreenSpaceError;
    
    @Schema(description = "当前状态：loading/ready/error")
    private String status;          // loading, ready, error
    
    @Schema(description = "创建时间")
    private Instant createdAt;
    
    @Schema(description = "创建者")
    private String createdBy;

    @Schema(description = "Tileset类型枚举")
    public enum TilesetType {
        CESIUM_ION,    // Cesium ion asset
        SELF_HOSTED,    // 自托管 3D Tiles
        MINIO           // MinIO/S3 存储
    }
}

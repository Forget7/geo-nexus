package com.geonexus.model.dto;

import com.geonexus.service.SpatialAnomalyService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 异常检测请求DTO
 */
@Data
@Schema(description = "异常检测请求")
public class AnomalyDetectionRequest {

    @Schema(description = "待检测的空间点", example = "[{\"lat\":39.9,\"lng\":116.4,\"properties\":{\"pm25\":150}}]")
    private List<SpatialAnomalyService.AnomalyPoint> points;

    @Schema(description = "检测方法：IQR/LOF/DBSCAN/GRID", example = "IQR")
    private String method;

    @Schema(description = "数值字段名（用于IQR和GRID）", example = "pm25")
    private String valueField;

    @Schema(description = "IQR倍数（默认1.5）", example = "1.5")
    private double iqrMultiplier = 1.5;

    @Schema(description = "LOF邻居数（默认5）", example = "5")
    private int kNeighbors = 5;

    @Schema(description = "LOF阈值（默认1.5）", example = "1.5")
    private double threshold = 1.5;

    @Schema(description = "DBSCAN半径km（默认1.0）", example = "1.0")
    private double epsKm = 1.0;

    @Schema(description = "DBSCAN最小点数（默认3）", example = "3")
    private int minPts = 3;

    @Schema(description = "GRID网格大小km（默认0.5）", example = "0.5")
    private double cellSizeKm = 0.5;

    @Schema(description = "GRID Z-score阈值（默认2.0）", example = "2.0")
    private double zScoreThreshold = 2.0;
}

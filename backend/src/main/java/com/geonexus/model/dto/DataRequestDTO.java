package com.geonexus.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 数据上传请求DTO
 */
@Data
@Schema(description = "数据上传请求")
public class DataUploadRequestDTO {
    
    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名过长")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "文件名包含非法字符")
    @Schema(description = "文件名", example = "hospitals.geojson")
    private String filename;
    
    @NotBlank(message = "数据类型不能为空")
    @Pattern(regexp = "^(GeoJSON|Shapefile|KML|GML|GPX|CSV)$", 
             flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "不支持的数据类型")
    @Schema(description = "数据类型", example = "GeoJSON", allowableValues = {"GeoJSON", "Shapefile", "KML", "GML", "GPX", "CSV"})
    private String dataType;
    
    @Size(max = 1000, message = "CRS过长")
    @Schema(description = "坐标参考系统", example = "EPSG:4326")
    private String crs; // e.g., "EPSG:4326"
    
    @Schema(description = "是否验证数据", example = "true")
    private Boolean validate = true;
    
    @Schema(description = "是否简化几何", example = "false")
    private Boolean simplify = false;
    
    @Min(value = 0, message = "简化容差必须大于0")
    @Max(value = 1, message = "简化容差必须小于1")
    @Schema(description = "Douglas-Peucker简化容差（0-1）", example = "0.001")
    private Double simplificationTolerance;
}

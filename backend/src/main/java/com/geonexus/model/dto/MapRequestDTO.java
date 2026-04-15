package com.geonexus.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 地图生成请求DTO
 */
@Data
@Schema(description = "地图生成请求")
public class MapRequestDTO {
    
    @Schema(description = "GeoJSON数据对象")
    private Map<String, Object> geojson;
    
    @Size(max = 100, message = "中心点坐标过长")
    @Schema(description = "地图中心点坐标", example = "39.9,116.4")
    private String center; // "lat,lng"
    
    @Min(value = 1, message = "缩放级别最小为1")
    @Max(value = 20, message = "缩放级别最大为20")
    @Schema(description = "缩放级别", example = "10")
    private Integer zoom;
    
    @Pattern(regexp = "2d|3d", message = "模式必须是2d或3d")
    @Schema(description = "渲染模式", example = "2d", allowableValues = {"2d", "3d"})
    private String mode;
    
    @Pattern(regexp = "osm|satellite|dark|terrain", message = "不支持的底图类型")
    @Schema(description = "底图类型", example = "osm", allowableValues = {"osm", "satellite", "dark", "terrain"})
    private String tileType;
    
    @Schema(description = "自定义样式配置")
    private Map<String, Object> style;
    
    @Schema(description = "图层配置列表")
    private List<Map<String, Object>> layers;
    
    @Schema(description = "是否显示标注", example = "true")
    private Boolean showLabels = true;
    
    @Schema(description = "是否显示比例尺", example = "true")
    private Boolean showScale = true;
    
    @Min(value = 0, message = "透明度必须在0-1之间")
    @Max(value = 1, message = "透明度必须在0-1之间")
    @Schema(description = "图层透明度", example = "1.0")
    private Double opacity;
}

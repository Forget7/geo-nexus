package com.geonexus.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "地图生成请求")
public class MapGenerateRequest {
    @Schema(description = "GeoJSON数据")
    private Map<String, Object> geojson;
    
    @Schema(description = "地图中心点 [经度, 纬度]", example = "[116.4, 39.9]")
    private List<Double> center = List.of(35.0, 105.0);
    
    @Schema(description = "缩放级别", example = "10", minimum = "1", maximum = "20")
    private Integer zoom = 10;
    
    @Schema(description = "高度（3D模式）", example = "10000")
    private Integer height = 10000;
    
    @Schema(description = "渲染模式", example = "2d", allowableValues = {"2d", "3d"})
    private String mode = "2d";  // 2d or 3d
    
    @Schema(description = "底图类型", example = "osm", allowableValues = {"osm", "satellite", "dark", "terrain"})
    private String tileType = "osm";  // osm, satellite, dark, terrain
    
    @Schema(description = "图层配置列表")
    private List<MapLayer> layers;
    
    @Data
    @Schema(description = "图层配置")
    public static class MapLayer {
        @Schema(description = "图层类型", example = "geojson", allowableValues = {"geojson", "heatmap", "markers", "choropleth"})
        private String type;  // geojson, heatmap, markers, choropleth
        private Object data;
        private Map<String, Object> style;
        @Schema(description = "图层名称", example = "医院分布")
        private String name;
    }
}

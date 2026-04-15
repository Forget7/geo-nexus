package com.geonexus.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

@Data
@Schema(description = "数据格式转换请求")
public class DataConvertRequest {
    @Schema(description = "待转换的GeoJSON数据对象", example = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[116.4,39.9]}}")
    private Map<String, Object> data;
    
    @Schema(description = "输入格式", example = "shapefile", allowableValues = {"geojson", "shapefile", "kml", "gml", "gpx"})
    private String inputFormat;  // geojson, shapefile, kml, gml, gpx
    
    @Schema(description = "输出格式", example = "geojson", allowableValues = {"geojson", "shapefile", "kml", "gml", "gpx"})
    private String outputFormat; // geojson, shapefile, kml, gml, gpx
    
    @Schema(description = "源CRS", example = "EPSG:4490")
    private String sourceCrs;
    
    @Schema(description = "目标CRS", example = "EPSG:4326")
    private String targetCrs;
}

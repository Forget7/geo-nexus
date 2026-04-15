package com.geonexus.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 地图模板 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "地图模板")
public class MapTemplateDTO {

    @Schema(description = "模板ID")
    private String id;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "英文名称")
    private String nameEn;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "分类：government/emergency/traffic/environment/population")
    private String category;

    @Schema(description = "缩略图路径")
    private String thumbnail;

    @Schema(description = "中心纬度")
    private Double centerLat;

    @Schema(description = "中心经度")
    private Double centerLng;

    @Schema(description = "默认缩放级别")
    private Integer zoom;

    @Schema(description = "底图类型：osm/arcgis/cesium-ion")
    private String tileType;

    @Schema(description = "预设图层配置")
    private List<LayerConfigDTO> layers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "图层配置")
    public static class LayerConfigDTO {
        @Schema(description = "图层类型：marker/label/polygon/heatmap/3dtiles")
        private String type;
        private String name;
        private Double lat;
        private Double lng;
        private String icon;
        private String style;
        private String geojson;
        private Integer opacity;
    }
}

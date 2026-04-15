package com.geonexus.config;

import com.geonexus.model.dto.MapTemplateDTO;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地图模板配置类
 * 定义所有预设的行业模板
 */
@Configuration
public class MapTemplatesConfig {

    @Bean
    public Map<String, MapTemplateDTO> mapTemplates() {
        Map<String, MapTemplateDTO> templates = new HashMap<>();

        // 1. 城市概览
        templates.put("city-overview", MapTemplateDTO.builder()
                .id("city-overview")
                .name("城市概览")
                .nameEn("City Overview")
                .description("城市全貌展示，适合政务公开")
                .category("government")
                .thumbnail("/templates/city-overview.png")
                .centerLat(39.9042)
                .centerLng(116.4074)
                .zoom(12)
                .tileType("osm")
                .layers(List.of(
                        MapTemplateDTO.LayerConfigDTO.builder()
                                .type("marker")
                                .name("天安门")
                                .lat(39.9042)
                                .lng(116.3974)
                                .icon("landmark")
                                .build(),
                        MapTemplateDTO.LayerConfigDTO.builder()
                                .type("label")
                                .name("北京市")
                                .style("city-label")
                                .build()
                ))
                .build());

        // 2. 灾害监测
        templates.put("disaster-monitoring", MapTemplateDTO.builder()
                .id("disaster-monitoring")
                .name("灾害监测")
                .nameEn("Disaster Monitoring")
                .description("台风/洪涝实时监控，支持时空轨迹数据")
                .category("emergency")
                .thumbnail("/templates/disaster-monitoring.png")
                .centerLat(23.5)
                .centerLng(121.0)
                .zoom(6)
                .tileType("satellite")
                .layers(List.of(
                        MapTemplateDTO.LayerConfigDTO.builder()
                                .type("heatmap")
                                .name("台风路径")
                                .style("heatmap-style")
                                .build()
                ))
                .build());

        // 3. 交通分析
        templates.put("traffic-analysis", MapTemplateDTO.builder()
                .id("traffic-analysis")
                .name("交通分析")
                .nameEn("Traffic Analysis")
                .description("城市交通态势，拥堵热力图和主干道标注")
                .category("traffic")
                .thumbnail("/templates/traffic-analysis.png")
                .centerLat(31.2304)
                .centerLng(121.4737)
                .zoom(11)
                .tileType("osm")
                .layers(List.of(
                        MapTemplateDTO.LayerConfigDTO.builder()
                                .type("heatmap")
                                .name("拥堵热力")
                                .style("traffic-heatmap")
                                .build(),
                        MapTemplateDTO.LayerConfigDTO.builder()
                                .type("label")
                                .name("主干道标注")
                                .style("road-label")
                                .build()
                ))
                .build());

        // 4. 人口密度
        templates.put("population-density", MapTemplateDTO.builder()
                .id("population-density")
                .name("人口密度")
                .nameEn("Population Density")
                .description("区域人口分布，分级设色可视化")
                .category("population")
                .thumbnail("/templates/population-density.png")
                .centerLat(39.9042)
                .centerLng(116.4074)
                .zoom(10)
                .tileType("osm")
                .layers(List.of(
                        MapTemplateDTO.LayerConfigDTO.builder()
                                .type("polygon")
                                .name("人口密度分区")
                                .style("choropleth-population")
                                .build()
                ))
                .build());

        // 5. 环境评估
        templates.put("environment-assessment", MapTemplateDTO.builder()
                .id("environment-assessment")
                .name("环境评估")
                .nameEn("Environment Assessment")
                .description("空气质量/绿化覆盖，绿色→红色色阶")
                .category("environment")
                .thumbnail("/templates/environment-assessment.png")
                .centerLat(31.2304)
                .centerLng(121.4737)
                .zoom(10)
                .tileType("satellite")
                .layers(List.of(
                        MapTemplateDTO.LayerConfigDTO.builder()
                                .type("heatmap")
                                .name("空气质量")
                                .style("aqi-heatmap")
                                .opacity(80)
                                .build()
                ))
                .build());

        // 6. 应急指挥
        templates.put("emergency-response", MapTemplateDTO.builder()
                .id("emergency-response")
                .name("应急指挥")
                .nameEn("Emergency Response")
                .description("灾害应急响应，3D地形与POI图层")
                .category("emergency")
                .thumbnail("/templates/emergency-response.png")
                .centerLat(39.9042)
                .centerLng(116.4074)
                .zoom(13)
                .tileType("terrain")
                .layers(List.of(
                        MapTemplateDTO.LayerConfigDTO.builder()
                                .type("marker")
                                .name("医院")
                                .style("hospital-marker")
                                .icon("hospital")
                                .build(),
                        MapTemplateDTO.LayerConfigDTO.builder()
                                .type("marker")
                                .name("消防站")
                                .style("fire-station-marker")
                                .icon("fire")
                                .build(),
                        MapTemplateDTO.LayerConfigDTO.builder()
                                .type("marker")
                                .name("警察局")
                                .style("police-marker")
                                .icon("police")
                                .build()
                ))
                .build());

        return templates;
    }
}

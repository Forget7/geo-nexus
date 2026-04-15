package com.geonexus.api.v1;

import com.geonexus.model.MapGenerateRequest;
import com.geonexus.service.MapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 地图控制器集成测试
 */
class MapControllerTest {
    
    private MapService mapService;
    
    @BeforeEach
    void setUp() {
        mapService = new MapService(
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
        // 设置输出目录
        try {
            var field = MapService.class.getDeclaredField("outputDir");
            field.setAccessible(true);
            field.set(mapService, "./target/test-output");
        } catch (Exception e) {
            // 忽略
        }
    }
    
    @Test
    @DisplayName("生成2D Leaflet地图")
    void testGenerate2DMap() {
        MapGenerateRequest request = new MapGenerateRequest();
        request.setMode("2d");
        request.setCenter(java.util.List.of(39.9042, 116.4074));
        request.setZoom(12);
        request.setTileType("osm");
        
        var response = mapService.generateMap(request);
        
        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getUrl());
        assertTrue(response.getUrl().contains("/html"));
    }
    
    @Test
    @DisplayName("生成3D Cesium地图")
    void testGenerate3DMap() {
        MapGenerateRequest request = new MapGenerateRequest();
        request.setMode("3d");
        request.setCenter(java.util.List.of(39.9042, 116.4074));
        request.setHeight(5000);
        
        var response = mapService.generateMap(request);
        
        assertNotNull(response);
        assertNotNull(response.getUrl3d());
    }
    
    @Test
    @DisplayName("带GeoJSON数据生成地图")
    void testGenerateMapWithGeoJSON() {
        MapGenerateRequest request = new MapGenerateRequest();
        request.setMode("2d");
        request.setCenter(java.util.List.of(39.9042, 116.4074));
        
        // 添加简单GeoJSON
        String geojson = """
            {
                "type": "Feature",
                "properties": {"name": "Test Point"},
                "geometry": {
                    "type": "Point",
                    "coordinates": [116.4074, 39.9042]
                }
            }
            """;
        
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            request.setGeojson(mapper.readValue(geojson, java.util.Map.class));
        } catch (Exception e) {
            fail("GeoJSON解析失败");
        }
        
        var response = mapService.generateMap(request);
        
        assertNotNull(response);
        assertNotNull(response.getId());
    }
    
    @Test
    @DisplayName("获取不存在的地图返回null")
    void testGetNonExistentMap() {
        var result = mapService.getMapInfo("non-existent-id");
        
        assertTrue(result.containsKey("error") || result == null);
    }
}

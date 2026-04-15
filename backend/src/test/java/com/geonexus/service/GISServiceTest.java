package com.geonexus.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GIS服务单元测试
 */
class GISServiceTest {
    
    private final GISService gisService = new GISService(new com.fasterxml.jackson.databind.ObjectMapper());
    
    @Test
    @DisplayName("计算两点间距离 - Haversine公式")
    void testCalculateDistance() {
        // 北京到上海约1068公里
        var result = gisService.calculateDistance(39.9042, 116.4074, 31.2304, 121.4737, "km");
        
        assertTrue(result.containsKey("distance"));
        assertEquals("km", result.get("unit"));
        
        double distance = (double) result.get("distance");
        // 允许一定误差
        assertTrue(distance > 1000 && distance < 1200, 
                "北京到上海距离应该在1000-1200km之间，实际: " + distance);
    }
    
    @Test
    @DisplayName("计算距离 - 米为单位")
    void testCalculateDistanceInMeters() {
        var result = gisService.calculateDistance(39.9042, 116.4074, 39.9142, 116.4174, "m");
        
        assertEquals("m", result.get("unit"));
        double distance = (double) result.get("distance");
        // 约1.4km
        assertTrue(distance > 1000 && distance < 2000);
    }
    
    @Test
    @DisplayName("计算距离 - 英里为单位")
    void testCalculateDistanceInMiles() {
        var result = gisService.calculateDistance(39.9042, 116.4074, 31.2304, 121.4737, "miles");
        
        assertEquals("miles", result.get("unit"));
        double distance = (double) result.get("distance");
        // 约663英里
        assertTrue(distance > 600 && distance < 750);
    }
    
    @Test
    @DisplayName("相同点距离为0")
    void testCalculateDistanceSamePoint() {
        var result = gisService.calculateDistance(39.9042, 116.4074, 39.9042, 116.4074, "km");
        
        assertEquals(0.0, result.get("distance"));
    }
    
    @Test
    @DisplayName("列出所有工具")
    void testListTools() {
        var tools = gisService.listTools();
        
        assertNotNull(tools);
        assertTrue(tools.size() > 0);
        
        // 验证包含关键工具
        var toolNames = tools.stream()
                .map(t -> t.getName())
                .toList();
        
        assertTrue(toolNames.contains("calculate_distance"));
        assertTrue(toolNames.contains("buffer_analysis"));
        assertTrue(toolNames.contains("geocode"));
    }
    
    @Test
    @DisplayName("执行距离计算工具")
    void testExecuteDistanceTool() {
        var request = new com.geonexus.model.ToolExecuteRequest();
        request.setTool("calculate_distance");
        request.setParams(java.util.Map.of(
                "point1", java.util.List.of(39.9042, 116.4074),
                "point2", java.util.List.of(31.2304, 121.4737),
                "unit", "km"
        ));
        
        var result = gisService.executeTool(request);
        
        assertNotNull(result);
        assertFalse(result.containsKey("error"), "不应该有错误: " + result.get("error"));
    }
    
    @Test
    @DisplayName("未知工具返回错误")
    void testExecuteUnknownTool() {
        var request = new com.geonexus.model.ToolExecuteRequest();
        request.setTool("unknown_tool");
        request.setParams(java.util.Map.of());
        
        var result = gisService.executeTool(request);
        
        assertTrue(result.containsKey("error"));
    }
}

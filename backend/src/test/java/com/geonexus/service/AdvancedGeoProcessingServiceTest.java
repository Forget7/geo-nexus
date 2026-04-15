package com.geonexus.service;

import org.junit.jupiter.api.*;
import org.locationtech.jts.geom.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 高级GIS处理服务测试
 */
class AdvancedGeoProcessingServiceTest {
    
    private AdvancedGeoProcessingService service;
    
    @BeforeEach
    void setUp() {
        service = new AdvancedGeoProcessingService(new com.fasterxml.jackson.databind.ObjectMapper());
    }
    
    @Nested
    @DisplayName("Delaunay三角剖分测试")
    class DelaunayTriangulationTests {
        
        @Test
        @DisplayName("应该正确生成三角网格")
        void shouldGenerateTriangulation() {
            List<List<Double>> points = List.of(
                    List.of(0.0, 0.0),
                    List.of(1.0, 0.0),
                    List.of(0.5, 1.0),
                    List.of(1.5, 1.0),
                    List.of(1.5, 0.0)
            );
            
            Map<String, Object> result = service.delaunayTriangulation(points);
            
            assertNotNull(result);
            assertFalse(result.containsKey("error"));
            assertEquals("FeatureCollection", result.get("type"));
            assertTrue((Integer) result.get("count") > 0);
        }
        
        @Test
        @DisplayName("点数不足应返回错误")
        void shouldReturnErrorForInsufficientPoints() {
            List<List<Double>> points = List.of(
                    List.of(0.0, 0.0),
                    List.of(1.0, 0.0)
            );
            
            Map<String, Object> result = service.delaunayTriangulation(points);
            
            assertTrue(result.containsKey("error"));
        }
        
        @Test
        @DisplayName("空列表应返回错误")
        void shouldReturnErrorForEmptyList() {
            Map<String, Object> result = service.delaunayTriangulation(List.of());
            assertTrue(result.containsKey("error"));
        }
    }
    
    @Nested
    @DisplayName("Voronoi图测试")
    class VoronoiDiagramTests {
        
        @Test
        @DisplayName("应该正确生成Voronoi图")
        void shouldGenerateVoronoiDiagram() {
            List<List<Double>> points = List.of(
                    List.of(0.0, 0.0),
                    List.of(1.0, 0.0),
                    List.of(0.5, 1.0)
            );
            
            List<Double> boundingBox = List.of(-1.0, 2.0, -1.0, 2.0);
            
            Map<String, Object> result = service.voronoiDiagram(points, boundingBox);
            
            assertNotNull(result);
            assertFalse(result.containsKey("error"));
            assertEquals("FeatureCollection", result.get("type"));
        }
        
        @Test
        @DisplayName("单点应返回错误")
        void shouldReturnErrorForSinglePoint() {
            List<List<Double>> points = List.of(List.of(0.0, 0.0));
            
            Map<String, Object> result = service.voronoiDiagram(points, null);
            
            assertTrue(result.containsKey("error"));
        }
    }
    
    @Nested
    @DisplayName("线段合并测试")
    class MergeLinesTests {
        
        @Test
        @DisplayName("应该正确合并线段")
        void shouldMergeLines() {
            List<List<List<Double>>> lineStrings = List.of(
                    List.of(List.of(0.0, 0.0), List.of(1.0, 0.0), List.of(1.0, 1.0)),
                    List.of(List.of(1.0, 1.0), List.of(2.0, 1.0), List.of(2.0, 2.0))
            );
            
            Map<String, Object> result = service.mergeLines(lineStrings);
            
            assertNotNull(result);
            assertFalse(result.containsKey("error"));
            assertEquals("FeatureCollection", result.get("type"));
            assertEquals(2, result.get("originalCount"));
        }
    }
    
    @Nested
    @DisplayName("多边形融合测试")
    class DissolvePolygonsTests {
        
        @Test
        @DisplayName("应该融合相邻多边形")
        void shouldDissolvePolygons() {
            // 两个相邻的方形
            List<List<List<Double>>> polygons = List.of(
                    List.of(
                            List.of(0.0, 0.0), List.of(1.0, 0.0),
                            List.of(1.0, 1.0), List.of(0.0, 1.0),
                            List.of(0.0, 0.0)
                    ),
                    List.of(
                            List.of(1.0, 0.0), List.of(2.0, 0.0),
                            List.of(2.0, 1.0), List.of(1.0, 1.0),
                            List.of(1.0, 0.0)
                    )
            );
            
            Map<String, Object> result = service.dissolvePolygons(polygons, null, null);
            
            assertNotNull(result);
            assertFalse(result.containsKey("error"));
            assertEquals(1, result.get("dissolvedCount")); // 应该融合成一个
        }
    }
    
    @Nested
    @DisplayName("缓冲区分析测试")
    class BufferWithEndcapsTests {
        
        @Test
        @DisplayName("应该正确生成round缓冲区")
        void shouldGenerateRoundBuffer() {
            List<List<Double>> points = List.of(
                    List.of(0.0, 0.0),
                    List.of(1.0, 0.0)
            );
            
            Map<String, Object> result = service.bufferWithEndcaps(points, 1.0, "round");
            
            assertNotNull(result);
            assertFalse(result.containsKey("error"));
            assertEquals(2, result.get("bufferCount"));
        }
        
        @Test
        @DisplayName("应该正确生成square缓冲区")
        void shouldGenerateSquareBuffer() {
            List<List<Double>> points = List.of(List.of(0.0, 0.0));
            
            Map<String, Object> result = service.bufferWithEndcaps(points, 1.0, "square");
            
            assertNotNull(result);
            assertFalse(result.containsKey("error"));
        }
        
        @Test
        @DisplayName("应该正确生成flat缓冲区")
        void shouldGenerateFlatBuffer() {
            List<List<Double>> points = List.of(List.of(0.0, 0.0));
            
            Map<String, Object> result = service.bufferWithEndcaps(points, 1.0, "flat");
            
            assertNotNull(result);
            assertFalse(result.containsKey("error"));
        }
    }
    
    @Nested
    @DisplayName("几何验证测试")
    class ValidateAndRepairTests {
        
        @Test
        @DisplayName("应该验证有效几何")
        void shouldValidateValidGeometry() {
            List<List<List<Double>>> polygons = List.of(
                    List.of(
                            List.of(0.0, 0.0), List.of(1.0, 0.0),
                            List.of(1.0, 1.0), List.of(0.0, 1.0),
                            List.of(0.0, 0.0)
                    )
            );
            
            Map<String, Object> result = service.validateAndRepair(polygons);
            
            assertNotNull(result);
            assertEquals(1, result.get("total"));
            assertEquals(1, result.get("valid"));
            assertEquals(0, result.get("invalid"));
        }
        
        @Test
        @DisplayName("应该检测并修复无效几何")
        void shouldDetectAndRepairInvalidGeometry() {
            // 自相交多边形
            List<List<List<Double>>> polygons = List.of(
                    List.of(
                            List.of(0.0, 0.0), List.of(2.0, 0.0),
                            List.of(0.0, 2.0), List.of(2.0, 2.0),
                            List.of(0.0, 0.0)
                    )
            );
            
            Map<String, Object> result = service.validateAndRepair(polygons);
            
            assertNotNull(result);
            // 应该尝试修复
            assertTrue((Integer) result.get("total") > 0);
        }
    }
}

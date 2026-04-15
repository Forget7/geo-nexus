package com.geonexus.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.locationtech.jts.geom.*;
import java.util.Map;

/**
 * GeometryCalcService 单元测试
 */
class GeometryCalcServiceTest {
    
    private final GeometryCalcService service = new GeometryCalcService();
    private final GeometryFactory gf = new GeometryFactory();
    
    @Test
    void testHaversineDistance() {
        // 北京到上海约 1068 km
        Map<String, Object> result = service.haversineDistance(39.9042, 116.4074, 31.2304, 121.4737, "km");
        
        double distance = (Double) result.get("distance");
        assertTrue(distance > 1000 && distance < 1200, "北京到上海距离应在 1000-1200 km 之间，实际: " + distance);
    }
    
    @Test
    void testHaversineDistanceMeters() {
        Map<String, Object> result = service.haversineDistance(39.9042, 116.4074, 31.2304, 121.4737, "m");
        double distance = (Double) result.get("distance");
        assertTrue(distance > 1_000_000, "距离应该大于 100 万米");
        assertEquals("m", result.get("unit"));
    }
    
    @Test
    void testCalculateArea() {
        // 创建 1 度 x 1 度的正方形，约 111km x 111km = 12321 sqkm
        Coordinate[] coords = {
            new Coordinate(0, 0),
            new Coordinate(1, 0),
            new Coordinate(1, 1),
            new Coordinate(0, 1),
            new Coordinate(0, 0)
        };
        Polygon polygon = gf.createPolygon(gf.createLineString(coords));
        
        double area = service.calculateArea(polygon, "sqkm");
        assertTrue(area > 10000 && area < 15000, "面积应在 10000-15000 sqkm 之间，实际: " + area);
    }
    
    @Test
    void testCalculateLength() {
        // 创建 1 度长的线，约 111 km
        Coordinate[] coords = {
            new Coordinate(0, 0),
            new Coordinate(1, 0)
        };
        LineString line = gf.createLineString(coords);
        
        double length = service.calculateLength(line, "km");
        assertTrue(length > 100 && length < 130, "长度应在 100-130 km 之间，实际: " + length);
    }
    
    @Test
    void testCalculateCentroid() {
        Coordinate[] coords = {
            new Coordinate(0, 0),
            new Coordinate(4, 0),
            new Coordinate(4, 4),
            new Coordinate(0, 4),
            new Coordinate(0, 0)
        };
        Polygon polygon = gf.createPolygon(gf.createLineString(coords));
        
        Point centroid = service.calculateCentroid(polygon);
        assertEquals(2.0, centroid.getX(), 0.001);
        assertEquals(2.0, centroid.getY(), 0.001);
    }
    
    @Test
    void testGeometryDistance() {
        Point p1 = gf.createPoint(new Coordinate(0, 0));
        Point p2 = gf.createPoint(new Coordinate(0.01, 0)); // 约 1.1 km
        
        double dist = service.geometryDistance(p1, p2, "km");
        assertTrue(dist > 1 && dist < 2, "距离应在 1-2 km 之间，实际: " + dist);
    }
    
    @Test
    void testReprojectGeometry() {
        // WGS84 点
        Point wgs84 = gf.createPoint(new Coordinate(116.4074, 39.9042));
        
        // 转换为 Web Mercator 再转回
        Geometry mercator = service.reprojectGeometry(wgs84, "EPSG:4326", "EPSG:3857");
        Geometry back = service.reprojectGeometry(mercator, "EPSG:3857", "EPSG:4326");
        
        // 转换精度会有损失
        assertEquals(wgs84.getX(), back.getCoordinate().x, 0.01);
        assertEquals(wgs84.getY(), back.getCoordinate().y, 0.01);
    }
    
    @Test
    void testTransformToEqualArea() {
        Point p = gf.createPoint(new Coordinate(116.4074, 39.9042));
        Geometry result = service.transformToEqualArea(p);
        assertNotNull(result);
    }
    
    @Test
    void testTransformToEqualDistance() {
        Point p = gf.createPoint(new Coordinate(116.4074, 39.9042));
        Geometry result = service.transformToEqualDistance(p);
        assertNotNull(result);
    }
}

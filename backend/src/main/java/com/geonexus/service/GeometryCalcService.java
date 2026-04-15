package com.geonexus.service;

import com.geonexus.common.exception.GeometryProcessException;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 几何计算服务
 * 职责：基础几何计算（距离/面积/长度/质心）
 */
@Slf4j
@Service
public class GeometryCalcService {
    
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    private final GeometryFactory geometryFactory;
    
    public GeometryCalcService() {
        this.geometryFactory = new GeometryFactory(new PackedCoordinateSequenceFactory());
    }
    
    // ==================== 距离计算 ====================
    
    /**
     * 使用Haversine公式计算两点间距离
     */
    public Map<String, Object> haversineDistance(Double lat1, Double lon1, 
                                                   Double lat2, Double lon2, String unit) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;
        
        if ("m".equals(unit)) {
            distance *= 1000;
        } else if ("miles".equals(unit)) {
            distance /= 1.60934;
        }
        
        return Map.of(
            "distance", Math.round(distance * 1000.0) / 1000.0,
            "unit", unit != null ? unit : "km",
            "point1", List.of(lat1, lon1),
            "point2", List.of(lat2, lon2)
        );
    }
    
    /**
     * 计算两个几何间的距离
     */
    public double geometryDistance(Geometry geom1, Geometry geom2, String unit) {
        try {
            Geometry proj1 = reprojectGeometry(geom1, "EPSG:4326", "EPSG:3857");
            Geometry proj2 = reprojectGeometry(geom2, "EPSG:4326", "EPSG:3857");
            
            double dist = proj1.distance(proj2);
            
            if ("m".equals(unit)) {
                // 已经是米
            } else if ("km".equals(unit)) {
                dist /= 1000;
            } else if ("miles".equals(unit)) {
                dist /= 1609.34;
            }
            
            return Math.round(dist * 1000.0) / 1000.0;
        } catch (Exception e) {
            throw new GeometryProcessException("计算几何距离失败", e);
        }
    }
    
    // ==================== 面积计算 ====================
    
    /**
     * 计算几何面积
     */
    public double calculateArea(Geometry geom, String unit) {
        try {
            // 使用等面积投影计算
            Geometry projected = transformToEqualArea(geom);
            double area = projected.getArea();
            
            if ("sqkm".equals(unit)) {
                area /= 1_000_000;
            } else if ("hectares".equals(unit)) {
                area /= 10000;
            } else if ("sqm".equals(unit)) {
                // 已经是平方米
            }
            
            return Math.round(area * 1000.0) / 1000.0;
        } catch (Exception e) {
            throw new GeometryProcessException("计算面积失败", e);
        }
    }
    
    // ==================== 长度计算 ====================
    
    /**
     * 计算几何长度/周长
     */
    public double calculateLength(Geometry geom, String unit) {
        try {
            // 使用等距离投影计算
            Geometry projected = transformToEqualDistance(geom);
            double length = projected.getLength();
            
            if ("m".equals(unit)) {
                length *= 1000;
            } else if ("km".equals(unit)) {
                length /= 1000;
            } else if ("miles".equals(unit)) {
                length /= 1609.34;
            }
            
            return Math.round(length * 1000.0) / 1000.0;
        } catch (Exception e) {
            throw new GeometryProcessException("计算长度失败", e);
        }
    }
    
    // ==================== 质心 ====================
    
    /**
     * 计算几何质心
     */
    public Point calculateCentroid(Geometry geom) {
        if (geom == null) {
            throw new GeometryProcessException("几何对象为空");
        }
        return geom.getCentroid();
    }
    
    // ==================== 投影转换辅助方法 ====================
    
    /**
     * 投影转换
     */
    public Geometry reprojectGeometry(Geometry geom, String fromCrs, String toCrs) {
        try {
            CoordinateReferenceSystem from = CRS.decode(fromCrs);
            CoordinateReferenceSystem to = CRS.decode(toCrs);
            MathTransform transform = CRS.findMathTransform(from, to, true);
            return JTS.transform(geom, transform);
        } catch (Exception e) {
            log.warn("投影转换失败，使用原始几何: {}", e.getMessage());
            return geom;
        }
    }
    
    /**
     * 转换到等面积投影
     */
    public Geometry transformToEqualArea(Geometry geom) {
        try {
            return reprojectGeometry(geom, "EPSG:4326", "EPSG:6933");
        } catch (Exception e) {
            return geom;
        }
    }
    
    /**
     * 转换到等距离投影
     */
    public Geometry transformToEqualDistance(Geometry geom) {
        try {
            return reprojectGeometry(geom, "EPSG:4326", "EPSG:3857");
        } catch (Exception e) {
            return geom;
        }
    }
}

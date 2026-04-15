package com.geonexus.service;

import com.geonexus.common.exception.GeometryProcessException;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 坐标投影服务
 * 职责：坐标投影转换
 */
@Slf4j
@Service
public class ProjectionService {
    
    private final GeometryFactory geometryFactory;
    
    public ProjectionService() {
        this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
    }
    
    // ==================== 坐标转换 ====================
    
    /**
     * 坐标投影转换
     */
    public Geometry transform(Geometry geom, String fromCrs, String toCrs) {
        if (geom == null) {
            throw new GeometryProcessException("几何对象为空");
        }
        
        if (fromCrs == null || toCrs == null) {
            throw new GeometryProcessException("CRS参数不能为空");
        }
        
        try {
            CoordinateReferenceSystem from = CRS.decode(fromCrs);
            CoordinateReferenceSystem to = CRS.decode(toCrs);
            MathTransform mathTransform = CRS.findMathTransform(from, to, true);
            return JTS.transform(geom, mathTransform);
        } catch (Exception e) {
            log.warn("坐标转换失败 from {} to {}: {}", fromCrs, toCrs, e.getMessage());
            throw new GeometryProcessException("坐标转换失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== CRS信息 ====================
    
    /**
     * 获取CRS信息
     */
    public Map<String, Object> getCRSInfo(String epsgCode) {
        try {
            CoordinateReferenceSystem crs = CRS.decode(epsgCode);
            
            return Map.of(
                "code", epsgCode,
                "name", crs.getName().getCode(),
                "type", crs.getType().getName(),
                "isGeographic", CRS.isGeographic(crs),
                "isProjected", CRS.isProjected(crs)
            );
        } catch (Exception e) {
            throw new GeometryProcessException("获取CRS信息失败: " + epsgCode, e);
        }
    }
    
    // ==================== 常用转换快捷方法 ====================
    
    /**
     * WGS84转Web Mercator
     */
    public Geometry toWebMercator(Geometry geom) {
        return transform(geom, "EPSG:4326", "EPSG:3857");
    }
    
    /**
     * Web Mercator转WGS84
     */
    public Geometry toWGS84(Geometry geom) {
        return transform(geom, "EPSG:3857", "EPSG:4326");
    }
    
    /**
     * 判断是否为地理坐标系
     */
    public boolean isGeographic(String epsgCode) {
        try {
            CoordinateReferenceSystem crs = CRS.decode(epsgCode);
            return CRS.isGeographic(crs);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 判断是否为投影坐标系
     */
    public boolean isProjected(String epsgCode) {
        try {
            CoordinateReferenceSystem crs = CRS.decode(epsgCode);
            return CRS.isProjected(crs);
        } catch (Exception e) {
            return false;
        }
    }
}

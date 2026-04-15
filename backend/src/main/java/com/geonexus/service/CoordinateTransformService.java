package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 坐标转换服务 - CRS转换与投影
 */
@Slf4j
@Service
public class CoordinateTransformService {
    
    // EPSG注册表
    private final Map<String, CoordinateSystem> crsRegistry = new ConcurrentHashMap<>();
    
    // 转换缓存
    private final Map<String, Transformation> transformationCache = new ConcurrentHashMap<>();
    
    public CoordinateTransformService() {
        initializeCRARegistry();
    }
    
    private void initializeCRARegistry() {
        // 地理坐标系
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4326")
                .name("WGS 84")
                .type("geographic")
                .unit("degree")
                .bounds(new double[]{-180, -90, 180, 90})
                .proj4("+proj=longlat +datum=WGS84 +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4490")
                .name("China Geodetic Coordinate System 2000")
                .type("geographic")
                .unit("degree")
                .bounds(new double[]{-180, -90, 180, 90})
                .proj4("+proj=longlat +ellps=GRS80 +no_defs")
                .build());
        
        // 投影坐标系
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:3857")
                .name("Web Mercator")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{-20037508.34, -20037508.34, 20037508.34, 20037508.34})
                .proj4("+proj=merc +a=6378137 +b=6378137 +lat_ts=0 +lon_0=0 +x_0=0 +y_0=0 +k=1 +units=m +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:3857")
                .name("Pseudo Mercator")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{-20037508.34, -20037508.34, 20037508.34, 20037508.34})
                .proj4("+proj=merc +a=6378137 +b=6378137 +lat_ts=0 +lon_0=0 +x_0=0 +y_0=0 +k=1 +units=m +no_defs")
                .build());
        
        // 中国常用坐标系
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4214")
                .name("Beijing 1954")
                .type("geographic")
                .unit("degree")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=longlat +ellps=krass +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:2433")
                .name("Beijing 1954 / Gauss-Kruger CM 75E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=75 +k=1 +x_0=25500000 +y_0=0 +ellps=krass +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:2434")
                .name("Beijing 1954 / Gauss-Kruger CM 81E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=81 +k=1 +x_0=26500000 +y_0=0 +ellps=krass +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:2435")
                .name("Beijing 1954 / Gauss-Kruger CM 87E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=87 +k=1 +x_0=27500000 +y_0=0 +ellps=krass +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:2436")
                .name("Beijing 1954 / Gauss-Kruger CM 93E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=93 +k=1 +x_0=28500000 +y_0=0 +ellps=krass +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:2437")
                .name("Beijing 1954 / Gauss-Kruger CM 99E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=99 +k=1 +x_0=29500000 +y_0=0 +ellps=krass +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:2438")
                .name("Beijing 1954 / Gauss-Kruger CM 105E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=105 +k=1 +x_0=30500000 +y_0=0 +ellps=krass +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:2439")
                .name("Beijing 1954 / Gauss-Kruger CM 111E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=111 +k=1 +x_0=31500000 +y_0=0 +ellps=krass +no_defs")
                .build());
        
        // CGCS2000 投影
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4491")
                .name("CGCS2000 / Gauss-Kruger CM 75E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=75 +k=1 +x_0=25500000 +y_0=0 +ellps=GRS80 +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4492")
                .name("CGCS2000 / Gauss-Kruger CM 81E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=81 +k=1 +x_0=26500000 +y_0=0 +ellps=GRS80 +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4493")
                .name("CGCS2000 / Gauss-Kruger CM 87E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=87 +k=1 +x_0=27500000 +y_0=0 +ellps=GRS80 +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4494")
                .name("CGCS2000 / Gauss-Kruger CM 93E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=93 +k=1 +x_0=28500000 +y_0=0 +ellps=GRS80 +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4495")
                .name("CGCS2000 / Gauss-Kruger CM 99E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=99 +k=1 +x_0=29500000 +y_0=0 +ellps=GRS80 +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4496")
                .name("CGCS2000 / Gauss-Kruger CM 105E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=105 +k=1 +x_0=30500000 +y_0=0 +ellps=GRS80 +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4497")
                .name("CGCS2000 / Gauss-Kruger CM 111E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=111 +k=1 +x_0=31500000 +y_0=0 +ellps=GRS80 +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4498")
                .name("CGCS2000 / Gauss-Kruger CM 117E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=117 +k=1 +x_0=32500000 +y_0=0 +ellps=GRS80 +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:4499")
                .name("CGCS2000 / Gauss-Kruger CM 123E")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{72, 18, 135, 54})
                .proj4("+proj=tmerc +lat_0=0 +lon_0=123 +k=1 +x_0=33500000 +y_0=0 +ellps=GRS80 +no_defs")
                .build());
        
        // UTM 投影
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:32650")
                .name("WGS 84 / UTM zone 50N")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{114, 0, 120, 84})
                .proj4("+proj=utm +zone=50 +datum=WGS84 +units=m +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:32651")
                .name("WGS 84 / UTM zone 51N")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{120, 0, 126, 84})
                .proj4("+proj=utm +zone=51 +datum=WGS84 +units=m +no_defs")
                .build());
        
        registerCRS(CoordinateSystem.builder()
                .epsg("EPSG:32652")
                .name("WGS 84 / UTM zone 52N")
                .type("projected")
                .unit("meter")
                .bounds(new double[]{126, 0, 132, 84})
                .proj4("+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs")
                .build());
    }
    
    // ==================== CRS管理 ====================
    
    private void registerCRS(CoordinateSystem crs) {
        crsRegistry.put(crs.getEpsg(), crs);
    }
    
    /**
     * 获取CRS信息
     */
    public CoordinateSystem getCRS(String epsg) {
        return crsRegistry.get(epsg);
    }
    
    /**
     * 搜索CRS
     */
    public List<CoordinateSystem> searchCRS(String keyword) {
        String k = keyword.toLowerCase();
        return crsRegistry.values().stream()
                .filter(crs -> crs.getEpsg().toLowerCase().contains(k) ||
                        crs.getName().toLowerCase().contains(k))
                .toList();
    }
    
    /**
     * 获取所有CRS
     */
    public List<CoordinateSystem> getAllCRS() {
        return new ArrayList<>(crsRegistry.values());
    }
    
    /**
     * 获取特定类型的CRS
     */
    public List<CoordinateSystem> getCRSByType(String type) {
        return crsRegistry.values().stream()
                .filter(crs -> crs.getType().equals(type))
                .toList();
    }
    
    // ==================== 坐标转换 ====================
    
    /**
     * 单点转换
     */
    public double[] transform(double[] point, String fromEpsg, String toEpsg) {
        CoordinateSystem from = crsRegistry.get(fromEpsg);
        CoordinateSystem to = crsRegistry.get(toEpsg);
        
        if (from == null || to == null) {
            throw new CRSNotFoundException("CRS not found: " + fromEpsg + " -> " + toEpsg);
        }
        
        // 如果相同
        if (fromEpsg.equals(toEpsg)) {
            return point;
        }
        
        // 实际应使用Proj4j或GeoTools进行转换
        // 这里简化实现
        log.info("坐标转换: {} -> {}, point={}", fromEpsg, toEpsg, Arrays.toString(point));
        
        // WGS84 <-> Web Mercator 简化转换
        if ("EPSG:4326".equals(fromEpsg) && "EPSG:3857".equals(toEpsg)) {
            double lon = point[0] * 20037508.34 / 180;
            double lat = Math.log(Math.tan((90 + point[1]) * Math.PI / 360)) / (Math.PI / 180) * 20037508.34 / 180;
            return new double[]{lon, lat};
        }
        
        if ("EPSG:3857".equals(fromEpsg) && "EPSG:4326".equals(toEpsg)) {
            double lon = point[0] / 20037508.34 * 180;
            double lat = point[1] / 20037508.34 * 180;
            lat = 180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180)) - Math.PI / 2);
            return new double[]{lon, lat};
        }
        
        // 默认返回原值
        return point;
    }
    
    /**
     * 批量转换
     */
    public List<double[]> transformBatch(List<double[]> points, String fromEpsg, String toEpsg) {
        return points.stream()
                .map(p -> transform(p, fromEpsg, toEpsg))
                .toList();
    }
    
    /**
     * GeoJSON转换
     */
    public String transformGeoJSON(String geojson, String fromEpsg, String toEpsg) {
        log.info("GeoJSON坐标转换: {} -> {}", fromEpsg, toEpsg);
        // 实际应解析GeoJSON并转换每个坐标
        return geojson;
    }
    
    // ==================== 投影转换 ====================
    
    /**
     * 经纬度转投影坐标
     */
    public double[] lonLatToXY(double lon, double lat, String targetEpsg) {
        return transform(new double[]{lon, lat}, "EPSG:4326", targetEpsg);
    }
    
    /**
     * 投影坐标转经纬度
     */
    public double[] xyToLonLat(double x, double y, String sourceEpsg) {
        return transform(new double[]{x, y}, sourceEpsg, "EPSG:4326");
    }
    
    /**
     * 计算中央经线
     */
    public double calculateCentralMeridian(double longitude) {
        int zone = (int) ((longitude - 1.5) / 3) + 1;
        return zone * 3.0;
    }
    
    /**
     * 获取UTM zone
     */
    public int getUTMZone(double longitude) {
        return (int) ((longitude + 180) / 6) + 1;
    }
    
    /**
     * 是否在北半球
     */
    public boolean isNorthern(double latitude) {
        return latitude >= 0;
    }
    
    // ==================== 七参数转换 ====================
    
    /**
     * 七参数转换 (布尔沙模型)
     */
    public double[] transformWithSevenParams(double[] point, SevenParams params, boolean reverse) {
        if (reverse) {
            // 逆向转换
            double dx = -params.getDx();
            double dy = -params.getDy();
            double dz = -params.getDz();
            double rx = -params.getRx();
            double ry = -params.getRy();
            double rz = -params.getRz();
            double scale = -params.getScale();
            return applySevenParams(point, dx, dy, dz, rx, ry, rz, scale);
        }
        return applySevenParams(point, params.getDx(), params.getDy(), params.getDz(), 
                params.getRx(), params.getRy(), params.getRz(), params.getScale());
    }
    
    private double[] applySevenParams(double[] point, double dx, double dy, double dz,
            double rx, double ry, double rz, double scale) {
        double x = point[0];
        double y = point[1];
        double z = point[2];
        
        double newX = dx + (1 + scale) * (x + rz * y - ry * z);
        double newY = dy + (1 + scale) * (-rz * x + y + rx * z);
        double newZ = dz + (1 + scale) * (ry * x - rx * y + z);
        
        return new double[]{newX, newY, newZ};
    }
    
    // ==================== 高程转换 ====================
    
    /**
     * 大地坐标转笛卡尔坐标
     */
    public double[] geodeticToCartesian(double lon, double lat, double height, String epsg) {
        CoordinateSystem crs = crsRegistry.get(epsg);
        if (crs == null) {
            throw new CRSNotFoundException("CRS not found: " + epsg);
        }
        
        // 简化实现
        return new double[]{lon, lat, height};
    }
    
    /**
     * 笛卡尔坐标转大地坐标
     */
    public double[] cartesianToGeodetic(double x, double y, double z, String epsg) {
        CoordinateSystem crs = crsRegistry.get(epsg);
        if (crs == null) {
            throw new CRSNotFoundException("CRS not found: " + epsg);
        }
        
        // 简化实现
        return new double[]{x, y, z};
    }
    
    /**
     * 计算高程异常
     */
    public double calculateHeightAnomaly(double lat, double lon) {
        // 简化实现
        return 0;
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 计算两点间距离
     */
    public double calculateDistance(double lon1, double lat1, double lon2, double lat2) {
        final double R = 6371; // km
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * 计算方位角
     */
    public double calculateBearing(double lon1, double lat1, double lon2, double lat2) {
        double dLon = Math.toRadians(lon2 - lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        
        double y = Math.sin(dLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon);
        
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class CoordinateSystem {
        private String epsg;
        private String name;
        private String type; // geographic, projected
        private String unit;
        private double[] bounds;
        private String proj4;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SevenParams {
        private double dx; // 米
        private double dy;
        private double dz;
        private double rx; // 弧度
        private double ry;
        private double rz;
        private double scale; // ppm
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Transformation {
        private String id;
        private String fromCRS;
        private String toCRS;
        private String method; // 七参数, 四参数, etc
        private SevenParams params;
    }
    
    public static class CRSNotFoundException extends RuntimeException {
        public CRSNotFoundException(String msg) { super(msg); }
    }
}

package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 地理编码与路径规划服务
 */
@Slf4j
@Service
public class GeoService {
    
    private final CacheService cacheService;
    
    // 地理编码缓存
    private final Map<String, GeocodingResult> geocodeCache = new ConcurrentHashMap<>();
    
    // 路径规划缓存
    private final Map<String, RouteResult> routeCache = new ConcurrentHashMap<>();
    
    // 地址数据库（简化实现）
    private final Map<String, List<AddressEntry>> addressIndex = new ConcurrentHashMap<>();
    
    public GeoService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeSampleData();
    }
    
    // ==================== 初始化示例数据 ====================
    
    private void initializeSampleData() {
        // 中国主要城市坐标
        addAddress("北京", 116.4074, 39.9042, "北京市", "北京市", "100000");
        addAddress("天安门", 116.3975, 39.9085, "北京市东城区", "北京市", "100006");
        addAddress("故宫", 116.3972, 39.9163, "北京市东城区", "北京市", "100006");
        addAddress("上海", 121.4737, 31.2304, "上海市", "上海市", "200000");
        addAddress("外滩", 121.4903, 31.2401, "上海市黄浦区", "上海市", "200001");
        addAddress("东方明珠", 121.4998, 31.2397, "上海市浦东新区", "上海市", "200120");
        addAddress("广州", 113.2644, 23.1291, "广东省广州市", "广州市", "510000");
        addAddress("深圳", 114.0579, 22.5431, "广东省深圳市", "深圳市", "518000");
        addAddress("成都", 104.0657, 30.6598, "四川省成都市", "成都市", "610000");
        addAddress("杭州", 120.1551, 30.2741, "浙江省杭州市", "杭州市", "310000");
        addAddress("武汉", 114.3055, 30.5928, "湖北省武汉市", "武汉市", "430000");
        addAddress("西安", 108.9402, 34.3416, "陕西省西安市", "西安市", "710000");
        addAddress("南京", 118.7969, 32.0603, "江苏省南京市", "南京市", "210000");
        addAddress("重庆", 106.5516, 29.5630, "重庆市", "重庆市", "400000");
        addAddress("天津", 117.3616, 39.3434, "天津市", "天津市", "300000");
        addAddress("苏州", 120.5853, 31.2989, "江苏省苏州市", "苏州市", "215000");
        addAddress("青岛", 120.3826, 36.0671, "山东省青岛市", "青岛市", "266000");
        addAddress("长沙", 112.9388, 28.2282, "湖南省长沙市", "长沙市", "410000");
        addAddress("郑州", 113.6254, 34.7466, "河南省郑州市", "郑州市", "450000");
        addAddress("沈阳", 123.4315, 41.8057, "辽宁省沈阳市", "沈阳市", "110000");
    }
    
    private void addAddress(String name, double lon, double lat, String district, String city, String postcode) {
        AddressEntry entry = AddressEntry.builder()
                .name(name)
                .lon(lon)
                .lat(lat)
                .district(district)
                .city(city)
                .province(city.substring(0, Math.min(2, city.length())))
                .postcode(postcode)
                .fullAddress(district + name)
                .build();
        
        // 按城市索引
        addressIndex.computeIfAbsent(city, k -> new ArrayList<>()).add(entry);
        
        // 按名称首字母索引
        String pinyin = pinyin(name);
        if (pinyin != null && !pinyin.isEmpty()) {
            addressIndex.computeIfAbsent(pinyin, k -> new ArrayList<>()).add(entry);
        }
    }
    
    private String pinyin(String chinese) {
        // 简化实现，实际应使用拼音库
        return chinese;
    }
    
    // ==================== 地理编码 (地址 → 坐标) ====================
    
    /**
     * 地理编码
     */
    public GeocodingResult geocode(String address) {
        // 检查缓存
        if (geocodeCache.containsKey(address)) {
            return geocodeCache.get(address);
        }
        
        log.info("地理编码: address={}", address);
        
        // 精确匹配
        for (List<AddressEntry> entries : addressIndex.values()) {
            for (AddressEntry entry : entries) {
                if (entry.getFullAddress().contains(address) || entry.getName().equals(address)) {
                    GeocodingResult result = buildGeocodingResult(entry, "exact");
                    geocodeCache.put(address, result);
                    return result;
                }
            }
        }
        
        // 模糊匹配
        String searchKey = address.length() > 2 ? address.substring(0, 2) : address;
        for (Map.Entry<String, List<AddressEntry>> entry : addressIndex.entrySet()) {
            if (entry.getKey().contains(searchKey)) {
                List<AddressEntry> matches = entry.getValue();
                if (!matches.isEmpty()) {
                    AddressEntry best = matches.get(0);
                    GeocodingResult result = buildGeocodingResult(best, "fuzzy");
                    geocodeCache.put(address, result);
                    return result;
                }
            }
        }
        
        // 返回null表示未找到
        return null;
    }
    
    /**
     * 批量地理编码
     */
    public List<GeocodingResult> batchGeocode(List<String> addresses) {
        List<GeocodingResult> results = new ArrayList<>();
        for (String address : addresses) {
            GeocodingResult result = geocode(address);
            results.add(result != null ? result : GeocodingResult.builder()
                    .input(address)
                    .found(false)
                    .build());
        }
        return results;
    }
    
    /**
     * 搜索地址建议
     */
    public List<AddressSuggestion> suggest(String query, int limit) {
        List<AddressSuggestion> suggestions = new ArrayList<>();
        
        for (List<AddressEntry> entries : addressIndex.values()) {
            for (AddressEntry entry : entries) {
                if (entry.getName().contains(query) || 
                        entry.getFullAddress().contains(query) ||
                        entry.getCity().contains(query)) {
                    
                    suggestions.add(AddressSuggestion.builder()
                            .address(entry.getFullAddress())
                            .name(entry.getName())
                            .city(entry.getCity())
                            .district(entry.getDistrict())
                            .lon(entry.getLon())
                            .lat(entry.getLat())
                            .score(calculateMatchScore(query, entry))
                            .build());
                    
                    if (suggestions.size() >= limit) {
                        break;
                    }
                }
            }
            if (suggestions.size() >= limit) {
                break;
            }
        }
        
        // 按匹配度排序
        suggestions.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        return suggestions.subList(0, Math.min(suggestions.size(), limit));
    }
    
    private GeocodingResult buildGeocodingResult(AddressEntry entry, String matchType) {
        return GeocodingResult.builder()
                .input(entry.getName())
                .found(true)
                .matchType(matchType)
                .lon(entry.getLon())
                .lat(entry.getLat())
                .formattedAddress(entry.getFullAddress())
                .city(entry.getCity())
                .district(entry.getDistrict())
                .postcode(entry.getPostcode())
                .confidence(matchType.equals("exact") ? 0.95 : 0.75)
                .build();
    }
    
    private double calculateMatchScore(String query, AddressEntry entry) {
        double score = 0.0;
        if (entry.getName().startsWith(query)) score += 0.8;
        else if (entry.getName().contains(query)) score += 0.5;
        if (entry.getCity().contains(query)) score += 0.2;
        return score;
    }
    
    // ==================== 逆地理编码 (坐标 → 地址) ====================
    
    /**
     * 逆地理编码
     */
    public GeocodingResult reverseGeocode(double lon, double lat) {
        log.info("逆地理编码: lon={}, lat={}", lon, lat);
        
        // 查找最近的地址
        AddressEntry nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (List<AddressEntry> entries : addressIndex.values()) {
            for (AddressEntry entry : entries) {
                double dist = haversineDistance(lon, lat, entry.getLon(), entry.getLat());
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = entry;
                }
            }
        }
        
        if (nearest != null && minDistance < 100) { // 100公里内
            return GeocodingResult.builder()
                    .found(true)
                    .lon(nearest.getLon())
                    .lat(nearest.getLat())
                    .formattedAddress(nearest.getFullAddress())
                    .city(nearest.getCity())
                    .district(nearest.getDistrict())
                    .postcode(nearest.getPostcode())
                    .distance(minDistance)
                    .confidence(Math.max(0, 1.0 - minDistance / 100))
                    .build();
        }
        
        return GeocodingResult.builder()
                .found(false)
                .lon(lon)
                .lat(lat)
                .formattedAddress("未知地址")
                .confidence(0.0)
                .build();
    }
    
    // ==================== 路径规划 ====================
    
    /**
     * 路径规划
     */
    public RouteResult route(RouteRequest request) {
        String cacheKey = request.getCacheKey();
        
        if (routeCache.containsKey(cacheKey)) {
            return routeCache.get(cacheKey);
        }
        
        log.info("路径规划: from={}, to={}, mode={}", 
                request.getFrom().getName(), request.getTo().getName(), request.getMode());
        
        // 计算直线距离（简化实现）
        double directDistance = haversineDistance(
                request.getFrom().getLon(), request.getFrom().getLat(),
                request.getTo().getLon(), request.getTo().getLat());
        
        // 道路距离估算（简化：直线距离 × 1.3）
        double roadDistance = directDistance * 1.3;
        
        // 估算时间
        double speed = getAverageSpeed(request.getMode());
        double duration = roadDistance / speed; // 秒
        
        // 生成路线点
        List<RouteWaypoint> waypoints = generateRoutePoints(
                request.getFrom(), request.getTo(), request.getMode());
        
        RouteResult result = RouteResult.builder()
                .from(request.getFrom())
                .to(request.getTo())
                .mode(request.getMode())
                .distance(roadDistance)
                .directDistance(directDistance)
                .duration(duration)
                .waypoints(waypoints)
                .build();
        
        // 添加步骤
        result.setSteps(generateRouteSteps(result));
        
        // 缓存结果
        routeCache.put(cacheKey, result);
        
        return result;
    }
    
    /**
     * 批量路径规划（多点多段路线）
     */
    public RouteResult multiStopRoute(List<RoutePoint> stops, String mode) {
        if (stops == null || stops.size() < 2) {
            throw new IllegalArgumentException("至少需要2个途经点");
        }
        
        RouteResult totalResult = RouteResult.builder()
                .from(stops.get(0))
                .to(stops.get(stops.size() - 1))
                .mode(mode)
                .waypoints(new ArrayList<>())
                .steps(new ArrayList<>())
                .build();
        
        double totalDistance = 0;
        double totalDuration = 0;
        
        for (int i = 0; i < stops.size() - 1; i++) {
            RouteRequest segment = RouteRequest.builder()
                    .from(stops.get(i))
                    .to(stops.get(i + 1))
                    .mode(mode)
                    .build();
            
            RouteResult segmentResult = route(segment);
            
            totalDistance += segmentResult.getDistance();
            totalDuration += segmentResult.getDuration();
            totalResult.getWaypoints().addAll(segmentResult.getWaypoints());
            totalResult.getSteps().addAll(segmentResult.getSteps());
        }
        
        totalResult.setDistance(totalDistance);
        totalResult.setDuration(totalDuration);
        
        return totalResult;
    }
    
    /**
     * 服务区分析
     */
    public ServiceAreaResult serviceArea(RoutePoint center, double radius, String mode, int intervals) {
        log.info("服务区分析: center={}, radius={}, mode={}", 
                center.getName(), radius, mode);
        
        List<ServiceAreaRing> rings = new ArrayList<>();
        
        for (int i = 1; i <= intervals; i++) {
            double ringRadius = radius * i / intervals;
            rings.add(ServiceAreaRing.builder()
                    .radius(ringRadius)
                    .estimatedDistance(ringRadius * 1.3) // 简化
                    .estimatedDuration(ringRadius * 1.3 / getAverageSpeed(mode))
                    .polygon(generateCirclePolygon(center.getLon(), center.getLat(), ringRadius))
                    .build());
        }
        
        return ServiceAreaResult.builder()
                .center(center)
                .radius(radius)
                .mode(mode)
                .rings(rings)
                .build();
    }
    
    // ==================== 辅助方法 ====================
    
    private double haversineDistance(double lon1, double lat1, double lon2, double lat2) {
        final double R = 6371; // 地球半径(公里)
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    private double getAverageSpeed(String mode) {
        // km/h
        return switch (mode.toLowerCase()) {
            case "driving" -> 40.0; // 城市道路平均
            case "walking" -> 5.0;
            case "cycling" -> 15.0;
            default -> 40.0;
        };
    }
    
    private List<RouteWaypoint> generateRoutePoints(RoutePoint from, RoutePoint to, String mode) {
        List<RouteWaypoint> points = new ArrayList<>();
        
        // 起点
        points.add(RouteWaypoint.builder()
                .lon(from.getLon())
                .lat(from.getLat())
                .type("start")
                .instruction("起点: " + from.getName())
                .build());
        
        // 中间点（简化：直线）
        int segments = 10;
        for (int i = 1; i < segments; i++) {
            double ratio = (double) i / segments;
            double lon = from.getLon() + (to.getLon() - from.getLon()) * ratio;
            double lat = from.getLat() + (to.getLat() - from.getLat()) * ratio;
            
            points.add(RouteWaypoint.builder()
                    .lon(lon)
                    .lat(lat)
                    .type("waypoint")
                    .instruction(null)
                    .build());
        }
        
        // 终点
        points.add(RouteWaypoint.builder()
                .lon(to.getLon())
                .lat(to.getLat())
                .type("end")
                .instruction("终点: " + to.getName())
                .build());
        
        return points;
    }
    
    private List<RouteStep> generateRouteSteps(RouteResult route) {
        List<RouteStep> steps = new ArrayList<>();
        
        steps.add(RouteStep.builder()
                .instruction("从 " + route.getFrom().getName() + " 出发")
                .distance(0)
                .duration(0)
                .type("depart")
                .build());
        
        steps.add(RouteStep.builder()
                .instruction("沿道路向" + getDirection(route.getFrom(), route.getTo()) + "行驶")
                .distance(route.getDistance() * 0.5)
                .duration(route.getDuration() * 0.5)
                .type("continue")
                .build());
        
        steps.add(RouteStep.builder()
                .instruction("到达 " + route.getTo().getName())
                .distance(route.getDistance())
                .duration(route.getDuration())
                .type("arrive")
                .build());
        
        return steps;
    }
    
    private String getDirection(RoutePoint from, RoutePoint to) {
        double dx = to.getLon() - from.getLon();
        double dy = to.getLat() - from.getLat();
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        
        if (angle >= -22.5 && angle < 22.5) return "东";
        if (angle >= 22.5 && angle < 67.5) return "东北";
        if (angle >= 67.5 && angle < 112.5) return "北";
        if (angle >= 112.5 && angle < 157.5) return "西北";
        if (angle >= 157.5 || angle < -157.5) return "西";
        if (angle >= -157.5 && angle < -112.5) return "西南";
        if (angle >= -112.5 && angle < -67.5) return "南";
        if (angle >= -67.5 && angle < -22.5) return "东南";
        
        return "未知";
    }
    
    private Object generateCirclePolygon(double centerLon, double centerLat, double radius) {
        // 简化的圆形多边形
        List<double[]> coordinates = new ArrayList<>();
        int points = 36;
        
        for (int i = 0; i <= points; i++) {
            double angle = 2 * Math.PI * i / points;
            double lon = centerLon + (radius / 111.0) * Math.cos(angle);
            double lat = centerLat + (radius / 111.0) * Math.sin(angle);
            coordinates.add(new double[]{lon, lat});
        }
        
        return Map.of("type", "Polygon", "coordinates", List.of(coordinates));
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class AddressEntry {
        private String name;
        private double lon;
        private double lat;
        private String province;
        private String city;
        private String district;
        private String postcode;
        private String fullAddress;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class GeocodingResult {
        private String input;
        private boolean found;
        private String matchType;
        private double lon;
        private double lat;
        private String formattedAddress;
        private String city;
        private String district;
        private String postcode;
        private double confidence;
        private Double distance;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AddressSuggestion {
        private String address;
        private String name;
        private String city;
        private String district;
        private double lon;
        private double lat;
        private double score;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RouteRequest {
        private RoutePoint from;
        private RoutePoint to;
        private String mode; // driving, walking, cycling
        
        public String getCacheKey() {
            return String.format("route:%f,%f->%f,%f:%s", 
                    from.getLon(), from.getLat(), to.getLon(), to.getLat(), mode);
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RoutePoint {
        private String name;
        private double lon;
        private double lat;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RouteResult {
        private RoutePoint from;
        private RoutePoint to;
        private String mode;
        private double distance; // km
        private double directDistance;
        private double duration; // seconds
        private List<RouteWaypoint> waypoints;
        private List<RouteStep> steps;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RouteWaypoint {
        private double lon;
        private double lat;
        private String type; // start, waypoint, end
        private String instruction;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RouteStep {
        private String instruction;
        private double distance;
        private double duration;
        private String type;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ServiceAreaResult {
        private RoutePoint center;
        private double radius;
        private String mode;
        private List<ServiceAreaRing> rings;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ServiceAreaRing {
        private double radius;
        private double estimatedDistance;
        private double estimatedDuration;
        private Object polygon;
    }
}

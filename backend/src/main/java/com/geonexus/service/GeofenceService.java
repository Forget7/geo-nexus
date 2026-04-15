package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 地理围栏服务 - 区域监控与告警
 */
@Slf4j
@Service
public class GeofenceService {
    
    private final CacheService cacheService;
    
    // 围栏定义
    private final Map<String, Geofence> geofences = new ConcurrentHashMap<>();
    
    // 监控目标
    private final Map<String, MonitoredTarget> targets = new ConcurrentHashMap<>();
    
    // 事件历史
    private final List<GeofenceEvent> eventHistory = new CopyOnWriteArrayList<>();
    
    public GeofenceService(CacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    // ==================== 围栏管理 ====================
    
    /**
     * 创建围栏
     */
    public Geofence createGeofence(Geofence fence) {
        fence.setId(UUID.randomUUID().toString());
        fence.setCreatedAt(System.currentTimeMillis());
        fence.setStatus("active");
        
        geofences.put(fence.getId(), fence);
        
        log.info("创建地理围栏: id={}, name={}, type={}", 
                fence.getId(), fence.getName(), fence.getGeometry().getType());
        
        return fence;
    }
    
    /**
     * 获取围栏
     */
    public Geofence getGeofence(String fenceId) {
        return geofences.get(fenceId);
    }
    
    /**
     * 更新围栏
     */
    public Geofence updateGeofence(String fenceId, Geofence updates) {
        Geofence existing = geofences.get(fenceId);
        if (existing == null) {
            throw new GeofenceNotFoundException("围栏不存在: " + fenceId);
        }
        
        updates.setId(fenceId);
        updates.setCreatedAt(existing.getCreatedAt());
        updates.setUpdatedAt(System.currentTimeMillis());
        
        geofences.put(fenceId, updates);
        
        return updates;
    }
    
    /**
     * 列出所有围栏
     */
    public List<Geofence> getGeofences() {
        return new ArrayList<>(geofences.values());
    }

    /**
     * 检查点是否在围栏内（公开方法）
     */
    public boolean isPointInside(String fenceId, double lon, double lat) {
        Geofence fence = geofences.get(fenceId);
        if (fence == null) return false;
        return isInside(new double[]{lon, lat}, fence);
    }

    /**
     * 删除围栏
     */
    public void deleteGeofence(String fenceId) {
        geofences.remove(fenceId);
        
        // 清理相关监控
        targets.values().forEach(t -> t.getMonitoredFences().remove(fenceId));
        
        log.info("删除地理围栏: id={}", fenceId);
    }
    
    /**
     * 启用/禁用围栏
     */
    public void setGeofenceStatus(String fenceId, boolean enabled) {
        Geofence fence = geofences.get(fenceId);
        if (fence != null) {
            fence.setStatus(enabled ? "active" : "disabled");
        }
    }
    
    // ==================== 监控目标管理 ====================
    
    /**
     * 添加监控目标
     */
    public MonitoredTarget addTarget(MonitoredTarget target) {
        target.setId(UUID.randomUUID().toString());
        target.setCreatedAt(System.currentTimeMillis());
        target.setLastUpdate(System.currentTimeMillis());
        
        targets.put(target.getId(), target);
        
        log.info("添加监控目标: id={}, name={}", target.getId(), target.getName());
        
        return target;
    }
    
    /**
     * 更新目标位置
     */
    public void updateTargetPosition(String targetId, double lon, double lat, double speed, double bearing) {
        MonitoredTarget target = targets.get(targetId);
        if (target == null) return;
        
        double[] oldPosition = target.getPosition();
        
        // 更新位置
        target.setPosition(new double[]{lon, lat});
        target.setSpeed(speed);
        target.setBearing(bearing);
        target.setLastUpdate(System.currentTimeMillis());
        
        // 检查围栏
        checkGeofenceEvents(target, oldPosition);
    }
    
    /**
     * 批量更新位置
     */
    public void batchUpdatePositions(List<TargetPosition> positions) {
        for (TargetPosition pos : positions) {
            updateTargetPosition(pos.getTargetId(), pos.getLon(), pos.getLat(), 
                    pos.getSpeed(), pos.getBearing());
        }
    }
    
    // ==================== 围栏检测 ====================
    
    private void checkGeofenceEvents(MonitoredTarget target, double[] oldPosition) {
        double[] newPosition = target.getPosition();
        
        for (Geofence fence : geofences.values()) {
            if (!"active".equals(fence.getStatus())) continue;
            
            boolean wasInside = isInside(oldPosition, fence);
            boolean isInsideNow = isInside(newPosition, fence);
            
            // 进入事件
            if (!wasInside && isInsideNow) {
                GeofenceEvent event = createEvent(target, fence, "enter");
                eventHistory.add(event);
                log.info("目标进入围栏: target={}, fence={}", target.getName(), fence.getName());
            }
            
            // 离开事件
            if (wasInside && !isInsideNow) {
                GeofenceEvent event = createEvent(target, fence, "exit");
                eventHistory.add(event);
                log.info("目标离开围栏: target={}, fence={}", target.getName(), fence.getName());
            }
            
            // 停留超时事件
            if (isInsideNow && fence.getDwellTime() != null) {
                Long lastEntry = target.getLastEntryTime().get(fence.getId());
                if (lastEntry == null) {
                    target.getLastEntryTime().put(fence.getId(), System.currentTimeMillis());
                } else if (System.currentTimeMillis() - lastEntry > fence.getDwellTime()) {
                    GeofenceEvent event = createEvent(target, fence, "dwell");
                    eventHistory.add(event);
                    log.info("目标停留超时: target={}, fence={}", target.getName(), fence.getName());
                }
            }
        }
    }
    
    private GeofenceEvent createEvent(MonitoredTarget target, Geofence fence, String type) {
        return GeofenceEvent.builder()
                .id(UUID.randomUUID().toString())
                .targetId(target.getId())
                .targetName(target.getName())
                .fenceId(fence.getId())
                .fenceName(fence.getName())
                .eventType(type)
                .position(target.getPosition())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    private boolean isInside(double[] position, Geofence fence) {
        Geometry geometry = fence.getGeometry();
        
        switch (geometry.getType()) {
            case "Circle":
                Circle circle = geometry.getCircle();
                double dist = haversineDistance(position[0], position[1], 
                        circle.getCenter()[0], circle.getCenter()[1]);
                return dist <= circle.getRadius();
                
            case "Polygon":
                return pointInPolygon(position, geometry.getCoordinates());
                
            case "Rectangle":
                double[] bbox = geometry.getBbox();
                return position[0] >= bbox[0] && position[0] <= bbox[2] &&
                        position[1] >= bbox[1] && position[1] <= bbox[3];
                
            default:
                return false;
        }
    }
    
    private boolean pointInPolygon(double[] point, List<double[]> polygon) {
        int n = polygon.size();
        boolean inside = false;
        
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon.get(i)[0], yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0], yj = polygon.get(j)[1];
            
            if ((yi > point[1]) != (yj > point[1]) &&
                    point[0] < (xj - xi) * (point[1] - yi) / (yj - yi) + xi) {
                inside = !inside;
            }
        }
        
        return inside;
    }
    
    private double haversineDistance(double lon1, double lat1, double lon2, double lat2) {
        final double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    // ==================== 查询 ====================
    
    /**
     * 查询目标状态
     */
    public List<MonitoredTarget> getTargetStatus(String fenceId) {
        return targets.values().stream()
                .filter(t -> t.getMonitoredFences().contains(fenceId))
                .toList();
    }
    
    /**
     * 获取围栏内目标
     */
    public List<MonitoredTarget> getTargetsInside(String fenceId) {
        Geofence fence = geofences.get(fenceId);
        if (fence == null) return Collections.emptyList();
        
        return targets.values().stream()
                .filter(t -> isInside(t.getPosition(), fence))
                .toList();
    }
    
    /**
     * 获取事件历史
     */
    public List<GeofenceEvent> getEventHistory(String targetId, String fenceId, 
            Long startTime, Long endTime, int limit) {
        return eventHistory.stream()
                .filter(e -> {
                    if (targetId != null && !targetId.equals(e.getTargetId())) return false;
                    if (fenceId != null && !fenceId.equals(e.getFenceId())) return false;
                    if (startTime != null && e.getTimestamp() < startTime) return false;
                    if (endTime != null && e.getTimestamp() > endTime) return false;
                    return true;
                })
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .limit(limit > 0 ? limit : 100)
                .toList();
    }
    
    /**
     * 获取统计信息
     */
    public GeofenceStatistics getStatistics(String fenceId, Long startTime, Long endTime) {
        List<GeofenceEvent> events = getEventHistory(null, fenceId, startTime, endTime, 0);
        
        long enterCount = events.stream().filter(e -> "enter".equals(e.getEventType())).count();
        long exitCount = events.stream().filter(e -> "exit".equals(e.getEventType())).count();
        long dwellCount = events.stream().filter(e -> "dwell".equals(e.getEventType())).count();
        
        Set<String> uniqueTargets = new HashSet<>();
        events.forEach(e -> uniqueTargets.add(e.getTargetId()));
        
        return GeofenceStatistics.builder()
                .fenceId(fenceId)
                .totalEvents(events.size())
                .enterCount(enterCount)
                .exitCount(exitCount)
                .dwellCount(dwellCount)
                .uniqueTargets(uniqueTargets.size())
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class Geofence {
        private String id;
        private String name;
        private String description;
        private String category;
        private Geometry geometry;
        private String status;
        private Map<String, Object> metadata;
        private Long dwellTime; // 毫秒
        private boolean alertsEnabled;
        private Long createdAt;
        private Long updatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Geometry {
        private String type; // Circle, Polygon, Rectangle
        private double[] center; // for Circle
        private double radius; // for Circle (km)
        private List<double[]> coordinates; // for Polygon
        private double[] bbox; // for Rectangle [minX, minY, maxX, maxY]
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Circle {
        private double[] center;
        private double radius;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MonitoredTarget {
        private String id;
        private String name;
        private String type; // vehicle, person, asset
        private double[] position;
        private double speed;
        private double bearing;
        private Map<String, Object> metadata;
        private Set<String> monitoredFences;
        private Map<String, Long> lastEntryTime;
        private Long createdAt;
        private Long lastUpdate;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class GeofenceEvent {
        private String id;
        private String targetId;
        private String targetName;
        private String fenceId;
        private String fenceName;
        private String eventType; // enter, exit, dwell
        private double[] position;
        private Long timestamp;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class GeofenceStatistics {
        private String fenceId;
        private long totalEvents;
        private long enterCount;
        private long exitCount;
        private long dwellCount;
        private long uniqueTargets;
        private Long startTime;
        private Long endTime;
    }
    
    @lombok.Data
    public static class TargetPosition {
        private String targetId;
        private double lon;
        private double lat;
        private double speed;
        private double bearing;
    }
    
    public static class GeofenceNotFoundException extends RuntimeException {
        public GeofenceNotFoundException(String msg) { super(msg); }
    }
}

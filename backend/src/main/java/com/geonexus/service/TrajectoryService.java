package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 轨迹服务 - 轨迹管理与可视化
 */
@Slf4j
@Service
public class TrajectoryService {
    
    private final CacheService cacheService;
    
    // 轨迹存储
    private final Map<String, Trajectory> trajectories = new ConcurrentHashMap<>();
    
    // 轨迹点
    private final Map<String, List<TrajectoryPoint>> trajectoryPoints = new ConcurrentHashMap<>();
    
    public TrajectoryService(CacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    // ==================== 轨迹管理 ====================
    
    /**
     * 创建轨迹
     */
    public Trajectory createTrajectory(Trajectory trajectory) {
        trajectory.setId(UUID.randomUUID().toString());
        trajectory.setCreatedAt(System.currentTimeMillis());
        trajectory.setStatus("active");
        
        trajectories.put(trajectory.getId(), trajectory);
        trajectoryPoints.put(trajectory.getId(), new ArrayList<>());
        
        log.info("创建轨迹: id={}, name={}, objectId={}", 
                trajectory.getId(), trajectory.getName(), trajectory.getObjectId());
        
        return trajectory;
    }
    
    /**
     * 获取轨迹
     */
    public Trajectory getTrajectory(String trajectoryId) {
        return trajectories.get(trajectoryId);
    }
    
    /**
     * 更新轨迹
     */
    public Trajectory updateTrajectory(String trajectoryId, Trajectory updates) {
        Trajectory existing = trajectories.get(trajectoryId);
        if (existing == null) {
            throw new TrajectoryNotFoundException("轨迹不存在: " + trajectoryId);
        }
        
        updates.setId(trajectoryId);
        updates.setCreatedAt(existing.getCreatedAt());
        updates.setUpdatedAt(System.currentTimeMillis());
        
        trajectories.put(trajectoryId, updates);
        
        return updates;
    }
    
    /**
     * 删除轨迹
     */
    public void deleteTrajectory(String trajectoryId) {
        trajectories.remove(trajectoryId);
        trajectoryPoints.remove(trajectoryId);
        
        log.info("删除轨迹: id={}", trajectoryId);
    }
    
    /**
     * 列出轨迹（支持过滤）
     */
    public List<Trajectory> getTrajectories(String objectId, String startTime, String endTime, int limit) {
        return trajectories.values().stream()
                .filter(t -> objectId == null || objectId.equals(t.getObjectId()))
                .filter(t -> {
                    if (startTime == null) return true;
                    Long st = parseTime(startTime);
                    return t.getCreatedAt() != null && t.getCreatedAt() >= st;
                })
                .filter(t -> {
                    if (endTime == null) return true;
                    Long et = parseTime(endTime);
                    return t.getCreatedAt() != null && t.getCreatedAt() <= et;
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取轨迹点
     */
    public List<TrajectoryPoint> getTrajectoryPoints(String trajectoryId) {
        return getPoints(trajectoryId, null, null);
    }
    
    /**
     * 获取轨迹统计
     */
    public Map<String, Object> getTrajectoryStats(String trajectoryId) {
        TrajectoryStatistics stats = calculateStatistics(trajectoryId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trajectoryId", stats.getTrajectoryId());
        result.put("pointCount", stats.getPointCount());
        result.put("totalDistance", stats.getTotalDistance());
        result.put("duration", stats.getDuration());
        result.put("maxSpeed", stats.getMaxSpeed());
        result.put("avgSpeed", stats.getAvgSpeed());
        result.put("startTime", stats.getStartTime());
        result.put("endTime", stats.getEndTime());
        result.put("bounds", stats.getBounds());
        return result;
    }
    
    /**
     * 空间过滤轨迹
     */
    public List<Trajectory> filterBySpatial(List<Double> bbox, String startTime, String endTime) {
        if (bbox == null || bbox.size() < 4) {
            return Collections.emptyList();
        }
        double minLon = bbox.get(0);
        double minLat = bbox.get(1);
        double maxLon = bbox.get(2);
        double maxLat = bbox.get(3);
        
        return trajectories.values().stream()
                .filter(t -> t.getBounds() != null)
                .filter(t -> {
                    double[] b = t.getBounds();
                    return !(b[0] > maxLon || b[2] < minLon || b[1] > maxLat || b[3] < minLat);
                })
                .filter(t -> {
                    if (startTime == null) return true;
                    Long st = parseTime(startTime);
                    return t.getCreatedAt() != null && t.getCreatedAt() >= st;
                })
                .filter(t -> {
                    if (endTime == null) return true;
                    Long et = parseTime(endTime);
                    return t.getCreatedAt() != null && t.getCreatedAt() <= et;
                })
                .collect(Collectors.toList());
    }
    
    private Long parseTime(String time) {
        if (time == null || time.isEmpty()) return null;
        try {
            return Long.parseLong(time);
        } catch (NumberFormatException e) {
            try {
                return java.time.Instant.parse(time).toEpochMilli();
            } catch (Exception ex) {
                return null;
            }
        }
    }
    
    // ==================== 轨迹点操作 ====================
    
    /**
     * 添加轨迹点
     */
    public TrajectoryPoint addPoint(String trajectoryId, TrajectoryPoint point) {
        Trajectory trajectory = getTrajectory(trajectoryId);
        if (trajectory == null) {
            throw new TrajectoryNotFoundException("轨迹不存在: " + trajectoryId);
        }
        
        point.setId(UUID.randomUUID().toString());
        point.setTrajectoryId(trajectoryId);
        point.setTimestamp(System.currentTimeMillis());
        
        List<TrajectoryPoint> points = trajectoryPoints.computeIfAbsent(
                trajectoryId, k -> new ArrayList<>());
        points.add(point);
        
        // 更新轨迹统计
        trajectory.setPointCount(points.size());
        trajectory.setUpdatedAt(System.currentTimeMillis());
        
        // 更新边界
        updateBounds(trajectory, point);
        
        return point;
    }
    
    /**
     * 批量添加点
     */
    public void addPoints(String trajectoryId, List<TrajectoryPoint> newPoints) {
        for (TrajectoryPoint point : newPoints) {
            addPoint(trajectoryId, point);
        }
    }
    
    /**
     * 获取轨迹点
     */
    public List<TrajectoryPoint> getPoints(String trajectoryId, Long startTime, Long endTime) {
        List<TrajectoryPoint> points = trajectoryPoints.get(trajectoryId);
        if (points == null) return Collections.emptyList();
        
        return points.stream()
                .filter(p -> {
                    if (startTime != null && p.getTimestamp() < startTime) return false;
                    if (endTime != null && p.getTimestamp() > endTime) return false;
                    return true;
                })
                .sorted(Comparator.comparingLong(TrajectoryPoint::getTimestamp))
                .collect(Collectors.toList());
    }
    
    // ==================== 轨迹分析 ====================
    
    /**
     * 计算轨迹统计
     */
    public TrajectoryStatistics calculateStatistics(String trajectoryId) {
        List<TrajectoryPoint> points = getPoints(trajectoryId, null, null);
        if (points == null || points.isEmpty()) {
            return new TrajectoryStatistics();
        }
        
        TrajectoryStatistics stats = new TrajectoryStatistics();
        stats.setTrajectoryId(trajectoryId);
        stats.setPointCount(points.size());
        
        // 计算距离
        double totalDistance = 0;
        double maxSpeed = 0;
        double totalSpeed = 0;
        
        for (int i = 1; i < points.size(); i++) {
            TrajectoryPoint prev = points.get(i - 1);
            TrajectoryPoint curr = points.get(i);
            
            double dist = haversineDistance(
                    prev.getLon(), prev.getLat(),
                    curr.getLon(), curr.getLat());
            totalDistance += dist;
            
            // 速度
            if (curr.getSpeed() != null) {
                maxSpeed = Math.max(maxSpeed, curr.getSpeed());
                totalSpeed += curr.getSpeed();
            }
            
            // 时间差
            long timeDiff = curr.getTimestamp() - prev.getTimestamp();
            stats.setDuration(stats.getDuration() + timeDiff);
        }
        
        stats.setTotalDistance(totalDistance);
        stats.setMaxSpeed(maxSpeed);
        stats.setAvgSpeed(points.stream()
                .filter(p -> p.getSpeed() != null)
                .mapToDouble(TrajectoryPoint::getSpeed)
                .average()
                .orElse(0));
        
        // 时间范围
        stats.setStartTime(points.get(0).getTimestamp());
        stats.setEndTime(points.get(points.size() - 1).getTimestamp());
        
        // 边界
        Trajectory trajectory = getTrajectory(trajectoryId);
        if (trajectory != null && trajectory.getBounds() != null) {
            stats.setBounds(trajectory.getBounds());
        }
        
        return stats;
    }
    
    /**
     * 轨迹分段
     */
    public List<TrajectorySegment> segmentTrajectory(String trajectoryId, long segmentDuration) {
        List<TrajectoryPoint> points = getPoints(trajectoryId, null, null);
        if (points == null || points.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<TrajectorySegment> segments = new ArrayList<>();
        TrajectorySegment currentSegment = null;
        
        for (TrajectoryPoint point : points) {
            if (currentSegment == null || 
                    point.getTimestamp() - currentSegment.getStartTime() > segmentDuration) {
                
                if (currentSegment != null) {
                    segments.add(currentSegment);
                }
                
                currentSegment = new TrajectorySegment();
                currentSegment.setId(UUID.randomUUID().toString());
                currentSegment.setTrajectoryId(trajectoryId);
                currentSegment.setStartTime(point.getTimestamp());
                currentSegment.setPoints(new ArrayList<>());
            }
            
            currentSegment.getPoints().add(point);
            currentSegment.setEndTime(point.getTimestamp());
        }
        
        if (currentSegment != null) {
            segments.add(currentSegment);
        }
        
        return segments;
    }
    
    /**
     * 停留点检测
     */
    public List<StayPoint> detectStayPoints(String trajectoryId, double minDistance, long minDuration) {
        List<TrajectoryPoint> points = getPoints(trajectoryId, null, null);
        if (points == null || points.size() < 2) {
            return Collections.emptyList();
        }
        
        List<StayPoint> stayPoints = new ArrayList<>();
        
        int i = 0;
        while (i < points.size()) {
            int j = i + 1;
            
            // 找到一群近距离点
            while (j < points.size()) {
                double dist = haversineDistance(
                        points.get(i).getLon(), points.get(i).getLat(),
                        points.get(j).getLon(), points.get(j).getLat());
                
                if (dist > minDistance) break;
                j++;
            }
            
            // 检查停留时间
            long duration = points.get(j - 1).getTimestamp() - points.get(i).getTimestamp();
            
            if (duration >= minDuration) {
                // 计算中心点
                double centerLat = 0, centerLon = 0;
                for (int k = i; k < j; k++) {
                    centerLat += points.get(k).getLat();
                    centerLon += points.get(k).getLon();
                }
                centerLat /= (j - i);
                centerLon /= (j - i);
                
                StayPoint stayPoint = new StayPoint();
                stayPoint.setId(UUID.randomUUID().toString());
                stayPoint.setTrajectoryId(trajectoryId);
                stayPoint.setStartTime(points.get(i).getTimestamp());
                stayPoint.setEndTime(points.get(j - 1).getTimestamp());
                stayPoint.setDuration(duration);
                stayPoint.setCenterLat(centerLat);
                stayPoint.setCenterLon(centerLon);
                stayPoint.setPointCount(j - i);
                
                stayPoints.add(stayPoint);
            }
            
            i = j;
        }
        
        return stayPoints;
    }
    
    /**
     * 轨迹匹配
     */
    public TrajectoryMatch matchToRoad(String trajectoryId, String roadNetwork) {
        List<TrajectoryPoint> points = getPoints(trajectoryId, null, null);
        
        TrajectoryMatch match = new TrajectoryMatch();
        match.setTrajectoryId(trajectoryId);
        match.setMatchedPoints(new ArrayList<>());
        match.setMatchingRate(0.85); // 简化
        match.setRoadNetwork(roadNetwork);
        
        // 简化：每个点都匹配
        for (TrajectoryPoint point : points) {
            MatchedPoint mp = new MatchedPoint();
            mp.setOriginalPoint(point);
            // matchedLon/lat应该通过地图匹配算法计算
            mp.setMatchedLon(point.getLon());
            mp.setMatchedLat(point.getLat());
            match.getMatchedPoints().add(mp);
        }
        
        return match;
    }
    
    // ==================== 可视化数据 ====================
    
    /**
     * 获取可视化数据
     */
    public TrajectoryVisualizationData getVisualizationData(String trajectoryId) {
        Trajectory trajectory = getTrajectory(trajectoryId);
        List<TrajectoryPoint> points = getPoints(trajectoryId, null, null);
        
        TrajectoryVisualizationData data = new TrajectoryVisualizationData();
        data.setTrajectory(trajectory);
        data.setPoints(points);
        
        // 计算统计数据
        if (points.size() >= 2) {
            List<double[]> coordinates = new ArrayList<>();
            double[] times = new double[points.size()];
            
            for (int i = 0; i < points.size(); i++) {
                coordinates.add(new double[]{points.get(i).getLon(), points.get(i).getLat()});
                times[i] = points.get(i).getTimestamp();
            }
            
            data.setCoordinates(coordinates);
            data.setTimes(times);
        }
        
        return data;
    }
    
    // ==================== 辅助方法 ====================
    
    private void updateBounds(Trajectory trajectory, TrajectoryPoint point) {
        double[] bounds = trajectory.getBounds();
        
        if (bounds == null) {
            bounds = new double[]{
                    point.getLon(), point.getLat(),
                    point.getLon(), point.getLat()
            };
        } else {
            bounds[0] = Math.min(bounds[0], point.getLon());
            bounds[1] = Math.min(bounds[1], point.getLat());
            bounds[2] = Math.max(bounds[2], point.getLon());
            bounds[3] = Math.max(bounds[3], point.getLat());
        }
        
        trajectory.setBounds(bounds);
    }
    
    private double haversineDistance(double lon1, double lat1, double lon2, double lat2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class Trajectory {
        private String id;
        private String name;
        private String description;
        private String objectId;
        private String objectType;
        private double[] bounds;
        private int pointCount;
        private String status;
        private Map<String, Object> metadata;
        private Long createdAt;
        private Long updatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class TrajectoryPoint {
        private String id;
        private String trajectoryId;
        private double lon;
        private double lat;
        private Double elevation;
        private Double speed;
        private Double bearing;
        private Long timestamp;
        private Map<String, Object> properties;
    }
    
    @lombok.Data
    public static class TrajectoryStatistics {
        private String trajectoryId;
        private int pointCount;
        private double totalDistance;
        private long duration;
        private double maxSpeed;
        private double avgSpeed;
        private Long startTime;
        private Long endTime;
        private double[] bounds;
    }
    
    @lombok.Data
    public static class TrajectorySegment {
        private String id;
        private String trajectoryId;
        private long startTime;
        private long endTime;
        private List<TrajectoryPoint> points;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class StayPoint {
        private String id;
        private String trajectoryId;
        private long startTime;
        private long endTime;
        private long duration;
        private double centerLat;
        private double centerLon;
        private int pointCount;
    }
    
    @lombok.Data
    public static class TrajectoryMatch {
        private String trajectoryId;
        private String roadNetwork;
        private List<MatchedPoint> matchedPoints;
        private double matchingRate;
    }
    
    @lombok.Data
    public static class MatchedPoint {
        private TrajectoryPoint originalPoint;
        private double matchedLon;
        private double matchedLat;
    }
    
    @lombok.Data
    public static class TrajectoryVisualizationData {
        private Trajectory trajectory;
        private List<TrajectoryPoint> points;
        private List<double[]> coordinates;
        private double[] times;
    }
    
    public static class TrajectoryNotFoundException extends RuntimeException {
        public TrajectoryNotFoundException(String msg) { super(msg); }
    }
}

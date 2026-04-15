package com.geonexus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 地形剖面图服务
 * 沿给定线路采样高程，返回剖面数据
 */
@Service
public class ElevationProfileService {

    private static final Logger log = LoggerFactory.getLogger(ElevationProfileService.class);

    /**
     * 沿给定线路采样高程，返回高程点列表。
     * 使用 USGS Elevation Point Query Service (或本地 terrain tile 采样)。
     * 这里用外部 HTTP 采样演示，实际可接入 Cesium ion terrain 或本地 DEM。
     */
    public ElevationProfileResult sampleElevation(List<List<Double>> lineCoordinates) {
        List<ElevationPoint> points = new ArrayList<>();
        double totalAscent = 0;
        double totalDescent = 0;
        double minElevation = Double.MAX_VALUE;
        double maxElevation = Double.MIN_VALUE;
        double distance = 0;

        for (int i = 0; i < lineCoordinates.size(); i++) {
            double lng = lineCoordinates.get(i).get(0);
            double lat = lineCoordinates.get(i).get(1);
            double elevation = sampleElevationPoint(lat, lng);
            double distFromPrev = 0;
            if (i > 0) {
                distFromPrev = haversineDistance(
                    lineCoordinates.get(i-1).get(1), lineCoordinates.get(i-1).get(0),
                    lat, lng
                );
                double elevDiff = elevation - points.get(i-1).getElevation();
                if (elevDiff > 0) totalAscent += elevDiff;
                else totalDescent += Math.abs(elevDiff);
            }
            distance += distFromPrev;
            minElevation = Math.min(minElevation, elevation);
            maxElevation = Math.max(maxElevation, elevation);
            points.add(new ElevationPoint(lat, lng, elevation, distance));
        }

        double totalElevationGain = totalAscent;
        double totalElevationLoss = totalDescent;
        double averageSlope = distance > 0 ? (totalAscent / distance) * 100 : 0;

        return new ElevationProfileResult(points, totalElevationGain, totalElevationLoss,
            minElevation, maxElevation, distance, averageSlope);
    }

    /**
     * 单点高程采样（演示用，随机地形模拟）。
     * 实际应接入 USGS Elevation API 或 Cesium ion terrain tiles。
     */
    private double sampleElevationPoint(double lat, double lng) {
        // 模拟地形：基于坐标生成伪高程（实际接入真实 API）
        double base = 500;
        double variation = Math.sin(lat * 50) * Math.cos(lng * 50) * 200
                        + Math.sin(lng * 30) * 100;
        return Math.max(0, base + variation + new Random().nextDouble() * 10);
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*
                   Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    public record ElevationPoint(double lat, double lng, double elevation, double distance) {}
    public record ElevationProfileResult(
        List<ElevationPoint> points,
        double totalAscent,
        double totalDescent,
        double minElevation,
        double maxElevation,
        double totalDistance,
        double averageSlope
    ) {}
}

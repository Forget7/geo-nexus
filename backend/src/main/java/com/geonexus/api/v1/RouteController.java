package com.geonexus.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * 路径规划控制器
 * 集成 OSRM (Open Source Routing Machine) 公开API
 */
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "路径规划", description = "基于OSRM的路径规划接口，支持驾车、步行、骑行")
public class RouteController {

    private static final String OSRM_BASE_URL = "https://router.project-osrm.org";
    private final RestTemplate restTemplate;

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "路径规划请求")
    public static class RoutePlanRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "起点坐标 [经度, 纬度]", example = "[116.4, 39.9]")
        private Double[] from; // [lon, lat]
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "终点坐标 [经度, 纬度]", example = "[116.5, 39.95]")
        private Double[] to;    // [lon, lat]
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "出行模式", example = "driving", allowableValues = {"driving", "walking", "cycling"})
        private String mode;    // driving | walking | cycling
    }

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "路径规划响应")
    public static class RoutePlanResponse {
        @io.swagger.v3.oas.annotations.media.Schema(description = "路线几何（GeoJSON LineString）")
        private String geometry;   // GeoJSON LineString
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "路线距离（米）", example = "1234.5")
        private Double distance;   // meters
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "预计时间（秒）", example = "300.0")
        private Double duration;   // seconds
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "警告信息")
        private String warning;    // optional
    }

    /**
     * 路径规划
     * POST /api/v1/routes/plan
     */
    @Operation(summary = "路径规划", description = "根据起点和终点坐标计算最优路径，支持驾车、步行、骑行模式")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "规划成功"),
        @ApiResponse(responseCode = "400", description = "坐标参数无效")
    })
    @PostMapping("/plan")
    public ResponseEntity<?> planRoute(@RequestBody RoutePlanRequest request) {
        // 参数校验
        if (request.getFrom() == null || request.getFrom().length < 2 ||
                request.getTo() == null || request.getTo().length < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "起点和终点坐标无效"));
        }

        Double fromLon = request.getFrom()[0];
        Double fromLat = request.getFrom()[1];
        Double toLon = request.getTo()[0];
        Double toLat = request.getTo()[1];

        String mode = request.getMode() != null ? request.getMode() : "driving";
        String osrmProfile;
        switch (mode) {
            case "walking" -> osrmProfile = "foot";
            case "cycling" -> osrmProfile = "bike";
            default -> osrmProfile = "car";
        }

        // 调用 OSRM API
        String osrmUrl = String.format(
                "%s/route/v1/%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                OSRM_BASE_URL, osrmProfile, fromLon, fromLat, toLon, toLat
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "GeoNexus/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    osrmUrl, HttpMethod.GET, entity, String.class
            );

            if (resp.getStatusCode() != HttpStatus.OK) {
                return buildMockResponse(fromLon, fromLat, toLon, toLat, mode, "OSRM服务器返回错误");
            }

            // 解析 OSRM 响应
            return parseOsrmResponse(resp.getBody(), fromLon, fromLat, toLon, toLat, mode);

        } catch (Exception e) {
            log.warn("OSRM调用失败，使用模拟数据: {}", e.getMessage());
            return buildMockResponse(fromLon, fromLat, toLon, toLat, mode,
                    "OSRM服务不可用，已返回模拟路线数据");
        }
    }

    private ResponseEntity<?> parseOsrmResponse(String body, Double fromLon, Double fromLat,
                                                  Double toLon, Double toLat, String mode) {
        try {
            // 简单的字符串解析（避免引入额外JSON库依赖）
            // OSRM返回格式: {"code":"Ok","routes":[{"geometry":{"coordinates":[[lon,lat],...],"type":"LineString"},"distance":123.4,"duration":56.7}]}
            
            // 检查是否成功
            if (!body.contains("\"code\":\"Ok\"")) {
                return buildMockResponse(fromLon, fromLat, toLon, toLat, mode, "OSRM路由计算失败");
            }

            // 提取距离
            Double distance = extractDouble(body, "\"distance\":", ",");
            if (distance == null) distance = haversineDistance(fromLon, fromLat, toLon, toLat);

            // 提取时间
            Double duration = extractDouble(body, "\"duration\":", "}");
            if (duration == null) duration = distance / 13.9; // 粗略估算

            // 提取几何坐标（简化处理）
            String geometry = buildSimpleLineString(fromLon, fromLat, toLon, toLat);

            RoutePlanResponse result = new RoutePlanResponse();
            result.setGeometry(geometry);
            result.setDistance(distance);
            result.setDuration(duration);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("解析OSRM响应失败", e);
            return buildMockResponse(fromLon, fromLat, toLon, toLat, mode, "解析响应失败");
        }
    }

    private Double extractDouble(String json, String key, String endChar) {
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return null;
        int start = keyIdx + key.length();
        int end = json.indexOf(endChar, start);
        if (end < 0) end = json.length();
        try {
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildSimpleLineString(Double fromLon, Double fromLat, Double toLon, Double toLat) {
        // 返回简单的两点线（实际使用时前端会插值）
        return String.format(
                "{\"type\":\"LineString\",\"coordinates\":[[%.6f,%.6f],[%.6f,%.6f]]}",
                fromLon, fromLat, toLon, toLat
        );
    }

    private ResponseEntity<RoutePlanResponse> buildMockResponse(Double fromLon, Double fromLat,
                                                                   Double toLon, Double toLat,
                                                                   String mode, String warning) {
        Double distance = haversineDistance(fromLon, fromLat, toLon, toLat);
        Double speedMps = switch (mode) {
            case "walking" -> 1.4;
            case "cycling" -> 4.2;
            default -> 13.9; // driving m/s
        };
        Double duration = distance / speedMps;

        RoutePlanResponse result = new RoutePlanResponse();
        result.setGeometry(buildSimpleLineString(fromLon, fromLat, toLon, toLat));
        result.setDistance(distance);
        result.setDuration(duration);
        result.setWarning(warning != null ? warning : "需配置OSRM服务器以获取真实路线");

        return ResponseEntity.ok(result);
    }

    /**
     * Haversine公式计算两点间距离（米）
     */
    private Double haversineDistance(Double lon1, Double lat1, Double lon2, Double lat2) {
        final double R = 6371000; // 地球半径（米）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

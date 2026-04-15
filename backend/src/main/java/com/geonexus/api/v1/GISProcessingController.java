package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.GISProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * GIS批处理API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gis-processing")
@RequiredArgsConstructor
@Tag(name = "GIS批处理", description = "大规模空间数据批量处理服务")
public class GISProcessingController {

    private final GISProcessingService gisProcessingService;

    @PostMapping("/batch-buffer")
    @Operation(summary = "批量缓冲区分析", description = "对一组几何对象批量执行缓冲区分析")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchBuffer(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> geometries = (List<Map<String, Object>>) request.get("geometries");
        double distanceKm = ((Number) request.getOrDefault("distanceKm", 1.0)).doubleValue();
        Map<String, Object> result = gisProcessingService.batchBuffer(geometries, distanceKm);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/batch-distance")
    @Operation(summary = "批量距离计算", description = "对两组对应点位批量计算距离")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchDistance(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<List<Double>> points1 = (List<List<Double>>) request.get("points1");
        @SuppressWarnings("unchecked")
        List<List<Double>> points2 = (List<List<Double>>) request.get("points2");
        Map<String, Object> result = gisProcessingService.batchDistance(points1, points2);
        if (result.containsKey("error")) {
            return ResponseEntity.badRequest().body(ApiResponse.fail((String) result.get("error")));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/grid-aggregation")
    @Operation(summary = "网格聚合", description = "按网格大小对点位进行空间聚合")
    public ResponseEntity<ApiResponse<Map<String, Object>>> gridAggregation(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> points = (List<Map<String, Object>>) request.get("points");
        double gridSizeDegrees = ((Number) request.getOrDefault("gridSizeDegrees", 0.01)).doubleValue();
        Map<String, Object> result = gisProcessingService.gridAggregation(points, gridSizeDegrees);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/spatial-join")
    @Operation(summary = "空间连接", description = "将点要素关联到匹配的多边形要素")
    public ResponseEntity<ApiResponse<Map<String, Object>>> spatialJoinPointsToPolygon(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> points = (List<Map<String, Object>>) request.get("points");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> polygons = (List<Map<String, Object>>) request.get("polygons");
        Map<String, Object> result = gisProcessingService.spatialJoinPointsToPolygon(points, polygons);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/simplify-path")
    @Operation(summary = "路径简化", description = "使用Douglas-Peucker算法简化路径坐标，保留关键点")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simplifyPath(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<List<Double>> coordinates = (List<List<Double>>) request.get("coordinates");
        double tolerance = ((Number) request.getOrDefault("tolerance", 0.001)).doubleValue();
        Map<String, Object> result = gisProcessingService.simplifyPath(coordinates, tolerance);
        if (result.containsKey("error")) {
            return ResponseEntity.badRequest().body(ApiResponse.fail((String) result.get("error")));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

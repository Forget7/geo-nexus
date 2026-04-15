package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.AdvancedGeoProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 高级几何计算控制器
 * 暴露AdvancedGeoProcessingService的REST API
 */
@RestController
@RequestMapping("/api/v1/geo-processing")
@Tag(name = "高级几何计算", description = "三角剖分/Voronoi/缓冲区/等高线/几何验证")
@RequiredArgsConstructor
public class GeoProcessingController {

    private final AdvancedGeoProcessingService geoProcessingService;

    @PostMapping("/delaunay")
    @Operation(summary = "Delaunay三角剖分", description = "将点集转为Delaunay三角网")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delaunayTriangulation(
            @RequestBody List<List<Double>> points) {
        return ResponseEntity.ok(ApiResponse.success(
            geoProcessingService.delaunayTriangulation(points)));
    }

    @PostMapping("/voronoi")
    @Operation(summary = "Voronoi图", description = "基于点和范围生成Voronoi图")
    public ResponseEntity<ApiResponse<Map<String, Object>>> voronoiDiagram(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<List<Double>> points = (List<List<Double>>) request.get("points");
        @SuppressWarnings("unchecked")
        List<Double> boundingBox = (List<Double>) request.get("bounds");
        return ResponseEntity.ok(ApiResponse.success(
            geoProcessingService.voronoiDiagram(points, boundingBox)));
    }

    @PostMapping("/merge-lines")
    @Operation(summary = "合并线", description = "将多个线段合并为单一几何")
    public ResponseEntity<ApiResponse<Map<String, Object>>> mergeLines(
            @RequestBody List<List<List<Double>>> lineStrings) {
        return ResponseEntity.ok(ApiResponse.success(
            geoProcessingService.mergeLines(lineStrings)));
    }

    @PostMapping("/dissolve")
    @Operation(summary = "融合多边形", description = "按属性融合相邻多边形")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dissolvePolygons(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<List<List<Double>>> polygons = (List<List<List<Double>>>) request.get("polygons");
        String dissolveField = (String) request.get("dissolveField");
        @SuppressWarnings("unchecked")
        List<Object> fieldValues = (List<Object>) request.get("fieldValues");
        return ResponseEntity.ok(ApiResponse.success(
            geoProcessingService.dissolvePolygons(polygons, dissolveField, fieldValues)));
    }

    @PostMapping("/clip")
    @Operation(summary = "几何裁剪", description = "用裁剪几何裁剪目标几何")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clipGeometry(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<List<Double>> target = (List<List<Double>>) request.get("target");
        @SuppressWarnings("unchecked")
        List<List<List<Double>>> clipper = (List<List<List<Double>>>) request.get("clipper");
        return ResponseEntity.ok(ApiResponse.success(
            geoProcessingService.clipGeometry(target, clipper)));
    }

    @PostMapping("/buffer")
    @Operation(summary = "缓冲区分析", description = "生成带端点样式的缓冲区")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bufferWithEndcaps(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<List<Double>> points = (List<List<Double>>) request.get("points");
        double distanceKm = ((Number) request.get("distance")).doubleValue();
        String endcapStyle = (String) request.getOrDefault("endcapType", "ROUND");
        return ResponseEntity.ok(ApiResponse.success(
            geoProcessingService.bufferWithEndcaps(points, distanceKm, endcapStyle)));
    }

    @PostMapping("/interpolate/idw")
    @Operation(summary = "IDW空间插值", description = "反距离加权插值生成网格")
    public ResponseEntity<ApiResponse<Map<String, Object>>> interpolateIDW(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<List<Double>> samplePoints = (List<List<Double>>) request.get("samplePoints");
        @SuppressWarnings("unchecked")
        List<Double> values = (List<Double>) request.get("values");
        @SuppressWarnings("unchecked")
        List<List<Double>> grid = (List<List<Double>>) request.get("grid");
        double power = request.containsKey("power")
                ? ((Number) request.get("power")).doubleValue() : 2.0;
        return ResponseEntity.ok(ApiResponse.success(
            geoProcessingService.interpolateIDW(samplePoints, values, grid, power)));
    }

    @PostMapping("/contours")
    @Operation(summary = "等高线生成", description = "从网格数据生成等高线")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateContours(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<List<Double>> grid = (List<List<Double>>) request.get("grid");
        @SuppressWarnings("unchecked")
        List<Double> levels = (List<Double>) request.get("levels");
        return ResponseEntity.ok(ApiResponse.success(
            geoProcessingService.generateContours(grid, levels)));
    }

    @PostMapping("/validate")
    @Operation(summary = "几何验证与修复", description = "检测并修复无效几何")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateAndRepair(
            @RequestBody List<List<List<Double>>> polygons) {
        return ResponseEntity.ok(ApiResponse.success(
            geoProcessingService.validateAndRepair(polygons)));
    }
}

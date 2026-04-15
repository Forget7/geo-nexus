package com.geonexus.api.v1;

import com.geonexus.service.GeoServerLayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GeoServer Layer/FeatureType 管理控制器
 */
@Tag(name = "GeoServer Layer", description = "GeoServer图层与数据存储管理")
@RestController
@RequestMapping("/api/v1/geoserver/layers")
@RequiredArgsConstructor
public class GeoServerLayerController {

    private final GeoServerLayerService layerService;

    @PostMapping("/stores/postgis")
    @Operation(summary = "创建PostGIS数据存储", description = "在GeoServer中创建连接PostGIS数据库的DataStore")
    public ResponseEntity<Map<String, Object>> createPostGISStore(
            @RequestBody Map<String, Object> request) {
        String storeName = (String) request.get("storeName");
        String database = (String) request.get("database");
        String host = (String) request.get("host");
        int port = ((Number) request.getOrDefault("port", 5432)).intValue();
        String user = (String) request.get("user");
        String password = (String) request.get("password");

        if (storeName == null || database == null || host == null || user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "storeName, database, host, user are required"));
        }

        boolean success = layerService.createPostGISStore(storeName, database, host, port, user, password);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "storeName", storeName,
                "message", success ? "PostGIS Store created" : "Failed to create PostGIS Store"
        ));
    }

    @PostMapping("/stores")
    @Operation(summary = "创建空DataStore", description = "创建一个空的DataStore，用于后续上传数据")
    public ResponseEntity<Map<String, Object>> createEmptyDataStore(
            @RequestBody Map<String, Object> request) {
        String storeName = (String) request.get("storeName");
        if (storeName == null || storeName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "storeName is required"));
        }
        boolean success = layerService.createEmptyDataStore(storeName);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "storeName", storeName,
                "message", success ? "DataStore created" : "Failed to create DataStore"
        ));
    }

    @PostMapping("/publish")
    @Operation(summary = "发布FeatureType", description = "将PostGIS表发布为GeoServer WFS图层")
    public ResponseEntity<Map<String, Object>> publishFeatureType(
            @RequestBody Map<String, Object> request) {
        String storeName = (String) request.get("storeName");
        String featureName = (String) request.get("featureName");
        String srs = (String) request.getOrDefault("srs", "EPSG:4326");

        if (storeName == null || featureName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "storeName and featureName are required"));
        }

        String[] bbox = null;
        if (request.containsKey("bbox")) {
            @SuppressWarnings("unchecked")
            List<Number> bboxList = (List<Number>) request.get("bbox");
            if (bboxList != null && bboxList.size() == 4) {
                bbox = bboxList.stream().map(Object::toString).toArray(String[]::new);
            }
        }

        boolean success = layerService.publishFeatureType(storeName, featureName, srs, bbox);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "featureName", featureName,
                "storeName", storeName,
                "message", success ? "FeatureType published" : "Failed to publish FeatureType"
        ));
    }

    @GetMapping("/{layerName}/info")
    @Operation(summary = "获取图层信息", description = "查询GeoServer中图层的详细信息")
    public ResponseEntity<Map<String, Object>> getLayerInfo(@PathVariable String layerName) {
        Map<String, Object> info = layerService.getLayerInfo(layerName);
        if (info.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }
}

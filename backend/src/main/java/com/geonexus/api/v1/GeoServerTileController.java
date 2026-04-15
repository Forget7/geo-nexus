package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.GeoServerTileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GeoServer TileCache / 瓦片图层控制器
 * 职责：TileCache 创建、GeoJSON 上传
 */
@RestController
@RequestMapping("/api/v1/geoserver/tiles")
@Tag(name = "GeoServer瓦片", description = "GeoServer瓦片图层管理与GeoJSON上传")
@RequiredArgsConstructor
public class GeoServerTileController {

    private final GeoServerTileService tileService;

    @PostMapping("/cache")
    @Operation(summary = "创建TileCache", description = "为图层创建GeoServer TileCache")
    public ResponseEntity<ApiResponse<Boolean>> createTileCache(
            @RequestBody Map<String, String> request) {
        String layerName = request.get("layerName");
        String gridSetId = request.get("gridSetId");
        if (layerName == null || layerName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("layerName is required"));
        }
        if (gridSetId == null || gridSetId.isBlank()) {
            gridSetId = "EPSG:4326";
        }
        boolean success = tileService.createTileCache(layerName, gridSetId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success(true, "TileCache created for: " + layerName));
        } else {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to create TileCache for: " + layerName));
        }
    }

    @PostMapping("/upload")
    @Operation(summary = "上传GeoJSON", description = "通过GeoServer REST API上传GeoJSON并发布为图层")
    public ResponseEntity<ApiResponse<Boolean>> uploadGeoJSON(
            @RequestBody Map<String, Object> request) {
        String layerName = (String) request.get("layerName");
        String geoJson = (String) request.get("geoJson");
        if (layerName == null || layerName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("layerName is required"));
        }
        if (geoJson == null || geoJson.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("geoJson is required"));
        }
        boolean success = tileService.uploadGeoJSON(layerName, geoJson);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success(true, "GeoJSON uploaded: " + layerName));
        } else {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to upload GeoJSON: " + layerName));
        }
    }
}
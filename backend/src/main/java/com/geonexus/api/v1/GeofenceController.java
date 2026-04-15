package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.GeofenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 地理围栏控制器 - 地理围栏创建与管理
 */
@RestController
@RequestMapping("/api/v1/geofences")
@Tag(name = "地理围栏", description = "地理围栏创建与管理")
@RequiredArgsConstructor
public class GeofenceController {

    private final GeofenceService geofenceService;

    @PostMapping
    @Operation(summary = "创建地理围栏")
    public ResponseEntity<ApiResponse<GeofenceService.Geofence>> createGeofence(
            @RequestBody GeofenceService.Geofence geofence) {
        return ResponseEntity.ok(ApiResponse.success(geofenceService.createGeofence(geofence)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取地理围栏")
    public ResponseEntity<ApiResponse<GeofenceService.Geofence>> getGeofence(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(geofenceService.getGeofence(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新地理围栏")
    public ResponseEntity<ApiResponse<GeofenceService.Geofence>> updateGeofence(
            @PathVariable String id, @RequestBody GeofenceService.Geofence updates) {
        return ResponseEntity.ok(ApiResponse.success(geofenceService.updateGeofence(id, updates)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地理围栏")
    public ResponseEntity<ApiResponse<Void>> deleteGeofence(@PathVariable String id) {
        geofenceService.deleteGeofence(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @Operation(summary = "列出地理围栏")
    public ResponseEntity<ApiResponse<List<GeofenceService.Geofence>>> getGeofences() {
        return ResponseEntity.ok(ApiResponse.success(geofenceService.getGeofences()));
    }

    @PostMapping("/check")
    @Operation(summary = "检查点是否在围栏内")
    public ResponseEntity<ApiResponse<Boolean>> checkPoint(
            @RequestBody Map<String, Object> body) {
        double lon = ((Number) body.get("lon")).doubleValue();
        double lat = ((Number) body.get("lat")).doubleValue();
        String geofenceId = (String) body.get("geofenceId");
        return ResponseEntity.ok(ApiResponse.success(
                geofenceService.isPointInside(geofenceId, lon, lat)));
    }
}

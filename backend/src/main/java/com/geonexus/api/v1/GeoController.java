package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.GeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 地理编码与路线规划控制器
 */
@RestController
@RequestMapping("/api/v1/geo")
@Tag(name = "地理编码与路线", description = "地理编码/逆编码/路线规划/服务区分析")
@RequiredArgsConstructor
public class GeoController {

    private final GeoService geoService;

    // ===== Geocoding =====

    @GetMapping("/geocode")
    @Operation(summary = "地址转坐标（地理编码）")
    public ResponseEntity<ApiResponse<GeoService.GeocodingResult>> geocode(
            @RequestParam String address) {
        return ResponseEntity.ok(ApiResponse.success(geoService.geocode(address)));
    }

    @PostMapping("/geocode/batch")
    @Operation(summary = "批量地理编码")
    public ResponseEntity<ApiResponse<List<GeoService.GeocodingResult>>> batchGeocode(
            @RequestBody List<String> addresses) {
        return ResponseEntity.ok(ApiResponse.success(geoService.batchGeocode(addresses)));
    }

    @GetMapping("/geocode/suggest")
    @Operation(summary = "地址建议", description = "输入提示/自动补全")
    public ResponseEntity<ApiResponse<List<GeoService.AddressSuggestion>>> suggest(
            @RequestParam String query, @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(geoService.suggest(query, limit)));
    }

    // ===== Reverse Geocoding =====

    @GetMapping("/reverse-geocode")
    @Operation(summary = "坐标转地址（逆地理编码）")
    public ResponseEntity<ApiResponse<GeoService.GeocodingResult>> reverseGeocode(
            @RequestParam double lon, @RequestParam double lat) {
        return ResponseEntity.ok(ApiResponse.success(geoService.reverseGeocode(lon, lat)));
    }

    // ===== Routing =====

    @PostMapping("/route")
    @Operation(summary = "路线规划")
    public ResponseEntity<ApiResponse<GeoService.RouteResult>> route(
            @RequestBody GeoService.RouteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(geoService.route(request)));
    }

    @PostMapping("/route/multi-stop")
    @Operation(summary = "多站点路线", description = "按最优顺序访问多个站点")
    public ResponseEntity<ApiResponse<GeoService.RouteResult>> multiStopRoute(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<GeoService.RoutePoint> stops = (List<GeoService.RoutePoint>) body.get("stops");
        String mode = (String) body.getOrDefault("mode", "driving");
        return ResponseEntity.ok(ApiResponse.success(geoService.multiStopRoute(stops, mode)));
    }

    // ===== Service Area =====

    @PostMapping("/service-area")
    @Operation(summary = "服务区分析", description = "从中心点出发给定时间内可达的范围")
    public ResponseEntity<ApiResponse<GeoService.ServiceAreaResult>> serviceArea(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        GeoService.RoutePoint center = (GeoService.RoutePoint) body.get("center");
        double radius = ((Number) body.get("radius")).doubleValue();
        String mode = (String) body.getOrDefault("mode", "driving");
        int intervals = ((Number) body.getOrDefault("intervals", 3)).intValue();
        return ResponseEntity.ok(ApiResponse.success(
            geoService.serviceArea(center, radius, mode, intervals)));
    }
}

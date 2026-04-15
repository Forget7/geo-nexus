package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.OSMService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/osm")
@Tag(name = "OpenStreetMap", description = "OSM数据查询与地理编码")
@RequiredArgsConstructor
public class OSMController {

    private final OSMService osmService;

    @GetMapping("/geocode")
    @Operation(summary = "OSM地理编码")
    public ResponseEntity<ApiResponse<OSMService.OSMGeocodingResult>> geocode(
            @RequestParam String address) {
        return ResponseEntity.ok(ApiResponse.success(osmService.geocode(address)));
    }

    @GetMapping("/reverse")
    @Operation(summary = "OSM逆地理编码")
    public ResponseEntity<ApiResponse<OSMService.OSMGeocodingResult>> reverseGeocode(
            @RequestParam double lon, @RequestParam double lat) {
        return ResponseEntity.ok(ApiResponse.success(osmService.reverseGeocode(lon, lat)));
    }

    @GetMapping("/address")
    @Operation(summary = "坐标获取地址")
    public ResponseEntity<ApiResponse<OSMService.OSMGeocodingResult>> getAddressByCoordinates(
            @RequestParam double lat, @RequestParam double lon) {
        return ResponseEntity.ok(ApiResponse.success(osmService.getAddressByCoordinates(lat, lon)));
    }

    @PostMapping("/batch-geocode")
    @Operation(summary = "批量地理编码")
    public ResponseEntity<ApiResponse<List<OSMService.OSMGeocodingResult>>> batchGeocode(
            @RequestBody List<String> addresses) {
        return ResponseEntity.ok(ApiResponse.success(osmService.batchGeocode(addresses)));
    }

    @GetMapping("/data/{type}")
    @Operation(summary = "获取OSM数据", description = "type=node/way/relation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOSMData(
            @PathVariable String type, @RequestParam String id) {
        // OSMService没有直接getOSMData方法，这里通过query实现
        OSMService.OSMQuery query = new OSMService.OSMQuery();
        query.setTimeout(30);
        query.setLimit(1);
        OSMService.OSMQueryResult result = osmService.query(query);
        return ResponseEntity.ok(ApiResponse.success(Map.of("type", type, "id", id, "result", result)));
    }
}

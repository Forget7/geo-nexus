package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.SatelliteImageryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/satellite")
@Tag(name = "卫星影像", description = "卫星影像元数据管理")
@RequiredArgsConstructor
public class SatelliteController {

    private final SatelliteImageryService satelliteService;

    @PostMapping("/images")
    @Operation(summary = "添加影像")
    public ResponseEntity<ApiResponse<SatelliteImageryService.SatelliteImage>> addImage(
            @RequestBody SatelliteImageryService.SatelliteImage image) {
        return ResponseEntity.ok(ApiResponse.success(satelliteService.addImage(image)));
    }

    @GetMapping("/images/{id}")
    @Operation(summary = "获取影像")
    public ResponseEntity<ApiResponse<SatelliteImageryService.SatelliteImage>> getImage(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(satelliteService.getImage(id)));
    }

    @PutMapping("/images/{id}")
    @Operation(summary = "更新影像")
    public ResponseEntity<ApiResponse<SatelliteImageryService.SatelliteImage>> updateImage(
            @PathVariable String id, @RequestBody SatelliteImageryService.SatelliteImage updates) {
        return ResponseEntity.ok(ApiResponse.success(satelliteService.updateImage(id, updates)));
    }

    @DeleteMapping("/images/{id}")
    @Operation(summary = "删除影像")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable String id) {
        satelliteService.deleteImage(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/images")
    @Operation(summary = "列出影像")
    public ResponseEntity<ApiResponse<List<SatelliteImageryService.SatelliteImage>>> getImages(
            @RequestParam(required = false) String satellite,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                satelliteService.getImages(satellite, startDate, endDate, limit)));
    }

    @GetMapping("/images/{id}/bands")
    @Operation(summary = "获取影像波段信息")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBandInfo(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(satelliteService.getBandInfo(id)));
    }

    @GetMapping("/satellites")
    @Operation(summary = "支持的卫星列表")
    public ResponseEntity<ApiResponse<List<String>>> getSupportedSatellites() {
        return ResponseEntity.ok(ApiResponse.success(satelliteService.getSupportedSatellites()));
    }
}

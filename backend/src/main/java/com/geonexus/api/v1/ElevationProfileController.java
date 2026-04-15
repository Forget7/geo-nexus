package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.ElevationProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地形剖面图控制器
 */
@RestController
@RequestMapping("/api/v1/elevation")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ElevationProfileController {

    private final ElevationProfileService elevationService;

    @PostMapping("/profile")
    @Operation(summary = "采样地形剖面", description = "沿给定线路采样高程，返回剖面数据")
    public ResponseEntity<ApiResponse<ElevationProfileService.ElevationProfileResult>> getProfile(
            @RequestBody ElevationProfileRequest request) {
        if (request.getCoordinates() == null || request.getCoordinates().size() < 2) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("至少需要2个坐标点"));
        }
        var result = elevationService.sampleElevation(request.getCoordinates());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Data
    public static class ElevationProfileRequest {
        private List<List<Double>> coordinates; // [[lng, lat], [lng, lat], ...]
    }
}

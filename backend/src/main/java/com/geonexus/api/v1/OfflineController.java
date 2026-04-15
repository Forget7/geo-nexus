package com.geonexus.api.v1;

import com.geonexus.service.TileCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 离线瓦片包管理 API
 */
@RestController
@RequestMapping("/api/v1/offline")
@RequiredArgsConstructor
@Tag(name = "离线管理", description = "离线瓦片包生成与查询接口")
public class OfflineController {

    private final TileCacheService tileCacheService;

    /**
     * POST /api/v1/offline/package - 创建离线包
     */
    @Operation(summary = "创建离线包", description = "根据指定区域和缩放级别生成离线瓦片包")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/package")
    public ResponseEntity<TileCacheService.OfflinePackageResult> createOfflinePackage(
            @RequestBody TileCacheService.OfflinePackageRequest request) {
        TileCacheService.OfflinePackageResult result = tileCacheService.generateOfflinePackage(request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/offline/package/{jobId} - 查询离线包状态
     */
    @Operation(summary = "查询离线包状态", description = "查询离线包生成任务的状态和下载URL")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/package/{jobId}")
    public ResponseEntity<TileCacheService.OfflinePackageResult> getPackageStatus(
            @Parameter(description = "离线包任务ID") @PathVariable String jobId) {
        return ResponseEntity.ok(tileCacheService.getPackageStatus(jobId));
    }

    /**
     * GET /api/v1/offline/estimate - 估算瓦片数量和大小
     */
    @Operation(summary = "估算离线包大小", description = "根据边界框和缩放级别估算所需瓦片数量和存储大小")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "估算成功")
    })
    @GetMapping("/estimate")
    public ResponseEntity<Map<String, Object>> estimatePackage(
            @Parameter(description = "最小经度") @RequestParam double minLon,
            @Parameter(description = "最小纬度") @RequestParam double minLat,
            @Parameter(description = "最大经度") @RequestParam double maxLon,
            @Parameter(description = "最大纬度") @RequestParam double maxLat,
            @Parameter(description = "缩放级别，如 1,3,5") @RequestParam String zoomLevels) {
        double[] bounds = { minLon, minLat, maxLon, maxLat };
        java.util.List<Integer> zLevels = java.util.Arrays.stream(zoomLevels.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();

        int tileCount = 0;
        for (int z : zLevels) {
            int minX = (int) Math.floor((minLon + 180.0) / 360.0 * Math.pow(2.0, z));
            int maxX = (int) Math.floor((maxLon + 180.0) / 360.0 * Math.pow(2.0, z));
            int minY = (int) Math.floor((1.0 - Math.log(Math.tan(Math.toRadians(maxLat)) + 1.0 / Math.cos(Math.toRadians(maxLat))) / Math.PI) / 2.0 * Math.pow(2.0, z));
            int maxY = (int) Math.floor((1.0 - Math.log(Math.tan(Math.toRadians(minLat)) + 1.0 / Math.cos(Math.toRadians(minLat))) / Math.PI) / 2.0 * Math.pow(2.0, z));
            tileCount += (maxX - minX + 1) * (maxY - minY + 1);
        }

        long estimatedBytes = tileCount * 15 * 1024L;
        String sizeStr = estimatedBytes < 1024 * 1024
                ? String.format("%.1f KB", estimatedBytes / 1024.0)
                : String.format("%.1f MB", estimatedBytes / (1024.0 * 1024));

        return ResponseEntity.ok(Map.of(
                "tileCount", tileCount,
                "estimatedSize", sizeStr,
                "zoomLevels", zLevels
        ));
    }
}

package com.geonexus.api.v1;

import com.geonexus.service.PointCloudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 点云数据 API - PNTS / Cesium Point Cloud Tile Set
 */
@Tag(name = "PointCloud", description = "点云数据加载与管理")
@RestController
@RequestMapping("/api/v1/pointcloud")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PointCloudController {

    private final PointCloudService pointCloudService;

    @Operation(summary = "获取支持的点云格式列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/formats")
    public ResponseEntity<Map<String, Object>> getFormats() {
        List<String> formats = pointCloudService.getSupportedFormats();
        return ResponseEntity.ok(Map.of(
                "formats", formats,
                "count", formats.size()
        ));
    }

    @Operation(summary = "加载点云数据", description = "加载PNTS/Cesium Point Cloud格式的点云数据")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "加载成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/load")
    public ResponseEntity<PointCloudService.PointCloudLoadResult> loadPointCloud(
            @RequestBody PointCloudService.PointCloudLoadRequest request) {
        PointCloudService.PointCloudLoadResult result = pointCloudService.loadPointCloud(request);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @Operation(summary = "批量加载点云数据", description = "批量加载多个点云数据")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "批量加载完成")
    })
    @PostMapping("/batch-load")
    public ResponseEntity<List<PointCloudService.PointCloudLoadResult>> batchLoad(
            @RequestBody List<PointCloudService.PointCloudLoadRequest> requests) {
        List<PointCloudService.PointCloudLoadResult> results =
                pointCloudService.batchLoadPointClouds(requests);
        return ResponseEntity.ok(results);
    }
}

package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.TrajectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 轨迹控制器 - 轨迹管理与分析 REST API
 */
@RestController
@RequestMapping("/api/v1/trajectories")
@Tag(name = "轨迹管理", description = "轨迹创建/查询/分析")
@RequiredArgsConstructor
public class TrajectoryController {

    private final TrajectoryService trajectoryService;

    @PostMapping
    @Operation(summary = "创建轨迹")
    public ResponseEntity<ApiResponse<TrajectoryService.Trajectory>> createTrajectory(
            @RequestBody TrajectoryService.Trajectory trajectory) {
        return ResponseEntity.ok(ApiResponse.success(trajectoryService.createTrajectory(trajectory)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取轨迹")
    public ResponseEntity<ApiResponse<TrajectoryService.Trajectory>> getTrajectory(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(trajectoryService.getTrajectory(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新轨迹")
    public ResponseEntity<ApiResponse<TrajectoryService.Trajectory>> updateTrajectory(
            @PathVariable String id, @RequestBody TrajectoryService.Trajectory updates) {
        return ResponseEntity.ok(ApiResponse.success(trajectoryService.updateTrajectory(id, updates)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除轨迹")
    public ResponseEntity<ApiResponse<Void>> deleteTrajectory(@PathVariable String id) {
        trajectoryService.deleteTrajectory(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @Operation(summary = "列出轨迹")
    public ResponseEntity<ApiResponse<List<TrajectoryService.Trajectory>>> getTrajectories(
            @RequestParam(required = false) String objectId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
            trajectoryService.getTrajectories(objectId, startTime, endTime, limit)));
    }

    @GetMapping("/{id}/points")
    @Operation(summary = "获取轨迹点")
    public ResponseEntity<ApiResponse<List<TrajectoryService.TrajectoryPoint>>> getTrajectoryPoints(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(trajectoryService.getTrajectoryPoints(id)));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "获取轨迹统计")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrajectoryStats(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(trajectoryService.getTrajectoryStats(id)));
    }

    @GetMapping("/spatial")
    @Operation(summary = "空间过滤轨迹")
    public ResponseEntity<ApiResponse<List<TrajectoryService.Trajectory>>> filterBySpatial(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Double> bbox = (List<Double>) body.get("bbox");
        String startTime = (String) body.getOrDefault("startTime", null);
        String endTime = (String) body.getOrDefault("endTime", null);
        return ResponseEntity.ok(ApiResponse.success(
            trajectoryService.filterBySpatial(bbox, startTime, endTime)));
    }
}

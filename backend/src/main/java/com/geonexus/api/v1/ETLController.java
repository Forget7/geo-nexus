package com.geonexus.api.v1;

import com.geonexus.service.GeoETLService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/etl")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:*")
@Tag(name = "地理数据ETL", description = "数据源管理/转换规则/Job调度")
public class ETLController {

    private final GeoETLService etlService;

    // ===== Data Sources =====

    @PostMapping("/datasources")
    @Operation(summary = "注册数据源")
    public ResponseEntity<ApiResponse<GeoETLService.DataSource>> addDataSource(
            @RequestBody GeoETLService.DataSource source) {
        return ResponseEntity.ok(ApiResponse.success(etlService.addDataSource(source)));
    }

    @GetMapping("/datasources/{id}")
    @Operation(summary = "获取数据源")
    public ResponseEntity<ApiResponse<GeoETLService.DataSource>> getDataSource(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(etlService.getDataSource(id)));
    }

    @DeleteMapping("/datasources/{id}")
    @Operation(summary = "删除数据源")
    public ResponseEntity<ApiResponse<Void>> deleteDataSource(@PathVariable String id) {
        etlService.deleteDataSource(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/datasources/{id}/preview")
    @Operation(summary = "预览数据源（前N条）")
    public ResponseEntity<ApiResponse<GeoETLService.DataPreview>> previewDataSource(
            @PathVariable String id, @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(etlService.previewDataSource(id, limit)));
    }

    // ===== Transform Rules =====

    @PostMapping("/rules")
    @Operation(summary = "创建转换规则")
    public ResponseEntity<ApiResponse<GeoETLService.TransformRule>> createTransformRule(
            @RequestBody GeoETLService.TransformRule rule) {
        return ResponseEntity.ok(ApiResponse.success(etlService.createTransformRule(rule)));
    }

    @GetMapping("/rules/{id}")
    @Operation(summary = "获取转换规则")
    public ResponseEntity<ApiResponse<GeoETLService.TransformRule>> getTransformRule(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(etlService.getTransformRule(id)));
    }

    @GetMapping("/rules")
    @Operation(summary = "列出所有转换规则")
    public ResponseEntity<ApiResponse<List<GeoETLService.TransformRule>>> getAllTransformRules() {
        return ResponseEntity.ok(ApiResponse.success(etlService.getAllTransformRules()));
    }

    // ===== ETL Jobs =====

    @PostMapping("/jobs")
    @Operation(summary = "创建ETL Job")
    public ResponseEntity<ApiResponse<GeoETLService.ETLJob>> createJob(
            @RequestBody GeoETLService.ETLJobRequest request) {
        return ResponseEntity.ok(ApiResponse.success(etlService.createJob(request)));
    }

    @PostMapping("/jobs/{id}/execute")
    @Operation(summary = "执行ETL Job")
    public ResponseEntity<ApiResponse<GeoETLService.ETLJob>> executeJob(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(etlService.executeJob(id)));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "获取Job详情")
    public ResponseEntity<ApiResponse<GeoETLService.ETLJob>> getJob(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(etlService.getJob(id)));
    }

    @PostMapping("/jobs/{id}/cancel")
    @Operation(summary = "取消Job")
    public ResponseEntity<ApiResponse<Void>> cancelJob(@PathVariable String id) {
        etlService.cancelJob(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/jobs/{id}")
    @Operation(summary = "删除Job")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable String id) {
        etlService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/jobs/history")
    @Operation(summary = "Job历史")
    public ResponseEntity<ApiResponse<List<GeoETLService.ETLJob>>> getJobHistory(
            @RequestParam String sourceId, @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(etlService.getJobHistory(sourceId, limit)));
    }

    @PostMapping("/jobs/batch")
    @Operation(summary = "批量执行Job")
    public ResponseEntity<ApiResponse<List<GeoETLService.ETLJob>>> batchExecute(
            @RequestBody List<String> jobIds) {
        return ResponseEntity.ok(ApiResponse.success(etlService.batchExecute(jobIds)));
    }

    @PostMapping("/jobs/{id}/schedule")
    @Operation(summary = "设置Cron调度")
    public ResponseEntity<ApiResponse<String>> scheduleJob(
            @PathVariable String id, @RequestParam String cron) {
        return ResponseEntity.ok(ApiResponse.success(etlService.scheduleJob(id, cron)));
    }
}

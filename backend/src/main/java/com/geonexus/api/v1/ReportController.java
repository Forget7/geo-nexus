package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报告生成 REST API
 * 暴露 ReportService 的报告生成、模板管理能力
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "报告生成", description = "地图报告生成与导出")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/generate")
    @Operation(summary = "生成报告")
    public ResponseEntity<ApiResponse<ReportService.GeneratedReport>> generateReport(
            @RequestBody ReportService.ReportRequest request) {
        ReportService.GeneratedReport report = reportService.generateReport(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "获取报告")
    public ResponseEntity<ApiResponse<ReportService.GeneratedReport>> getReport(
            @PathVariable String reportId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(reportService.getReport(reportId)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{reportId}/download")
    @Operation(summary = "下载报告（HTML）")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String reportId) {
        byte[] html = reportService.downloadReport(reportId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setContentDispositionFormData("attachment", "report_" + reportId + ".html");
        return new ResponseEntity<>(html, headers, 200);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量生成报告")
    public ResponseEntity<ApiResponse<List<ReportService.GeneratedReport>>> batchGenerate(
            @RequestBody ReportService.BatchReportRequest request) {
        List<ReportService.GeneratedReport> reports = reportService.batchGenerate(request);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/templates")
    @Operation(summary = "获取报告模板")
    public ResponseEntity<ApiResponse<List<ReportService.ReportTemplate>>> getTemplates(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(
            reportService.getTemplates(category)));
    }

    @PostMapping("/templates")
    @Operation(summary = "创建报告模板")
    public ResponseEntity<ApiResponse<ReportService.ReportTemplate>> createTemplate(
            @RequestBody ReportService.ReportTemplate template) {
        ReportService.ReportTemplate created = reportService.createTemplate(template);
        return ResponseEntity.ok(ApiResponse.success(created));
    }
}

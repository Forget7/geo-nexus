package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.EnterpriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业功能 API
 */
@RestController
@RequestMapping("/api/v1/enterprise")
@Tag(name = "企业功能", description = "企业级扩展功能")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @GetMapping("/stats")
    @Operation(summary = "获取企业统计")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(enterpriseService.getStats()));
    }

    @GetMapping("/usage")
    @Operation(summary = "获取使用报告")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsageReport(
            @RequestParam(required = false) String period) {
        return ResponseEntity.ok(ApiResponse.success(
            enterpriseService.getUsageReport(period)));
    }

    @PostMapping("/audit")
    @Operation(summary = "记录审计日志")
    public ResponseEntity<ApiResponse<Void>> logAuditEvent(
            @RequestBody Map<String, Object> event) {
        enterpriseService.logAuditEvent(event);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/audit/logs")
    @Operation(summary = "查询审计日志")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
            enterpriseService.getAuditLogs(userId, action, limit)));
    }
}

package com.geonexus.api.v1;

import com.geonexus.api.config.SecurityConfig;
import com.geonexus.domain.AuditLog;
import com.geonexus.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计日志查询 API
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "审计管理", description = "审计日志查询与统计接口")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final SecurityConfig securityConfig;

    /**
     * GET /api/v1/audit/logs - 查询审计日志（分页）
     * 要求管理员角色
     */
    @Operation(summary = "查询审计日志", description = "分页查询审计日志，支持按用户、操作类型、资源类型和时间范围过滤（仅管理员）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLog>> queryAuditLogs(
            @Parameter(description = "认证令牌") @RequestHeader(value = "Authorization", required = false) String auth,
            @Parameter(description = "操作用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String action,
            @Parameter(description = "资源类型") @RequestParam(required = false) String resourceType,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "每页数量，最大100") @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page) {

        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).build();
        }

        Instant start = startTime != null ? startTime.toInstant(ZoneOffset.UTC) : null;
        Instant end = endTime != null ? endTime.toInstant(ZoneOffset.UTC) : null;
        Pageable pageable = PageRequest.of(page, Math.min(limit, 100));

        Page<AuditLog> logs = auditLogRepository.searchAuditLogs(
                userId, action, resourceType, start, end, pageable);

        return ResponseEntity.ok(logs);
    }

    /**
     * GET /api/v1/audit/logs/:id - 获取单条审计日志详情
     * 要求管理员角色
     */
    @Operation(summary = "获取审计日志详情", description = "根据ID获取单条审计日志的详细信息（仅管理员）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "日志不存在")
    })
    @GetMapping("/logs/{id}")
    public ResponseEntity<AuditLog> getAuditLog(
            @Parameter(description = "认证令牌") @RequestHeader(value = "Authorization", required = false) String auth,
            @Parameter(description = "审计日志ID") @PathVariable String id) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).build();
        }
        return auditLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/audit/stats - 审计统计
     * 要求管理员角色
     */
    @Operation(summary = "获取审计统计", description = "统计指定时间范围内各类操作的数量（仅管理员）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAuditStats(
            @Parameter(description = "认证令牌") @RequestHeader(value = "Authorization", required = false) String auth,
            @Parameter(description = "统计起始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @Parameter(description = "资源类型") @RequestParam(required = false) String resourceType) {

        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).build();
        }

        Instant sinceInstant = since != null ? since.toInstant(ZoneOffset.UTC) : Instant.now().minusSeconds(86400);

        List<AuditLog> logs = sinceInstant != null
                ? auditLogRepository.findByTimeRange(sinceInstant, Instant.now())
                : auditLogRepository.findAll();

        Map<String, Long> byAction = new HashMap<>();
        Map<String, Long> byResource = new HashMap<>();
        long total = logs.size();

        for (AuditLog log : logs) {
            byAction.merge(log.getAction(), 1L, Long::sum);
            if (log.getResourceType() != null) {
                byResource.merge(log.getResourceType(), 1L, Long::sum);
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("byAction", byAction);
        stats.put("byResource", byResource);
        stats.put("since", since);

        return ResponseEntity.ok(stats);
    }

    /**
     * 验证调用者是否具有管理员角色
     */
    private boolean isAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        try {
            String userId = securityConfig.extractUserIdFromAuth(authHeader);
            if (userId == null) return false;
            // 简单admin检查：实际生产应查询用户角色
            // 这里通过JWT claims中的role字段判断
            var claims = securityConfig.validateToken(authHeader.substring(7));
            String role = claims.get("role");
            return "admin".equals(role);
        } catch (Exception e) {
            return false;
        }
    }
}

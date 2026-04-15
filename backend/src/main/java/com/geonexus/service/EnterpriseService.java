package com.geonexus.service;

import com.geonexus.config.DataSourceProperties;
import com.geonexus.config.DataSourceProperties.ServiceEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 企业级服务整合 - 审批/审计/调度/记忆/监控/数据源
 */
@Slf4j
@Service
public class EnterpriseService {
    
    private final CacheService cacheService;
    private final DataSourceProperties dataSourceProperties;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // 审批流程
    private final Map<String, ApprovalWorkflow> workflows = new ConcurrentHashMap<>();
    private final Map<String, ApprovalRequest> approvalRequests = new ConcurrentHashMap<>();
    
    // 审计日志
    private final List<AuditLog> auditLogs = new ArrayList<>();
    
    // 任务调度
    private final Map<String, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();
    
    // 记忆服务
    private final Map<String, List<MemoryEntry>> userMemories = new ConcurrentHashMap<>();
    
    // 系统监控
    private final SystemMetrics metrics = new SystemMetrics();
    
    // 数据源
    private final Map<String, DataSourceConfig> dataSources = new ConcurrentHashMap<>();
    
    public EnterpriseService(CacheService cacheService, DataSourceProperties dataSourceProperties) {
        this.cacheService = cacheService;
        this.dataSourceProperties = dataSourceProperties;
        initializeDataSources();
        startMetricsCollection();
    }
    
    // ==================== 审批流程 ====================
    
    public String createApprovalRequest(ApprovalRequest request) {
        request.setId(UUID.randomUUID().toString());
        request.setStatus("pending");
        request.setCreatedAt(System.currentTimeMillis());
        
        approvalRequests.put(request.getId(), request);
        
        // 通知审批人
        notifyApprovers(request);
        
        log.info("创建审批请求: id={}, type={}, applicant={}", 
                request.getId(), request.getType(), request.getApplicantId());
        
        return request.getId();
    }
    
    public ApprovalRequest getApprovalRequest(String requestId) {
        return approvalRequests.get(requestId);
    }
    
    public void approve(String requestId, String approverId, String comment) {
        ApprovalRequest request = approvalRequests.get(requestId);
        if (request == null) throw new RuntimeException("审批请求不存在");
        
        request.setStatus("approved");
        request.setApprovedBy(approverId);
        request.setApprovedAt(System.currentTimeMillis());
        request.setApproverComment(comment);
        
        // 执行审批结果
        executeApprovalResult(request);
        
        log.info("审批通过: requestId={}, approver={}", requestId, approverId);
    }
    
    public void reject(String requestId, String approverId, String reason) {
        ApprovalRequest request = approvalRequests.get(requestId);
        if (request == null) throw new RuntimeException("审批请求不存在");
        
        request.setStatus("rejected");
        request.setApprovedBy(approverId);
        request.setApprovedAt(System.currentTimeMillis());
        request.setApproverComment(reason);
        
        log.info("审批拒绝: requestId={}, approver={}, reason={}", requestId, approverId, reason);
    }
    
    private void notifyApprovers(ApprovalRequest request) {
        // 实际应发送邮件/消息通知
    }
    
    private void executeApprovalResult(ApprovalRequest request) {
        // 根据审批类型执行相应操作
        switch (request.getType()) {
            case "data_permission":
                // 授予数据权限
                break;
            case "map_publish":
                // 发布地图
                break;
            case "user_register":
                // 激活用户
                break;
        }
    }
    
    // ==================== 审计日志 ====================
    
    public void logAudit(AuditLog log) {
        log.setId(UUID.randomUUID().toString());
        log.setTimestamp(System.currentTimeMillis());
        auditLogs.add(log);
        
        // 保留最近10000条
        if (auditLogs.size() > 10000) {
            auditLogs.remove(0);
        }
    }
    
    // ─── API bridge methods ─────────────────────────────────────────────────

    /**
     * Bridge: getStats → delegates to getMetrics()
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStats() {
        SystemMetrics m = getMetrics();
        return new java.util.LinkedHashMap<>() {{
            put("memoryUsage", m.getMemoryUsage());
            put("cpuUsage", m.getCpuUsage());
            put("threadCount", m.getThreadCount());
            put("sessionCount", m.getSessionCount());
            put("timestamp", m.getTimestamp());
        }};
    }

    /**
     * Bridge: getUsageReport(period) → generates a simple usage report
     */
    public Map<String, Object> getUsageReport(String period) {
        return new java.util.LinkedHashMap<>() {{
            put("period", period != null ? period : "all");
            put("totalRequests", auditLogs.size());
            put("totalTasks", scheduledTasks.size());
            put("totalDataSources", dataSources.size());
        }};
    }

    /**
     * Bridge: logAuditEvent(Map) → converts Map to AuditLog and calls logAudit()
     */
    public void logAuditEvent(Map<String, Object> event) {
        AuditLog log = AuditLog.builder()
                .userId((String) event.get("userId"))
                .action((String) event.get("action"))
                .resource((String) event.get("resource"))
                .resourceId((String) event.get("resourceId"))
                .details(event)
                .ipAddress((String) event.get("ipAddress"))
                .userAgent((String) event.get("userAgent"))
                .build();
        logAudit(log);
    }

    /**
     * Bridge: getAuditLogs(userId, action, limit) → delegates to queryAuditLogs
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAuditLogs(String userId, String action, int limit) {
        return queryAuditLogs(userId, action, null, null, limit).stream()
                .map(log -> (Map<String, Object>) new java.util.LinkedHashMap<String, Object>() {{
                    put("id", log.getId());
                    put("userId", log.getUserId());
                    put("action", log.getAction());
                    put("resource", log.getResource());
                    put("resourceId", log.getResourceId());
                    put("timestamp", log.getTimestamp());
                    put("details", log.getDetails());
                }})
                .toList();
    }

    // ─── Original queryAuditLogs ────────────────────────────────────────────

    public List<AuditLog> queryAuditLogs(String userId, String action, Long startTime, Long endTime, int limit) {
        List<AuditLog> results = new ArrayList<>();
        
        for (AuditLog log : auditLogs) {
            if (userId != null && !userId.equals(log.getUserId())) continue;
            if (action != null && !action.equals(log.getAction())) continue;
            if (startTime != null && log.getTimestamp() < startTime) continue;
            if (endTime != null && log.getTimestamp() > endTime) continue;
            
            results.add(log);
            if (limit > 0 && results.size() >= limit) break;
        }
        
        return results;
    }
    
    // ==================== 任务调度 ====================
    
    public String scheduleTask(ScheduledTask task) {
        task.setId(UUID.randomUUID().toString());
        task.setStatus("scheduled");
        task.setNextRunTime(task.getCronExpression() != null ? 
                parseCron(task.getCronExpression()) : task.getStartTime());
        
        scheduledTasks.put(task.getId(), task);
        
        if (task.getCronExpression() != null) {
            scheduler.scheduleAtFixedRate(() -> executeScheduledTask(task),
                    0, parseCronInterval(task.getCronExpression()), TimeUnit.MILLISECONDS);
        } else if (task.getIntervalMs() != null) {
            scheduler.scheduleAtFixedRate(() -> executeScheduledTask(task),
                    0, task.getIntervalMs(), TimeUnit.MILLISECONDS);
        } else {
            scheduler.schedule(() -> executeScheduledTask(task), 
                    task.getStartTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
        
        log.info("调度任务: id={}, name={}", task.getId(), task.getName());
        
        return task.getId();
    }
    
    public void cancelTask(String taskId) {
        ScheduledTask task = scheduledTasks.get(taskId);
        if (task != null) {
            task.setStatus("cancelled");
            log.info("取消任务: id={}", taskId);
        }
    }
    
    private void executeScheduledTask(ScheduledTask task) {
        task.setLastRunTime(System.currentTimeMillis());
        task.setRunCount(task.getRunCount() + 1);
        
        try {
            // 执行任务
            log.info("执行任务: id={}, name={}", task.getId(), task.getName());
            task.setStatus("running");
            // task.getHandler().run();
            task.setStatus("completed");
            task.setLastError(null);
        } catch (Exception e) {
            task.setStatus("failed");
            task.setLastError(e.getMessage());
            log.error("任务执行失败: id={}", task.getId(), e);
        }
    }
    
    private long parseCron(String cron) {
        // 简化实现
        return 60000; // 默认1分钟
    }
    
    private long parseCronInterval(String cron) {
        return 60000;
    }
    
    // ==================== 记忆服务 ====================
    
    public void saveMemory(String userId, MemoryEntry entry) {
        entry.setId(UUID.randomUUID().toString());
        entry.setCreatedAt(System.currentTimeMillis());
        
        List<MemoryEntry> memories = userMemories.computeIfAbsent(userId, k -> new ArrayList<>());
        memories.add(entry);
        
        // 保留最近1000条
        if (memories.size() > 1000) {
            memories.remove(0);
        }
        
        log.debug("保存记忆: userId={}, type={}", userId, entry.getType());
    }
    
    public List<MemoryEntry> getMemories(String userId, String type, int limit) {
        List<MemoryEntry> memories = userMemories.getOrDefault(userId, new ArrayList<>());
        List<MemoryEntry> results = new ArrayList<>();
        
        for (MemoryEntry entry : memories) {
            if (type == null || type.equals(entry.getType())) {
                results.add(entry);
                if (limit > 0 && results.size() >= limit) break;
            }
        }
        
        return results;
    }
    
    public void clearMemories(String userId) {
        userMemories.remove(userId);
        log.info("清除记忆: userId={}", userId);
    }
    
    // ==================== 系统监控 ====================
    
    public SystemMetrics getMetrics() {
        return metrics;
    }
    
    private void startMetricsCollection() {
        scheduler.scheduleAtFixedRate(() -> {
            metrics.setTimestamp(System.currentTimeMillis());
            metrics.setMemoryUsage(Runtime.getRuntime().usedMemory() / (double) Runtime.getRuntime().maxMemory());
            metrics.setCpuUsage(0.5); // 简化
            metrics.setThreadCount(Thread.activeCount());
            metrics.setSessionCount(scheduledTasks.size());
        }, 0, 30, TimeUnit.SECONDS);
    }
    
    // ==================== 数据源管理 ====================
    
    private void initializeDataSources() {
        if (dataSourceProperties == null || dataSourceProperties.getServices() == null) {
            log.warn("DataSourceProperties not configured, using empty data source map");
            return;
        }
        
        for (Map.Entry<String, ServiceEndpoint> entry : dataSourceProperties.getServices().entrySet()) {
            ServiceEndpoint ep = entry.getValue();
            if (ep.isEnabled()) {
                dataSources.put(entry.getKey(), DataSourceConfig.builder()
                        .id(entry.getKey())
                        .name(ep.getName())
                        .type(ep.getType())
                        .url(ep.getUrl())
                        .capabilities(ep.getCapabilities())
                        .build());
            }
        }
    }
    
    public List<DataSourceConfig> getAllDataSources() {
        return new ArrayList<>(dataSources.values());
    }
    
    public DataSourceConfig getDataSource(String id) {
        return dataSources.get(id);
    }
    
    public void testDataSource(String id) {
        DataSourceConfig ds = dataSources.get(id);
        if (ds == null) throw new RuntimeException("数据源不存在");
        // 实际应测试连接
        log.info("测试数据源: id={}, type={}", id, ds.getType());
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class ApprovalRequest {
        private String id;
        private String type; // data_permission, map_publish, user_register, resource_request
        private String applicantId;
        private String targetId;
        private Map<String, Object> params;
        private String status; // pending, approved, rejected
        private String approvedBy;
        private Long approvedAt;
        private String approverComment;
        private Long createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ApprovalWorkflow {
        private String id;
        private String name;
        private String type;
        private List<String> approvers;
        private boolean multiLevel;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AuditLog {
        private String id;
        private String userId;
        private String action;
        private String resource;
        private String resourceId;
        private Map<String, Object> details;
        private String ipAddress;
        private String userAgent;
        private Long timestamp;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ScheduledTask {
        private String id;
        private String name;
        private String type;
        private String cronExpression;
        private Long startTime;
        private Long intervalMs;
        private Map<String, Object> params;
        private String status;
        private Long nextRunTime;
        private Long lastRunTime;
        private Long runCount;
        private String lastError;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MemoryEntry {
        private String id;
        private String type; // conversation, fact, preference, history
        private String content;
        private Map<String, Object> metadata;
        private Long createdAt;
        private Long expiresAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SystemMetrics {
        private Long timestamp;
        private double memoryUsage;
        private double cpuUsage;
        private int threadCount;
        private int sessionCount;
        private Map<String, Double> customMetrics;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class DataSourceConfig {
        private String id;
        private String name;
        private String type; // geoserver, supermap, arcgis, postgis, mysql, mongodb
        private String url;
        private Map<String, String> credentials;
        private List<String> capabilities;
        private boolean enabled;
    }
}

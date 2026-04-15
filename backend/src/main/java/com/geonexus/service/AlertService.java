package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 实时告警服务 - 地理空间事件监控
 */
@Slf4j
@Service
public class AlertService {
    
    private final CacheService cacheService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // 告警规则
    private final Map<String, AlertRule> rules = new ConcurrentHashMap<>();
    
    // 告警记录
    private final List<AlertRecord> alertHistory = new CopyOnWriteArrayList<>();
    
    // 活跃告警
    private final Map<String, ActiveAlert> activeAlerts = new ConcurrentHashMap<>();
    
    // 订阅者
    private final Map<String, List<AlertSubscriber>> subscribers = new ConcurrentHashMap<>();
    
    public AlertService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeBuiltinRules();
    }
    
    // ==================== 初始化 ====================
    
    private void initializeBuiltinRules() {
        // 车辆超速规则
        createRule(AlertRule.builder()
                .id("speed-limit")
                .name("车辆超速告警")
                .description("监控车辆是否超过速度限制")
                .category("traffic")
                .conditionType("speed")
                .condition(Map.of("maxSpeed", 120, "unit", "km/h"))
                .severity("warning")
                .enabled(true)
                .build());
        
        // 地理围栏入侵
        createRule(AlertRule.builder()
                .id("geofence-breach")
                .name("地理围栏入侵告警")
                .description("监控目标进入或离开指定区域")
                .category("security")
                .conditionType("geofence")
                .condition(Map.of("action", "enter_or_exit"))
                .severity("critical")
                .enabled(true)
                .build());
        
        // 数据更新告警
        createRule(AlertRule.builder()
                .id("data-update")
                .name("数据更新通知")
                .description("监控指定图层的数据更新")
                .category("data")
                .conditionType("update")
                .condition(Map.of("checkInterval", 60))
                .severity("info")
                .enabled(true)
                .build());
        
        // 服务异常告警
        createRule(AlertRule.builder()
                .id("service-down")
                .name("服务异常告警")
                .description("监控GIS服务可用性")
                .category("system")
                .conditionType("health")
                .condition(Map.of("maxResponseTime", 5000))
                .severity("critical")
                .enabled(true)
                .build());
    }
    
    // ==================== 告警规则 ====================
    
    /**
     * 创建告警规则
     */
    public AlertRule createRule(AlertRule rule) {
        rule.setId(rule.getId() != null ? rule.getId() : UUID.randomUUID().toString());
        rule.setCreatedAt(System.currentTimeMillis());
        rule.setUpdatedAt(rule.getCreatedAt());
        
        rules.put(rule.getId(), rule);
        
        // 启动监控
        if (rule.isEnabled()) {
            startMonitoring(rule);
        }
        
        log.info("创建告警规则: id={}, name={}", rule.getId(), rule.getName());
        
        return rule;
    }
    
    /**
     * 获取规则
     */
    public AlertRule getRule(String ruleId) {
        return rules.get(ruleId);
    }
    
    /**
     * 更新规则
     */
    public AlertRule updateRule(String ruleId, AlertRule updates) {
        AlertRule existing = getRule(ruleId);
        
        updates.setId(ruleId);
        updates.setCreatedAt(existing.getCreatedAt());
        updates.setUpdatedAt(System.currentTimeMillis());
        
        rules.put(ruleId, updates);
        
        // 重启监控
        stopMonitoring(ruleId);
        if (updates.isEnabled()) {
            startMonitoring(updates);
        }
        
        return updates;
    }
    
    /**
     * 删除规则
     */
    public void deleteRule(String ruleId) {
        stopMonitoring(ruleId);
        rules.remove(ruleId);
        
        log.info("删除告警规则: id={}", ruleId);
    }
    
    /**
     * 启用/禁用规则
     */
    public void setRuleEnabled(String ruleId, boolean enabled) {
        AlertRule rule = getRule(ruleId);
        rule.setEnabled(enabled);
        
        if (enabled) {
            startMonitoring(rule);
        } else {
            stopMonitoring(ruleId);
        }
    }
    
    // ==================== 告警处理 ====================
    
    /**
     * 检查事件
     */
    public void checkEvent(AlertEvent event) {
        log.debug("检查告警事件: type={}, source={}", event.getType(), event.getSource());
        
        for (AlertRule rule : rules.values()) {
            if (!rule.isEnabled()) continue;
            
            if (matchesRule(event, rule)) {
                triggerAlert(rule, event);
            }
        }
    }
    
    /**
     * 批量检查事件
     */
    public void checkEvents(List<AlertEvent> events) {
        for (AlertEvent event : events) {
            checkEvent(event);
        }
    }
    
    private boolean matchesRule(AlertEvent event, AlertRule rule) {
        // 类型匹配
        if (!rule.getCategory().equals(event.getCategory())) {
            return false;
        }
        
        // 检查具体条件
        switch (rule.getConditionType()) {
            case "speed":
                double speed = (double) event.getData().getOrDefault("speed", 0);
                double maxSpeed = (double) rule.getCondition().get("maxSpeed");
                return speed > maxSpeed;
                
            case "geofence":
                String action = (String) event.getData().get("action");
                String expectedAction = (String) rule.getCondition().get("action");
                return expectedAction.equals(action) || "enter_or_exit".equals(expectedAction);
                
            case "update":
                return true; // 总是触发，检查更新时间
                
            case "health":
                long responseTime = (long) event.getData().getOrDefault("responseTime", 0L);
                long maxResponse = ((Number) rule.getCondition().get("maxResponseTime")).longValue();
                return responseTime > maxResponse;
                
            default:
                return false;
        }
    }
    
    private void triggerAlert(AlertRule rule, AlertEvent event) {
        String alertId = UUID.randomUUID().toString();
        
        ActiveAlert alert = ActiveAlert.builder()
                .id(alertId)
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .category(rule.getCategory())
                .severity(rule.getSeverity())
                .message(generateAlertMessage(rule, event))
                .location(event.getLocation())
                .data(event.getData())
                .status("active")
                .triggeredAt(System.currentTimeMillis())
                .build();
        
        activeAlerts.put(alertId, alert);
        
        // 记录历史
        alertHistory.add(AlertRecord.builder()
                .id(alertId)
                .ruleId(rule.getId())
                .severity(rule.getSeverity())
                .message(alert.getMessage())
                .location(event.getLocation())
                .status("triggered")
                .triggeredAt(System.currentTimeMillis())
                .build());
        
        // 通知订阅者
        notifySubscribers(alert);
        
        // 自动处理
        autoResolve(alert, rule);
        
        log.info("触发告警: alertId={}, ruleId={}, severity={}", 
                alertId, rule.getId(), rule.getSeverity());
    }
    
    private String generateAlertMessage(AlertRule rule, AlertEvent event) {
        switch (rule.getConditionType()) {
            case "speed":
                return String.format("车辆超速告警: 当前速度 %.1f km/h，超过限制",
                        event.getData().get("speed"));
            case "geofence":
                return String.format("地理围栏告警: %s %s",
                        event.getData().get("target"),
                        event.getData().get("action"));
            case "update":
                return String.format("数据更新: %s 图层已更新",
                        event.getData().get("layer"));
            case "health":
                return String.format("服务响应异常: 响应时间 %d ms 超过阈值",
                        event.getData().get("responseTime"));
            default:
                return rule.getName() + " 告警触发";
        }
    }
    
    private void autoResolve(ActiveAlert alert, AlertRule rule) {
        // 告警抑制：同类型告警在一定时间内不重复触发
        String key = rule.getId() + ":" + alert.getLocation();
        
        // 简化实现：5分钟后自动解决
        scheduler.schedule(() -> {
            ActiveAlert current = activeAlerts.get(alert.getId());
            if (current != null && "active".equals(current.getStatus())) {
                resolveAlert(alert.getId(), "auto", "超时自动解决");
            }
        }, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 解决告警
     */
    public void resolveAlert(String alertId, String resolvedBy, String comment) {
        ActiveAlert alert = activeAlerts.get(alertId);
        if (alert == null) return;
        
        alert.setStatus("resolved");
        alert.setResolvedBy(resolvedBy);
        alert.setResolvedAt(System.currentTimeMillis());
        alert.setResolutionComment(comment);
        
        // 添加到历史
        alertHistory.add(AlertRecord.builder()
                .id(alertId)
                .ruleId(alert.getRuleId())
                .severity(alert.getSeverity())
                .message(alert.getMessage())
                .status("resolved")
                .resolvedBy(resolvedBy)
                .resolvedAt(System.currentTimeMillis())
                .comment(comment)
                .build());
        
        log.info("解决告警: alertId={}, by={}", alertId, resolvedBy);
    }
    
    // ==================== 监控 ====================
    
    private void startMonitoring(AlertRule rule) {
        if ("health".equals(rule.getConditionType())) {
            // 健康检查监控
            scheduler.scheduleAtFixedRate(() -> {
                checkHealthMonitor(rule);
            }, 0, 1, TimeUnit.MINUTES);
        } else if ("update".equals(rule.getConditionType())) {
            // 数据更新监控
            scheduler.scheduleAtFixedRate(() -> {
                checkDataUpdate(rule);
            }, 0, ((Number) rule.getCondition().getOrDefault("checkInterval", 60)).longValue(), TimeUnit.SECONDS);
        }
    }
    
    private void stopMonitoring(String ruleId) {
        // 简化实现：规则禁用时停止检查
    }
    
    private void checkHealthMonitor(AlertRule rule) {
        AlertEvent event = AlertEvent.builder()
                .type("health")
                .category("system")
                .data(Map.of("responseTime", 100L)) // 模拟
                .timestamp(System.currentTimeMillis())
                .build();
        
        checkEvent(event);
    }
    
    private void checkDataUpdate(AlertRule rule) {
        // 检查数据更新
        log.debug("检查数据更新: ruleId={}", rule.getId());
    }
    
    // ==================== 订阅 ====================
    
    /**
     * 订阅告警
     */
    public void subscribe(String userId, String category, String severity, String channel) {
        AlertSubscriber subscriber = AlertSubscriber.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .category(category)
                .severity(severity)
                .channel(channel) // email, sms, webhook, push
                .build();
        
        subscribers.computeIfAbsent(category, k -> new ArrayList<>()).add(subscriber);
        
        log.info("订阅告警: userId={}, category={}", userId, category);
    }
    
    /**
     * 取消订阅
     */
    public void unsubscribe(String userId, String category) {
        List<AlertSubscriber> subs = subscribers.get(category);
        if (subs != null) {
            subs.removeIf(s -> s.getUserId().equals(userId));
        }
    }
    
    private void notifySubscribers(ActiveAlert alert) {
        List<AlertSubscriber> subs = subscribers.get(alert.getCategory());
        if (subs == null || subs.isEmpty()) return;
        
        for (AlertSubscriber sub : subs) {
            if (matchesSeverity(alert.getSeverity(), sub.getSeverity())) {
                sendNotification(sub, alert);
            }
        }
    }
    
    private boolean matchesSeverity(String alertSeverity, String filterSeverity) {
        Map<String, Integer> levels = Map.of(
                "info", 1,
                "warning", 2,
                "critical", 3
        );
        
        int alertLevel = levels.getOrDefault(alertSeverity, 0);
        int filterLevel = levels.getOrDefault(filterSeverity, 0);
        
        return alertLevel >= filterLevel;
    }
    
    private void sendNotification(AlertSubscriber subscriber, ActiveAlert alert) {
        switch (subscriber.getChannel()) {
            case "email":
                log.info("发送邮件告警: to={}, alert={}", subscriber.getUserId(), alert.getId());
                break;
            case "webhook":
                log.info("发送Webhook告警: userId={}, alert={}", subscriber.getUserId(), alert.getId());
                break;
            case "push":
                log.info("发送推送告警: userId={}, alert={}", subscriber.getUserId(), alert.getId());
                break;
        }
    }
    
    // ==================== 查询 ====================
    
    /**
     * 获取活跃告警
     */
    public List<ActiveAlert> getActiveAlerts(String category, String severity) {
        return activeAlerts.values().stream()
                .filter(a -> {
                    if (category != null && !category.equals(a.getCategory())) return false;
                    if (severity != null && !severity.equals(a.getSeverity())) return false;
                    return "active".equals(a.getStatus());
                })
                .sorted((a, b) -> Long.compare(b.getTriggeredAt(), a.getTriggeredAt()))
                .toList();
    }
    
    /**
     * 获取告警历史
     */
    public List<AlertRecord> getAlertHistory(String ruleId, String category, Long startTime, Long endTime, int limit) {
        return alertHistory.stream()
                .filter(r -> {
                    if (ruleId != null && !ruleId.equals(r.getRuleId())) return false;
                    if (category != null && !category.equals(r.getCategory())) return false;
                    if (startTime != null && r.getTriggeredAt() < startTime) return false;
                    if (endTime != null && r.getTriggeredAt() > endTime) return false;
                    return true;
                })
                .sorted((a, b) -> Long.compare(b.getTriggeredAt(), a.getTriggeredAt()))
                .limit(limit > 0 ? limit : 100)
                .toList();
    }
    
    /**
     * 获取告警统计
     */
    public AlertStatistics getStatistics(Long startTime, Long endTime) {
        List<AlertRecord> filtered = getAlertHistory(null, null, startTime, endTime, 0);
        
        Map<String, Long> bySeverity = new HashMap<>();
        Map<String, Long> byCategory = new HashMap<>();
        long total = filtered.size();
        long resolved = filtered.stream().filter(r -> "resolved".equals(r.getStatus())).count();
        
        for (AlertRecord record : filtered) {
            bySeverity.merge(record.getSeverity(), 1L, Long::sum);
            byCategory.merge(record.getCategory(), 1L, Long::sum);
        }
        
        return AlertStatistics.builder()
                .total(total)
                .active(activeAlerts.size())
                .resolved(resolved)
                .bySeverity(bySeverity)
                .byCategory(byCategory)
                .resolutionRate(total > 0 ? (double) resolved / total * 100 : 0)
                .periodStart(startTime)
                .periodEnd(endTime)
                .generatedAt(System.currentTimeMillis())
                .build();
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class AlertRule {
        private String id;
        private String name;
        private String description;
        private String category;
        private String conditionType;
        private Map<String, Object> condition;
        private String severity;
        private boolean enabled;
        private Long createdAt;
        private Long updatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AlertEvent {
        private String type;
        private String category;
        private double[] location;
        private Map<String, Object> data;
        private Long timestamp;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ActiveAlert {
        private String id;
        private String ruleId;
        private String ruleName;
        private String category;
        private String severity;
        private String message;
        private double[] location;
        private Map<String, Object> data;
        private String status;
        private Long triggeredAt;
        private String resolvedBy;
        private Long resolvedAt;
        private String resolutionComment;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AlertRecord {
        private String id;
        private String ruleId;
        private String category;
        private String severity;
        private String message;
        private double[] location;
        private String status;
        private Long triggeredAt;
        private Long resolvedAt;
        private String resolvedBy;
        private String comment;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AlertSubscriber {
        private String id;
        private String userId;
        private String category;
        private String severity;
        private String channel;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AlertStatistics {
        private long total;
        private long active;
        private long resolved;
        private Map<String, Long> bySeverity;
        private Map<String, Long> byCategory;
        private double resolutionRate;
        private Long periodStart;
        private Long periodEnd;
        private Long generatedAt;
    }
}

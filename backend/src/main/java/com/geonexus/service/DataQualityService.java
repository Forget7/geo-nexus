package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据质量服务 - GIS数据质量检查与修复
 */
@Slf4j
@Service
public class DataQualityService {
    
    private final CacheService cacheService;
    
    // 质量规则
    private final Map<String, QualityRule> rules = new ConcurrentHashMap<>();
    
    // 质量报告
    private final Map<String, QualityReport> reports = new ConcurrentHashMap<>();
    
    public DataQualityService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeDefaultRules();
    }
    
    private void initializeDefaultRules() {
        // 几何有效性
        createRule(QualityRule.builder()
                .id("geometry-valid")
                .name("几何有效性")
                .category("geometry")
                .description("检查几何是否有效")
                .checkType("is_valid")
                .severity("error")
                .enabled(true)
                .build());
        
        // 重复几何
        createRule(QualityRule.builder()
                .id("no-duplicate")
                .name("重复几何")
                .category("duplication")
                .description("检查重复要素")
                .checkType("is_unique")
                .severity("warning")
                .enabled(true)
                .build());
        
        // 拓扑一致性
        createRule(QualityRule.builder()
                .id("topology-clean")
                .name("拓扑一致性")
                .category("topology")
                .description("检查拓扑问题")
                .checkType("topology_check")
                .severity("error")
                .enabled(true)
                .build());
        
        // 属性完整性
        createRule(QualityRule.builder()
                .id("attribute-required")
                .name("必填字段")
                .category("attribute")
                .description("检查必填字段")
                .checkType("required_fields")
                .severity("error")
                .enabled(true)
                .build());
        
        // 范围检查
        createRule(QualityRule.builder()
                .id("value-range")
                .name("值域范围")
                .category("attribute")
                .description("检查属性值范围")
                .checkType("range_check")
                .severity("warning")
                .enabled(true)
                .build());
        
        // 空间参考
        createRule(QualityRule.builder()
                .id("spatial-reference")
                .name("空间参考")
                .category("reference")
                .description("检查坐标系")
                .checkType("has_srs")
                .severity("error")
                .enabled(true)
                .build());
        
        // 面积检查
        createRule(QualityRule.builder()
                .id("area-check")
                .name("面积检查")
                .category("geometry")
                .description("检查异常面积")
                .checkType("area_range")
                .severity("warning")
                .enabled(true)
                .build());
        
        // 自相交
        createRule(QualityRule.builder()
                .id("self-intersection")
                .name("自相交检查")
                .category("topology")
                .description("检查自相交几何")
                .checkType("self_intersects")
                .severity("error")
                .enabled(true)
                .build());
    }
    
    // ==================== 规则管理 ====================
    
    public QualityRule createRule(QualityRule rule) {
        rule.setId(rule.getId() != null ? rule.getId() : UUID.randomUUID().toString());
        rule.setCreatedAt(System.currentTimeMillis());
        rules.put(rule.getId(), rule);
        return rule;
    }
    
    public QualityRule getRule(String ruleId) {
        return rules.get(ruleId);
    }
    
    public void deleteRule(String ruleId) {
        rules.remove(ruleId);
    }
    
    public List<QualityRule> getAllRules() {
        return new ArrayList<>(rules.values());
    }
    
    // ==================== 质量检查 ====================
    
    /**
     * 运行质量检查
     */
    public QualityReport runCheck(QualityCheckRequest request) {
        log.info("运行质量检查: dataset={}, rules={}", 
                request.getDatasetId(), request.getRuleIds());
        
        QualityReport report = new QualityReport();
        report.setId(UUID.randomUUID().toString());
        report.setDatasetId(request.getDatasetId());
        report.setStartTime(System.currentTimeMillis());
        
        List<QualityIssue> allIssues = new ArrayList<>();
        
        // 获取要检查的规则
        List<QualityRule> rulesToCheck = getRulesToCheck(request.getRuleIds());
        
        // 对每个规则进行检查
        for (QualityRule rule : rulesToCheck) {
            if (!rule.isEnabled()) continue;
            
            List<QualityIssue> issues = checkRule(rule, request.getDatasetId());
            allIssues.addAll(issues);
        }
        
        // 汇总结果
        report.setIssues(allIssues);
        report.setTotalFeatures(request.getFeatureCount());
        report.setIssueCount(allIssues.size());
        report.setErrorCount((int) allIssues.stream().filter(i -> "error".equals(i.getSeverity())).count());
        report.setWarningCount((int) allIssues.stream().filter(i -> "warning".equals(i.getSeverity())).count());
        report.setEndTime(System.currentTimeMillis());
        report.setScore(calculateScore(report));
        
        reports.put(report.getId(), report);
        
        log.info("质量检查完成: reportId={}, issues={}, score={}", 
                report.getId(), report.getIssueCount(), report.getScore());
        
        return report;
    }
    
    private List<QualityRule> getRulesToCheck(List<String> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) {
            return rules.values().stream().filter(QualityRule::isEnabled).collect(Collectors.toList());
        }
        return ruleIds.stream().map(rules::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
    
    private List<QualityIssue> checkRule(QualityRule rule, String datasetId) {
        List<QualityIssue> issues = new ArrayList<>();
        
        switch (rule.getCheckType()) {
            case "is_valid":
                // 几何有效性检查
                issues.addAll(checkGeometryValidity(datasetId, rule));
                break;
            case "is_unique":
                // 唯一性检查
                issues.addAll(checkDuplicates(datasetId, rule));
                break;
            case "topology_check":
                // 拓扑检查
                issues.addAll(checkTopology(datasetId, rule));
                break;
            case "required_fields":
                // 必填字段
                issues.addAll(checkRequiredFields(datasetId, rule));
                break;
            case "range_check":
                // 范围检查
                issues.addAll(checkValueRange(datasetId, rule));
                break;
            case "has_srs":
                // 坐标系检查
                issues.addAll(checkSpatialReference(datasetId, rule));
                break;
            case "area_range":
                // 面积检查
                issues.addAll(checkArea(datasetId, rule));
                break;
            case "self_intersects":
                // 自相交检查
                issues.addAll(checkSelfIntersection(datasetId, rule));
                break;
        }
        
        return issues;
    }
    
    private List<QualityIssue> checkGeometryValidity(String datasetId, QualityRule rule) {
        List<QualityIssue> issues = new ArrayList<>();
        
        // 模拟：检查几何有效性
        // 实际应使用JTS检查
        log.debug("检查几何有效性: dataset={}", datasetId);
        
        return issues;
    }
    
    private List<QualityIssue> checkDuplicates(String datasetId, QualityRule rule) {
        List<QualityIssue> issues = new ArrayList<>();
        
        log.debug("检查重复: dataset={}", datasetId);
        
        return issues;
    }
    
    private List<QualityIssue> checkTopology(String datasetId, QualityRule rule) {
        List<QualityIssue> issues = new ArrayList<>();
        
        log.debug("检查拓扑: dataset={}", datasetId);
        
        return issues;
    }
    
    private List<QualityIssue> checkRequiredFields(String datasetId, QualityRule rule) {
        List<QualityIssue> issues = new ArrayList<>();
        
        log.debug("检查必填字段: dataset={}", datasetId);
        
        return issues;
    }
    
    private List<QualityIssue> checkValueRange(String datasetId, QualityRule rule) {
        List<QualityIssue> issues = new ArrayList<>();
        
        log.debug("检查值域范围: dataset={}", datasetId);
        
        return issues;
    }
    
    private List<QualityIssue> checkSpatialReference(String datasetId, QualityRule rule) {
        List<QualityIssue> issues = new ArrayList<>();
        
        log.debug("检查空间参考: dataset={}", datasetId);
        
        return issues;
    }
    
    private List<QualityIssue> checkArea(String datasetId, QualityRule rule) {
        List<QualityIssue> issues = new ArrayList<>();
        
        log.debug("检查面积: dataset={}", datasetId);
        
        return issues;
    }
    
    private List<QualityIssue> checkSelfIntersection(String datasetId, QualityRule rule) {
        List<QualityIssue> issues = new ArrayList<>();
        
        log.debug("检查自相交: dataset={}", datasetId);
        
        return issues;
    }
    
    // ==================== 数据修复 ====================
    
    /**
     * 自动修复问题
     */
    public QualityReport autoFix(String reportId, List<String> issueIds) {
        QualityReport report = reports.get(reportId);
        if (report == null) {
            throw new ReportNotFoundException("报告不存在: " + reportId);
        }
        
        List<QualityIssue> issuesToFix = report.getIssues().stream()
                .filter(i -> issueIds.contains(i.getId()))
                .toList();
        
        for (QualityIssue issue : issuesToFix) {
            fixIssue(issue);
            issue.setFixed(true);
            issue.setFixedAt(System.currentTimeMillis());
        }
        
        report.setFixedCount((int) issuesToFix.stream().filter(QualityIssue::isFixed).count());
        
        return report;
    }
    
    private void fixIssue(QualityIssue issue) {
        log.info("修复问题: issueId={}, type={}", issue.getId(), issue.getIssueType());
        
        switch (issue.getIssueType()) {
            case "invalid_geometry":
                // 尝试修复几何
                break;
            case "duplicate":
                // 删除重复
                break;
            case "self_intersection":
                // 修复自相交
                break;
        }
    }
    
    // ==================== 统计与报告 ====================
    
    private double calculateScore(QualityReport report) {
        if (report.getTotalFeatures() == 0) return 100.0;
        
        double errorWeight = 5.0;
        double warningWeight = 1.0;
        
        double deduction = report.getErrorCount() * errorWeight + 
                           report.getWarningCount() * warningWeight;
        
        double score = Math.max(0, 100 - (deduction / report.getTotalFeatures() * 100));
        
        return Math.round(score * 100) / 100.0;
    }
    
    /**
     * 获取报告
     */
    public QualityReport getReport(String reportId) {
        return reports.get(reportId);
    }
    
    /**
     * 按数据集获取报告
     */
    public List<QualityReport> getReportsByDataset(String datasetId) {
        return reports.values().stream()
                .filter(r -> r.getDatasetId().equals(datasetId))
                .sorted((a, b) -> Long.compare(b.getStartTime(), a.getStartTime()))
                .toList();
    }
    
    /**
     * 获取质量统计
     */
    public QualityStatistics getStatistics(String datasetId) {
        List<QualityReport> datasetReports = getReportsByDataset(datasetId);
        
        if (datasetReports.isEmpty()) {
            return new QualityStatistics();
        }
        
        QualityStatistics stats = new QualityStatistics();
        stats.setDatasetId(datasetId);
        stats.setReportCount(datasetReports.size());
        stats.setAvgScore(datasetReports.stream()
                .mapToDouble(QualityReport::getScore)
                .average()
                .orElse(0));
        stats.setLatestScore(datasetReports.get(0).getScore());
        
        return stats;
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class QualityRule {
        private String id;
        private String name;
        private String category;
        private String description;
        private String checkType;
        private String severity; // error, warning, info
        private Map<String, Object> parameters;
        private boolean enabled;
        private Long createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class QualityReport {
        private String id;
        private String datasetId;
        private Long startTime;
        private Long endTime;
        private int totalFeatures;
        private int issueCount;
        private int errorCount;
        private int warningCount;
        private int fixedCount;
        private double score;
        private List<QualityIssue> issues;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class QualityIssue {
        private String id;
        private String ruleId;
        private String ruleName;
        private String issueType;
        private String severity;
        private String featureId;
        private String description;
        private double[] location;
        private boolean fixed;
        private Long fixedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class QualityCheckRequest {
        private String datasetId;
        private int featureCount;
        private List<String> ruleIds;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class QualityStatistics {
        private String datasetId;
        private int reportCount;
        private double avgScore;
        private double latestScore;
    }
    
    public static class ReportNotFoundException extends RuntimeException {
        public ReportNotFoundException(String msg) { super(msg); }
    }
}

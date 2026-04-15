package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.*;

/**
 * 报表生成服务 - 自动生成地图报表 (PDF/HTML/PNG)
 */
@Slf4j
@Service
public class ReportService {
    
    private final CacheService cacheService;
    
    // 报表模板
    private final Map<String, ReportTemplate> templates = new ConcurrentHashMap<>();
    
    // 生成的报表
    private final Map<String, GeneratedReport> reports = new ConcurrentHashMap<>();
    
    public ReportService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeDefaultTemplates();
    }
    
    // ==================== 初始化 ====================
    
    private void initializeDefaultTemplates() {
        // 地图概览报表
        templates.put("map-overview", ReportTemplate.builder()
                .id("map-overview")
                .name("地图概览报表")
                .description("展示地图全貌、图层信息、图例")
                .category("map")
                .elements(List.of(
                        ReportElement.builder()
                                .type("title")
                                .content("地图概览报告")
                                .style(Map.of("fontSize", 24, "bold", true))
                                .build(),
                        ReportElement.builder()
                                .type("map")
                                .content("${mapSnapshot}")
                                .style(Map.of("width", "100%", "height", 400))
                                .build(),
                        ReportElement.builder()
                                .type("legend")
                                .content("${legend}")
                                .build(),
                        ReportElement.builder()
                                .type("layers")
                                .content("${layerList}")
                                .build(),
                        ReportElement.builder()
                                .type("metadata")
                                .content("${metadata}")
                                .build()
                ))
                .build());
        
        // 数据分析报表
        templates.put("data-analysis", ReportTemplate.builder()
                .id("data-analysis")
                .name("数据分析报表")
                .description("包含统计图表、数据表格、空间分析结果")
                .category("analysis")
                .elements(List.of(
                        ReportElement.builder()
                                .type("title")
                                .content("数据分析报告")
                                .build(),
                        ReportElement.builder()
                                .type("chart")
                                .content("${charts}")
                                .style(Map.of("width", "100%", "height", 300))
                                .build(),
                        ReportElement.builder()
                                .type("table")
                                .content("${dataTable}")
                                .build(),
                        ReportElement.builder()
                                .type("summary")
                                .content("${summary}")
                                .build()
                ))
                .build());
        
        // 空间分析报表
        templates.put("spatial-analysis", ReportTemplate.builder()
                .id("spatial-analysis")
                .name("空间分析报表")
                .description("展示缓冲区、叠加分析、网络分析结果")
                .category("analysis")
                .elements(List.of(
                        ReportElement.builder()
                                .type("title")
                                .content("空间分析报告")
                                .build(),
                        ReportElement.builder()
                                .type("map")
                                .content("${analysisResultMap}")
                                .build(),
                        ReportElement.builder()
                                .type("description")
                                .content("${analysisDescription}")
                                .build(),
                        ReportElement.builder()
                                .type("metrics")
                                .content("${analysisMetrics}")
                                .build()
                ))
                .build());
        
        // 专题地图报表
        templates.put("thematic-map", ReportTemplate.builder()
                .id("thematic-map")
                .name("专题地图报表")
                .description("展示分级色彩图、分级符号图")
                .category("thematic")
                .elements(List.of(
                        ReportElement.builder()
                                .type("title")
                                .content("${thematicTitle}")
                                .build(),
                        ReportElement.builder()
                                .type("subtitle")
                                .content("${thematicSubtitle}")
                                .build(),
                        ReportElement.builder()
                                .type("map")
                                .content("${thematicMap}")
                                .style(Map.of("width", "100%", "height", 450))
                                .build(),
                        ReportElement.builder()
                                .type("legend")
                                .content("${thematicLegend}")
                                .build(),
                        ReportElement.builder()
                                .type("notes")
                                .content("${dataNotes}")
                                .build()
                ))
                .build());
        
        // 比较分析报表
        templates.put("comparison", ReportTemplate.builder()
                .id("comparison")
                .name("比较分析报表")
                .description("对比分析多个地图或数据集")
                .category("analysis")
                .elements(List.of(
                        ReportElement.builder()
                                .type("title")
                                .content("比较分析报告")
                                .build(),
                        ReportElement.builder()
                                .type("comparison")
                                .content("${comparisonMaps}")
                                .build(),
                        ReportElement.builder()
                                .type("table")
                                .content("${comparisonTable}")
                                .build()
                ))
                .build());
    }
    
    // ==================== 报表生成 ====================
    
    /**
     * 生成报表
     */
    public GeneratedReport generateReport(ReportRequest request) {
        ReportTemplate template = templates.get(request.getTemplateId());
        if (template == null) {
            throw new TemplateNotFoundException("模板不存在: " + request.getTemplateId());
        }
        
        GeneratedReport report = GeneratedReport.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName() != null ? request.getName() : template.getName())
                .templateId(template.getId())
                .templateName(template.getName())
                .format(request.getFormat())
                .status("generating")
                .createdAt(System.currentTimeMillis())
                .createdBy(request.getUserId())
                .parameters(request.getParameters())
                .build();
        
        reports.put(report.getId(), report);
        
        // 异步生成报表
        try {
            generateReportAsync(report, template, request);
            report.setStatus("completed");
            report.setCompletedAt(System.currentTimeMillis());
        } catch (Exception e) {
            report.setStatus("failed");
            report.setError(e.getMessage());
            log.error("报表生成失败: id={}", report.getId(), e);
        }
        
        return report;
    }
    
    private void generateReportAsync(GeneratedReport report, 
            ReportTemplate template, ReportRequest request) {
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>").append(report.getName()).append("</title>\n");
        html.append(getDefaultCSS());
        html.append("</head>\n<body>\n");
        
        // 生成标题
        html.append("<header class='report-header'>\n");
        html.append("<h1>").append(report.getName()).append("</h1>\n");
        html.append("<div class='report-meta'>\n");
        html.append("<span>生成时间: ").append(new Date()).append("</span>\n");
        html.append("<span>模板: ").append(template.getName()).append("</span>\n");
        html.append("</div>\n");
        html.append("</header>\n");
        
        // 生成各个元素
        for (ReportElement element : template.getElements()) {
            html.append(generateElement(element, request.getParameters()));
        }
        
        html.append("<footer class='report-footer'>\n");
        html.append("<p>由 GeoNexus GIS专家系统 生成</p>\n");
        html.append("</footer>\n");
        
        html.append("</body>\n</html>");
        
        report.setContent(html.toString());
        
        // 根据格式转换
        if ("pdf".equals(report.getFormat())) {
            // PDF转换需要额外库支持，这里生成HTML
            report.setContentType("text/html");
        } else if ("png".equals(report.getFormat())) {
            // 图片格式
            report.setContentType("image/png");
        } else {
            report.setContentType("text/html");
        }
        
        log.info("生成报表: id={}, format={}", report.getId(), report.getFormat());
    }
    
    private String generateElement(ReportElement element, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        String content = element.getContent();
        
        // 替换变量
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                content = content.replace("${" + entry.getKey() + "}", 
                        String.valueOf(entry.getValue()));
            }
        }
        
        sb.append("<div class='element element-").append(element.getType()).append("'>\n");
        
        switch (element.getType()) {
            case "title":
                sb.append("<h1 class='element-title'>").append(content).append("</h1>\n");
                break;
            case "subtitle":
                sb.append("<h2 class='element-subtitle'>").append(content).append("</h2>\n");
                break;
            case "map":
                sb.append("<div class='element-map'>\n");
                if (content.startsWith("${")) {
                    sb.append("<img src='data:image/png;base64,").append(content).append("' />\n");
                } else {
                    sb.append(content);
                }
                sb.append("</div>\n");
                break;
            case "chart":
                sb.append("<div class='element-chart'>\n");
                sb.append(content);
                sb.append("</div>\n");
                break;
            case "table":
                sb.append("<div class='element-table'>\n");
                sb.append(content);
                sb.append("</div>\n");
                break;
            case "legend":
                sb.append("<div class='element-legend'>\n");
                sb.append("<h3>图例</h3>\n");
                sb.append(content);
                sb.append("</div>\n");
                break;
            case "layers":
                sb.append("<div class='element-layers'>\n");
                sb.append("<h3>图层列表</h3>\n");
                sb.append(content);
                sb.append("</div>\n");
                break;
            case "metadata":
                sb.append("<div class='element-metadata'>\n");
                sb.append("<h3>元数据</h3>\n");
                sb.append(content);
                sb.append("</div>\n");
                break;
            case "summary":
                sb.append("<div class='element-summary'>\n");
                sb.append("<h3>统计摘要</h3>\n");
                sb.append(content);
                sb.append("</div>\n");
                break;
            case "notes":
                sb.append("<div class='element-notes'>\n");
                sb.append("<h3>说明</h3>\n");
                sb.append(content);
                sb.append("</div>\n");
                break;
            default:
                sb.append("<div class='element-generic'>\n");
                sb.append(content);
                sb.append("</div>\n");
        }
        
        sb.append("</div>\n");
        
        return sb.toString();
    }
    
    private String getDefaultCSS() {
        return """
            <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { font-family: 'Segoe UI', Arial, sans-serif; color: #333; line-height: 1.6; }
            .report-header { background: linear-gradient(135deg, #1e3a5f 0%, #2563eb 100%); color: white; padding: 2rem; text-align: center; }
            .report-header h1 { font-size: 2rem; margin-bottom: 0.5rem; }
            .report-meta { font-size: 0.875rem; opacity: 0.9; }
            .report-meta span { margin: 0 1rem; }
            .element { padding: 1.5rem; border-bottom: 1px solid #eee; }
            .element-title { color: #1e3a5f; font-size: 1.75rem; margin-bottom: 1rem; }
            .element-subtitle { color: #475569; font-size: 1.25rem; margin-bottom: 0.75rem; }
            .element-map { text-align: center; margin: 1rem 0; }
            .element-map img { max-width: 100%; border: 1px solid #ddd; border-radius: 8px; }
            .element-chart { background: #f8fafc; border-radius: 8px; padding: 1rem; }
            .element-table { overflow-x: auto; }
            .element-table table { width: 100%; border-collapse: collapse; }
            .element-table th, .element-table td { padding: 0.75rem; text-align: left; border-bottom: 1px solid #e2e8f0; }
            .element-table th { background: #f1f5f9; font-weight: 600; }
            .element-legend, .element-layers, .element-metadata, .element-summary { background: #f8fafc; border-radius: 8px; padding: 1rem; }
            .element-legend h3, .element-layers h3, .element-metadata h3, .element-summary h3 { color: #1e3a5f; margin-bottom: 0.75rem; font-size: 1rem; }
            .report-footer { background: #1e293b; color: #94a3b8; text-align: center; padding: 1.5rem; font-size: 0.875rem; }
            </style>
            """;
    }
    
    /**
     * 获取报表
     */
    public GeneratedReport getReport(String reportId) {
        GeneratedReport report = reports.get(reportId);
        if (report == null) {
            throw new ReportNotFoundException("报表不存在: " + reportId);
        }
        return report;
    }
    
    /**
     * 下载报表
     */
    public byte[] downloadReport(String reportId) {
        GeneratedReport report = getReport(reportId);
        
        if ("html".equals(report.getFormat())) {
            return report.getContent().getBytes();
        } else if ("pdf".equals(report.getFormat())) {
            // 实际应转换为PDF
            return report.getContent().getBytes();
        }
        
        return report.getContent().getBytes();
    }
    
    /**
     * 获取报表模板列表
     */
    public List<ReportTemplate> getTemplates(String category) {
        if (category != null) {
            return templates.values().stream()
                    .filter(t -> category.equals(t.getCategory()))
                    .toList();
        }
        return new ArrayList<>(templates.values());
    }
    
    /**
     * 创建自定义模板
     */
    public ReportTemplate createTemplate(ReportTemplate template) {
        template.setId(UUID.randomUUID().toString());
        template.setBuiltin(false);
        templates.put(template.getId(), template);
        log.info("创建报表模板: id={}, name={}", template.getId(), template.getName());
        return template;
    }
    
    /**
     * 批量生成报表
     */
    public List<GeneratedReport> batchGenerate(BatchReportRequest request) {
        List<GeneratedReport> results = new ArrayList<>();
        
        for (Map<String, Object> params : request.getItems()) {
            ReportRequest req = ReportRequest.builder()
                    .templateId(request.getTemplateId())
                    .name(request.getName())
                    .format(request.getFormat())
                    .userId(request.getUserId())
                    .parameters(params)
                    .build();
            
            results.add(generateReport(req));
        }
        
        return results;
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class ReportTemplate {
        private String id;
        private String name;
        private String description;
        private String category;
        private List<ReportElement> elements;
        private boolean builtin;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ReportElement {
        private String type; // title, subtitle, map, chart, table, legend, layers, metadata, summary, notes
        private String content;
        private Map<String, Object> style;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class GeneratedReport {
        private String id;
        private String name;
        private String templateId;
        private String templateName;
        private String format;
        private String status;
        private String content;
        private String contentType;
        private Long createdAt;
        private Long completedAt;
        private String createdBy;
        private String error;
        private Map<String, Object> parameters;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ReportRequest {
        private String templateId;
        private String name;
        private String format; // html, pdf, png
        private String userId;
        private Map<String, Object> parameters;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class BatchReportRequest {
        private String templateId;
        private String name;
        private String format;
        private String userId;
        private List<Map<String, Object>> items;
    }
    
    public static class TemplateNotFoundException extends RuntimeException {
        public TemplateNotFoundException(String msg) { super(msg); }
    }
    
    public static class ReportNotFoundException extends RuntimeException {
        public ReportNotFoundException(String msg) { super(msg); }
    }
}

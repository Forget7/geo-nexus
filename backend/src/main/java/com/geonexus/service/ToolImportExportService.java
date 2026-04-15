package com.geonexus.service;

import com.geonexus.domain.tool.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 工具导入导出服务
 */
@Slf4j
@Service
public class ToolImportExportService {

    private final ToolRegistryService toolRegistryService;

    public ToolImportExportService(ToolRegistryService toolRegistryService) {
        this.toolRegistryService = toolRegistryService;
    }

    /**
     * 从URL导入工具
     */
    public ToolDefinition importFromUrl(String url, String format) {
        try {
            // 获取远程工具定义
            String content = fetchRemoteContent(url);

            if ("openapi".equalsIgnoreCase(format) || "yaml".equalsIgnoreCase(format)) {
                return parseOpenAPITool(content);
            } else if ("json".equalsIgnoreCase(format)) {
                return parseJsonTool(content);
            } else {
                throw new IllegalArgumentException("不支持的格式: " + format);
            }

        } catch (Exception e) {
            log.error("从URL导入工具失败: {}", url, e);
            throw new ToolImportException("导入失败: " + e.getMessage());
        }
    }

    /**
     * 导入工具配置
     */
    public ToolDefinition importTool(String content, String format) {
        try {
            ToolDefinition tool;

            if ("json".equalsIgnoreCase(format)) {
                tool = parseJsonTool(content);
            } else if ("openapi".equalsIgnoreCase(format) || "yaml".equalsIgnoreCase(format)) {
                tool = parseOpenAPITool(content);
            } else {
                throw new IllegalArgumentException("不支持的格式: " + format);
            }

            return toolRegistryService.registerTool(tool);

        } catch (Exception e) {
            log.error("导入工具失败", e);
            throw new ToolImportException("导入失败: " + e.getMessage());
        }
    }

    /**
     * 导出工具
     */
    public String exportTool(String toolId, String format) {
        ToolDefinition tool = toolRegistryService.getTool(toolId);

        if ("json".equalsIgnoreCase(format)) {
            return toJson(tool);
        } else if ("openapi".equalsIgnoreCase(format) || "yaml".equalsIgnoreCase(format)) {
            return toOpenAPI(tool);
        } else {
            throw new IllegalArgumentException("不支持的格式: " + format);
        }
    }

    private String fetchRemoteContent(String url) {
        // 简化实现
        return "";
    }

    private ToolDefinition parseJsonTool(String content) {
        try {
            Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    content, Map.class);

            return ToolDefinition.builder()
                    .id((String) map.get("id"))
                    .name((String) map.get("name"))
                    .description((String) map.get("description"))
                    .category((String) map.getOrDefault("category", "custom"))
                    .type("external")
                    .apiConfig((Map<String, Object>) map.get("api"))
                    .capabilities((List<String>) map.get("capabilities"))
                    .build();
        } catch (Exception e) {
            throw new ToolImportException("解析JSON工具失败: " + e.getMessage());
        }
    }

    private ToolDefinition parseOpenAPITool(String content) {
        // 解析OpenAPI规范为工具定义
        // 简化实现
        return ToolDefinition.builder()
                .name("Imported API Tool")
                .category("custom")
                .type("external")
                .build();
    }

    private String toJson(ToolDefinition tool) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(tool);
        } catch (Exception e) {
            throw new RuntimeException("序列化失败", e);
        }
    }

    private String toOpenAPI(ToolDefinition tool) {
        // 转换为OpenAPI规范
        return "# OpenAPI Specification\n\nTool: " + tool.getName();
    }

    // ==================== 异常类 ====================

    public static class ToolImportException extends RuntimeException {
        public ToolImportException(String message) { super(message); }
    }
}

package com.geonexus.domain.tool;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * 工具定义
 */
@Data
@Builder
public class ToolDefinition {
    private String id;
    private String name;
    private String description;
    private String category; // map, analysis, data, ai, external, custom
    private String type; // builtin, external
    private String icon;
    private List<String> capabilities;
    private List<String> requiredConfigs;
    private Map<String, Object> apiConfig;
    private List<String> tags;
    private Long registeredAt;
    private Long updatedAt;
}

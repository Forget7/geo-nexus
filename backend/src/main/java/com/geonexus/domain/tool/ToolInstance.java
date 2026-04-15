package com.geonexus.domain.tool;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

/**
 * 用户工具实例
 */
@Data
@Builder
public class ToolInstance {
    private String id;
    private String toolId;
    private String toolName;
    private String userId;
    private Map<String, Object> config;
    private boolean enabled;
    private Long createdAt;
    private Long updatedAt;
    private Long lastUsedAt;
}

package com.geonexus.domain.tool;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

/**
 * 工具执行结果
 */
@Data
@Builder
public class ToolExecutionResult {
    private String executionId;
    private String toolId;
    private String instanceId;
    private boolean success;
    private Long startTime;
    private Long endTime;
    private Long durationMs;
    private Map<String, Object> params;
    private Map<String, Object> output;
    private String error;
}

package com.geonexus.service;

import com.geonexus.domain.tool.ToolDefinition;
import com.geonexus.domain.tool.ToolExecutionResult;
import com.geonexus.domain.tool.ToolInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * 工具执行服务
 */
@Slf4j
@Service
public class ToolExecutionService {

    private final ToolRegistryService toolRegistryService;
    private final UserToolInstanceService userToolInstanceService;

    public ToolExecutionService(ToolRegistryService toolRegistryService,
                                 UserToolInstanceService userToolInstanceService) {
        this.toolRegistryService = toolRegistryService;
        this.userToolInstanceService = userToolInstanceService;
    }

    /**
     * 执行工具
     */
    public ToolExecutionResult executeTool(String instanceId, Map<String, Object> params) {
        ToolInstance instance = userToolInstanceService.findInstanceById(instanceId);

        if (instance == null) {
            throw new UserToolInstanceService.ToolInstanceNotFoundException("工具实例不存在: " + instanceId);
        }

        ToolDefinition tool = toolRegistryService.getTool(instance.getToolId());

        ToolExecutionResult result = ToolExecutionResult.builder()
                .executionId(UUID.randomUUID().toString())
                .toolId(tool.getId())
                .instanceId(instanceId)
                .success(true)
                .startTime(System.currentTimeMillis())
                .params(params)
                .build();

        try {
            // 根据工具类型执行
            if ("builtin".equals(tool.getType())) {
                result.setOutput(executeBuiltinTool(tool, params));
            } else if ("external".equals(tool.getType())) {
                result.setOutput(executeExternalTool(tool, instance.getConfig(), params));
            }

            result.setSuccess(true);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        }

        result.setEndTime(System.currentTimeMillis());
        result.setDurationMs(result.getEndTime() - result.getStartTime());

        return result;
    }

    private Map<String, Object> executeBuiltinTool(ToolDefinition tool, Map<String, Object> params) {
        // 执行内置工具
        return Map.of("result", "Builtin tool executed: " + tool.getName());
    }

    private Map<String, Object> executeExternalTool(ToolDefinition tool,
            Map<String, Object> config, Map<String, Object> params) {
        // 执行外部工具API
        // 简化实现
        return Map.of("result", "External tool executed: " + tool.getName());
    }
}

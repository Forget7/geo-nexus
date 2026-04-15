package com.geonexus.service;

import com.geonexus.domain.tool.ToolDefinition;
import com.geonexus.domain.tool.ToolInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户工具实例管理服务
 */
@Slf4j
@Service
public class UserToolInstanceService {

    private final CacheService cacheService;
    private final ToolRegistryService toolRegistryService;
    private final Map<String, ToolInstance> userTools = new ConcurrentHashMap<>();

    private static final String USER_TOOL_PREFIX = "tool:user:";

    public UserToolInstanceService(CacheService cacheService, ToolRegistryService toolRegistryService) {
        this.cacheService = cacheService;
        this.toolRegistryService = toolRegistryService;
    }

    /**
     * 为用户创建工具实例
     */
    public ToolInstance createUserToolInstance(String userId, String toolId, Map<String, Object> config) {
        ToolDefinition tool = toolRegistryService.getTool(toolId);

        // 验证必需的配置
        if (tool.getRequiredConfigs() != null) {
            for (String required : tool.getRequiredConfigs()) {
                if (!config.containsKey(required)) {
                    throw new IllegalArgumentException("缺少必需配置: " + required);
                }
            }
        }

        ToolInstance instance = ToolInstance.builder()
                .id(UUID.randomUUID().toString())
                .toolId(toolId)
                .toolName(tool.getName())
                .userId(userId)
                .config(config)
                .enabled(true)
                .createdAt(System.currentTimeMillis())
                .build();

        String key = USER_TOOL_PREFIX + userId + ":" + instance.getId();
        cacheService.set(key, instance);

        userTools.put(key, instance);

        log.info("创建用户工具实例: userId={}, toolId={}, instanceId={}",
                userId, toolId, instance.getId());

        return instance;
    }

    /**
     * 获取用户的工具实例
     */
    public List<ToolInstance> getUserToolInstances(String userId) {
        List<ToolInstance> instances = new ArrayList<>();

        for (ToolInstance instance : userTools.values()) {
            if (instance.getUserId().equals(userId)) {
                instances.add(instance);
            }
        }

        return instances;
    }

    /**
     * 更新工具实例配置
     */
    public ToolInstance updateToolInstance(String instanceId, Map<String, Object> config) {
        ToolInstance instance = findInstanceById(instanceId);

        if (instance == null) {
            throw new ToolInstanceNotFoundException("工具实例不存在: " + instanceId);
        }

        instance.setConfig(config);
        instance.setUpdatedAt(System.currentTimeMillis());

        String instanceKey = findInstanceKey(instanceId);
        if (instanceKey != null) {
            cacheService.set(instanceKey, instance);
        }

        return instance;
    }

    /**
     * 删除工具实例
     */
    public void deleteToolInstance(String instanceId) {
        String instanceKey = findInstanceKey(instanceId);

        if (instanceKey != null) {
            userTools.remove(instanceKey);
            cacheService.delete(instanceKey);
            log.info("删除工具实例: id={}", instanceId);
        }
    }

    /**
     * 根据实例ID查找实例
     */
    public ToolInstance findInstanceById(String instanceId) {
        for (ToolInstance si : userTools.values()) {
            if (si.getId().equals(instanceId)) {
                return si;
            }
        }
        return null;
    }

    private String findInstanceKey(String instanceId) {
        for (Map.Entry<String, ToolInstance> entry : userTools.entrySet()) {
            if (entry.getValue().getId().equals(instanceId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // ==================== 异常类 ====================

    public static class ToolInstanceNotFoundException extends RuntimeException {
        public ToolInstanceNotFoundException(String message) { super(message); }
    }
}

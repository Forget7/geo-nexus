package com.geonexus.service;

import com.geonexus.domain.tool.ToolDefinition;
import com.geonexus.domain.tool.ToolExecutionResult;
import com.geonexus.domain.tool.ToolInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 工具管理服务 (Facade)
 * 
 * 职责：委托给各专责服务，保持向后兼容
 * - ToolRegistryService: 工具注册与发现
 * - ToolExecutionService: 工具执行
 * - ToolImportExportService: 工具导入导出
 * - UserToolInstanceService: 用户工具实例管理
 */
@Slf4j
@Service
public class ToolManagementService {

    private final ToolRegistryService toolRegistryService;
    private final ToolExecutionService toolExecutionService;
    private final ToolImportExportService toolImportExportService;
    private final UserToolInstanceService userToolInstanceService;

    public ToolManagementService(ToolRegistryService toolRegistryService,
                                  ToolExecutionService toolExecutionService,
                                  ToolImportExportService toolImportExportService,
                                  UserToolInstanceService userToolInstanceService) {
        this.toolRegistryService = toolRegistryService;
        this.toolExecutionService = toolExecutionService;
        this.toolImportExportService = toolImportExportService;
        this.userToolInstanceService = userToolInstanceService;
    }

    // ==================== 工具注册与发现 (委托给 ToolRegistryService) ====================

    public ToolDefinition registerTool(ToolDefinition tool) {
        return toolRegistryService.registerTool(tool);
    }

    public ToolDefinition getTool(String toolId) {
        return toolRegistryService.getTool(toolId);
    }

    public List<ToolDefinition> getAllTools() {
        return toolRegistryService.getAllTools();
    }

    public List<ToolDefinition> getToolsByCategory(String category) {
        return toolRegistryService.getToolsByCategory(category);
    }

    public List<ToolDefinition> searchTools(String keyword) {
        return toolRegistryService.searchTools(keyword);
    }

    public ToolDefinition updateTool(String toolId, ToolDefinition tool) {
        return toolRegistryService.updateTool(toolId, tool);
    }

    public void unregisterTool(String toolId) {
        toolRegistryService.unregisterTool(toolId);
    }

    // ==================== 工具导入导出 (委托给 ToolImportExportService) ====================

    public ToolDefinition importFromUrl(String url, String format) {
        return toolImportExportService.importFromUrl(url, format);
    }

    public ToolDefinition importTool(String content, String format) {
        return toolImportExportService.importTool(content, format);
    }

    public String exportTool(String toolId, String format) {
        return toolImportExportService.exportTool(toolId, format);
    }

    // ==================== 用户工具实例 (委托给 UserToolInstanceService) ====================

    public ToolInstance createUserToolInstance(String userId, String toolId, Map<String, Object> config) {
        return userToolInstanceService.createUserToolInstance(userId, toolId, config);
    }

    public List<ToolInstance> getUserToolInstances(String userId) {
        return userToolInstanceService.getUserToolInstances(userId);
    }

    public ToolInstance updateToolInstance(String instanceId, Map<String, Object> config) {
        return userToolInstanceService.updateToolInstance(instanceId, config);
    }

    public void deleteToolInstance(String instanceId) {
        userToolInstanceService.deleteToolInstance(instanceId);
    }

    // ==================== 工具执行 (委托给 ToolExecutionService) ====================

    public ToolExecutionResult executeTool(String instanceId, Map<String, Object> params) {
        return toolExecutionService.executeTool(instanceId, params);
    }

    // ==================== 异常类 (保留向后兼容) ====================

    public static class ToolNotFoundException extends RuntimeException {
        public ToolNotFoundException(String message) { super(message); }
    }

    public static class ToolImportException extends RuntimeException {
        public ToolImportException(String message) { super(message); }
    }
}

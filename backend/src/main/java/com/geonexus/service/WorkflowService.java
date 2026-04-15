package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 工作流自动化服务 - 可视化流程设计器
 */
@Slf4j
@Service
public class WorkflowService {
    
    private final CacheService cacheService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    
    // 工作流定义
    private final Map<String, WorkflowDefinition> workflows = new ConcurrentHashMap<>();
    
    // 工作流实例
    private final Map<String, WorkflowInstance> instances = new ConcurrentHashMap<>();
    
    // 触发器
    private final Map<String, WorkflowTrigger> triggers = new ConcurrentHashMap<>();
    
    public WorkflowService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeBuiltinWorkflows();
    }
    
    // ==================== 初始化 ====================
    
    private void initializeBuiltinWorkflows() {
        // 数据导入工作流
        createWorkflow(WorkflowDefinition.builder()
                .id("data-import-flow")
                .name("数据导入工作流")
                .description("自动导入并处理GIS数据")
                .category("data")
                .nodes(List.of(
                        WorkflowNode.builder()
                                .id("start")
                                .type("start")
                                .name("开始")
                                .position(new Position(100, 100))
                                .build(),
                        WorkflowNode.builder()
                                .id("upload")
                                .type("upload")
                                .name("上传文件")
                                .position(new Position(250, 100))
                                .config(Map.of("acceptedFormats", List.of("geojson", "shapefile", "kml")))
                                .build(),
                        WorkflowNode.builder()
                                .id("validate")
                                .type("validate")
                                .name("数据验证")
                                .position(new Position(400, 100))
                                .config(Map.of("rules", List.of("geometry", "schema", "CRS")))
                                .build(),
                        WorkflowNode.builder()
                                .id("transform")
                                .type("transform")
                                .name("格式转换")
                                .position(new Position(550, 100))
                                .config(Map.of("targetFormat", "geojson"))
                                .build(),
                        WorkflowNode.builder()
                                .id("store")
                                .type("store")
                                .name("存储数据")
                                .position(new Position(700, 100))
                                .config(Map.of("database", "postgis"))
                                .build(),
                        WorkflowNode.builder()
                                .id("notify")
                                .type("notify")
                                .name("发送通知")
                                .position(new Position(550, 250))
                                .config(Map.of("channel", "email"))
                                .build(),
                        WorkflowNode.builder()
                                .id("end")
                                .type("end")
                                .name("结束")
                                .position(new Position(700, 250))
                                .build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().id("e1").source("start").target("upload").build(),
                        WorkflowEdge.builder().id("e2").source("upload").target("validate").build(),
                        WorkflowEdge.builder().id("e3").source("validate").target("transform").condition("valid").build(),
                        WorkflowEdge.builder().id("e4").source("validate").target("notify").condition("invalid").build(),
                        WorkflowEdge.builder().id("e5").source("transform").target("store").build(),
                        WorkflowEdge.builder().id("e6").source("store").target("notify").build(),
                        WorkflowEdge.builder().id("e7").source("notify").target("end").build()
                ))
                .builtin(true)
                .build());
        
        // 地图发布工作流
        createWorkflow(WorkflowDefinition.builder()
                .id("map-publish-flow")
                .name("地图发布工作流")
                .description("创建并发布地图服务")
                .category("publishing")
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type("start").name("开始").position(new Position(100, 100)).build(),
                        WorkflowNode.builder().id("create").type("create").name("创建地图").position(new Position(250, 100)).build(),
                        WorkflowNode.builder().id("style").type("style").name("配置样式").position(new Position(400, 100)).build(),
                        WorkflowNode.builder().id("preview").type("preview").name("预览").position(new Position(550, 100)).build(),
                        WorkflowNode.builder().id("approve").type("approve").name("审批").position(new Position(400, 250)).build(),
                        WorkflowNode.builder().id("publish").type("publish").name("发布").position(new Position(550, 250)).build(),
                        WorkflowNode.builder().id("end").type("end").name("结束").position(new Position(700, 250)).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().id("e1").source("start").target("create").build(),
                        WorkflowEdge.builder().id("e2").source("create").target("style").build(),
                        WorkflowEdge.builder().id("e3").source("style").target("preview").build(),
                        WorkflowEdge.builder().id("e4").source("preview").target("approve").build(),
                        WorkflowEdge.builder().id("e5").source("approve").target("publish").condition("approved").build(),
                        WorkflowEdge.builder().id("e6").source("approve").target("style").condition("rejected").build(),
                        WorkflowEdge.builder().id("e7").source("publish").target("end").build()
                ))
                .builtin(true)
                .build());
        
        // 定时备份工作流
        createWorkflow(WorkflowDefinition.builder()
                .id("backup-flow")
                .name("定时备份工作流")
                .description("自动备份GIS数据")
                .category("maintenance")
                .nodes(List.of(
                        WorkflowNode.builder().id("trigger").type("schedule").name("定时触发").position(new Position(100, 100)).build(),
                        WorkflowNode.builder().id("export").type("export").name("导出数据").position(new Position(250, 100)).build(),
                        WorkflowNode.builder().id("compress").type("compress").name("压缩").position(new Position(400, 100)).build(),
                        WorkflowNode.builder().id("upload").type("upload").name("上传存储").position(new Position(550, 100)).build(),
                        WorkflowNode.builder().id("cleanup").type("cleanup").name("清理旧备份").position(new Position(400, 250)).build(),
                        WorkflowNode.builder().id("end").type("end").name("完成").position(new Position(700, 100)).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().id("e1").source("trigger").target("export").build(),
                        WorkflowEdge.builder().id("e2").source("export").target("compress").build(),
                        WorkflowEdge.builder().id("e3").source("compress").target("upload").build(),
                        WorkflowEdge.builder().id("e4").source("upload").target("cleanup").build(),
                        WorkflowEdge.builder().id("e5").source("cleanup").target("end").build()
                ))
                .builtin(true)
                .build());
    }
    
    // ==================== 工作流管理 ====================
    
    /**
     * 创建工作流
     */
    public WorkflowDefinition createWorkflow(WorkflowDefinition workflow) {
        workflow.setId(workflow.getId() != null ? workflow.getId() : UUID.randomUUID().toString());
        workflow.setCreatedAt(System.currentTimeMillis());
        workflow.setUpdatedAt(workflow.getCreatedAt());
        workflow.setStatus("draft");
        
        workflows.put(workflow.getId(), workflow);
        
        log.info("创建工作流: id={}, name={}", workflow.getId(), workflow.getName());
        
        return workflow;
    }
    
    /**
     * 获取工作流
     */
    public WorkflowDefinition getWorkflow(String workflowId) {
        WorkflowDefinition workflow = workflows.get(workflowId);
        if (workflow == null) {
            throw new WorkflowNotFoundException("工作流不存在: " + workflowId);
        }
        return workflow;
    }
    
    /**
     * 更新工作流
     */
    public WorkflowDefinition updateWorkflow(String workflowId, WorkflowDefinition updates) {
        WorkflowDefinition existing = getWorkflow(workflowId);
        
        if (existing.isBuiltin()) {
            throw new IllegalArgumentException("不能修改内置工作流");
        }
        
        updates.setId(workflowId);
        updates.setCreatedAt(existing.getCreatedAt());
        updates.setUpdatedAt(System.currentTimeMillis());
        
        workflows.put(workflowId, updates);
        
        return updates;
    }
    
    /**
     * 发布工作流
     */
    public void publishWorkflow(String workflowId) {
        WorkflowDefinition workflow = getWorkflow(workflowId);
        workflow.setStatus("published");
        workflow.setPublishedAt(System.currentTimeMillis());
        
        log.info("发布工作流: id={}", workflowId);
    }
    
    /**
     * 删除工作流
     */
    public void deleteWorkflow(String workflowId) {
        WorkflowDefinition workflow = getWorkflow(workflowId);
        
        if (workflow.isBuiltin()) {
            throw new IllegalArgumentException("不能删除内置工作流");
        }
        
        workflows.remove(workflowId);
        
        log.info("删除工作流: id={}", workflowId);
    }
    
    // ==================== 工作流执行 ====================
    
    /**
     * 启动工作流实例
     */
    public String startWorkflow(String workflowId, String userId, Map<String, Object> inputs) {
        WorkflowDefinition workflow = getWorkflow(workflowId);
        
        String instanceId = UUID.randomUUID().toString();
        
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(instanceId)
                .workflowId(workflowId)
                .workflowName(workflow.getName())
                .userId(userId)
                .status("running")
                .currentNodeId("start")
                .inputs(inputs)
                .outputs(new HashMap<>())
                .nodeStates(new HashMap<>())
                .createdAt(System.currentTimeMillis())
                .build();
        
        // 初始化节点状态
        for (WorkflowNode node : workflow.getNodes()) {
            instance.getNodeStates().put(node.getId(), NodeState.builder()
                    .nodeId(node.getId())
                    .status("pending")
                    .build());
        }
        
        instances.put(instanceId, instance);
        
        // 异步执行
        scheduler.submit(() -> executeWorkflow(instance));
        
        log.info("启动工作流实例: instanceId={}, workflowId={}", instanceId, workflowId);
        
        return instanceId;
    }
    
    private void executeWorkflow(WorkflowInstance instance) {
        WorkflowDefinition workflow = getWorkflow(instance.getWorkflowId());
        
        try {
            // 从开始节点执行
            String currentNodeId = "start";
            String nextNodeId;
            
            while (currentNodeId != null && !"end".equals(currentNodeId)) {
                WorkflowNode node = findNode(workflow, currentNodeId);
                
                instance.setCurrentNodeId(node.getId());
                updateNodeState(instance, node.getId(), "running");
                
                // 执行节点
                Object result = executeNode(node, instance);
                
                // 更新输出
                instance.getOutputs().put(node.getId(), result);
                updateNodeState(instance, node.getId(), "completed");
                
                // 找到下一个节点
                nextNodeId = findNextNode(workflow, node.getId(), result, instance);
                
                currentNodeId = nextNodeId;
                
                // 模拟执行延迟
                Thread.sleep(100);
            }
            
            instance.setStatus("completed");
            instance.setCompletedAt(System.currentTimeMillis());
            
            log.info("工作流执行完成: instanceId={}", instance.getId());
            
        } catch (Exception e) {
            instance.setStatus("failed");
            instance.setError(e.getMessage());
            instance.setCompletedAt(System.currentTimeMillis());
            
            log.error("工作流执行失败: instanceId={}", instance.getId(), e);
        }
    }
    
    private Object executeNode(WorkflowNode node, WorkflowInstance instance) {
        switch (node.getType()) {
            case "start":
            case "end":
                return Map.of("status", "ok");
                
            case "upload":
                return Map.of("fileId", UUID.randomUUID().toString(), "filename", "data.geojson");
                
            case "validate":
                return Map.of("valid", true, "errors", Collections.emptyList());
                
            case "transform":
                return Map.of("format", "geojson", "converted", true);
                
            case "store":
                return Map.of("storedId", UUID.randomUUID().toString());
                
            case "notify":
                return Map.of("sent", true, "channel", "email");
                
            case "create":
                return Map.of("mapId", UUID.randomUUID().toString());
                
            case "style":
                return Map.of("styleApplied", true);
                
            case "preview":
                return Map.of("previewUrl", "/preview/" + UUID.randomUUID());
                
            case "approve":
                // 模拟审批
                return Map.of("approved", true, "approver", "admin");
                
            case "publish":
                return Map.of("publishedUrl", "https://geonexus.ai/map/" + UUID.randomUUID());
                
            case "schedule":
                return Map.of("triggered", true, "time", System.currentTimeMillis());
                
            case "export":
                return Map.of("exportedFile", "/exports/" + UUID.randomUUID() + ".zip");
                
            case "compress":
                return Map.of("compressed", true, "size", 1024 * 1024);
                
            case "upload":
                return Map.of("uploaded", true, "storageUrl", "s3://backup/" + UUID.randomUUID());
                
            case "cleanup":
                return Map.of("cleanedFiles", 5);
                
            default:
                return Map.of("status", "executed");
        }
    }
    
    private String findNextNode(WorkflowDefinition workflow, String currentNodeId, Object result, WorkflowInstance instance) {
        // 找到当前节点的出边
        List<WorkflowEdge> outgoing = workflow.getEdges().stream()
                .filter(e -> e.getSource().equals(currentNodeId))
                .toList();
        
        if (outgoing.isEmpty()) {
            return null;
        }
        
        // 评估条件，选择下一节点
        for (WorkflowEdge edge : outgoing) {
            if (edge.getCondition() == null) {
                return edge.getTarget();
            }
            
            // 检查条件
            boolean conditionMet = evaluateCondition(edge.getCondition(), result);
            if (conditionMet) {
                return edge.getTarget();
            }
        }
        
        // 默认返回第一条边
        return outgoing.get(0).getTarget();
    }
    
    private boolean evaluateCondition(String condition, Object result) {
        if (condition == null) return true;
        
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        switch (condition) {
            case "valid":
                return Boolean.TRUE.equals(resultMap.get("valid"));
            case "invalid":
                return Boolean.FALSE.equals(resultMap.get("valid"));
            case "approved":
                return Boolean.TRUE.equals(resultMap.get("approved"));
            case "rejected":
                return Boolean.FALSE.equals(resultMap.get("approved"));
            default:
                return true;
        }
    }
    
    private WorkflowNode findNode(WorkflowDefinition workflow, String nodeId) {
        return workflow.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));
    }
    
    private void updateNodeState(WorkflowInstance instance, String nodeId, String status) {
        NodeState state = instance.getNodeStates().get(nodeId);
        if (state != null) {
            state.setStatus(status);
            state.setUpdatedAt(System.currentTimeMillis());
            if ("completed".equals(status)) {
                state.setCompletedAt(System.currentTimeMillis());
            }
        }
    }
    
    /**
     * 获取工作流实例
     */
    public WorkflowInstance getInstance(String instanceId) {
        return instances.get(instanceId);
    }
    
    /**
     * 获取用户的工作流实例
     */
    public List<WorkflowInstance> getUserInstances(String userId) {
        return instances.values().stream()
                .filter(i -> i.getUserId().equals(userId))
                .toList();
    }
    
    // ==================== 触发器 ====================
    
    /**
     * 创建触发器
     */
    public String createTrigger(WorkflowTrigger trigger) {
        trigger.setId(UUID.randomUUID().toString());
        trigger.setCreatedAt(System.currentTimeMillis());
        
        // 启动定时器
        if ("schedule".equals(trigger.getType())) {
            String cron = (String) trigger.getConfig().get("cron");
            // 简化实现
            scheduler.scheduleAtFixedRate(() -> {
                startWorkflow(trigger.getWorkflowId(), "system", trigger.getConfig());
            }, 0, 60, TimeUnit.MINUTES);
        }
        
        triggers.put(trigger.getId(), trigger);
        
        log.info("创建工作流触发器: id={}, workflowId={}, type={}", 
                trigger.getId(), trigger.getWorkflowId(), trigger.getType());
        
        return trigger.getId();
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class WorkflowDefinition {
        private String id;
        private String name;
        private String description;
        private String category;
        private List<WorkflowNode> nodes;
        private List<WorkflowEdge> edges;
        private String status;
        private boolean builtin;
        private Long createdAt;
        private Long updatedAt;
        private Long publishedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class WorkflowNode {
        private String id;
        private String type;
        private String name;
        private Position position;
        private Map<String, Object> config;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class WorkflowEdge {
        private String id;
        private String source;
        private String target;
        private String condition;
        private Map<String, Object> style;
    }
    
    @lombok.Data
    public static class Position {
        private double x;
        private double y;
        
        public Position() {}
        public Position(double x, double y) { this.x = x; this.y = y; }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class WorkflowInstance {
        private String id;
        private String workflowId;
        private String workflowName;
        private String userId;
        private String status;
        private String currentNodeId;
        private Map<String, Object> inputs;
        private Map<String, Object> outputs;
        private Map<String, NodeState> nodeStates;
        private String error;
        private Long createdAt;
        private Long completedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class NodeState {
        private String nodeId;
        private String status;
        private Long updatedAt;
        private Long completedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class WorkflowTrigger {
        private String id;
        private String name;
        private String workflowId;
        private String type; // schedule, event, webhook
        private Map<String, Object> config;
        private boolean enabled;
        private Long createdAt;
    }
    
    public static class WorkflowNotFoundException extends RuntimeException {
        public WorkflowNotFoundException(String msg) { super(msg); }
    }
}

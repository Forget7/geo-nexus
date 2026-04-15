package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能管理服务 - Skill注册与管理
 */
@Slf4j
@Service
public class SkillManagementService {
    
    private final CacheService cacheService;
    private final RestTemplate restTemplate;
    
    // Skill注册表
    private final Map<String, SkillDefinition> skillRegistry = new ConcurrentHashMap<>();
    private final Map<String, SkillInstance> userSkills = new ConcurrentHashMap<>();
    
    private static final String SKILL_PREFIX = "skill:def:";
    private static final String USER_SKILL_PREFIX = "skill:user:";
    
    public SkillManagementService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeBuiltinSkills();
    }
    
    // ==================== 初始化内置Skill ====================
    
    private void initializeBuiltinSkills() {
        // GIS分析Skill
        registerSkill(SkillDefinition.builder()
                .id("gis-spatial-analysis")
                .name("空间分析")
                .description("执行GIS空间分析操作，如缓冲区、叠加、距离计算等")
                .category("gis")
                .version("1.0.0")
                .builtin(true)
                .definition(Map.of(
                        "name", "spatial_analysis",
                        "description", "执行GIS空间分析",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "operation", Map.of("type", "string", "enum", List.of("buffer", "intersect", "union", "distance")),
                                        "layer", Map.of("type", "string"),
                                        "params", Map.of("type", "object")
                                ),
                                "required", List.of("operation", "layer")
                        )
                ))
                .capabilities(List.of("buffer", "intersect", "union", "difference", "distance", "within", "contains"))
                .build());
        
        // 地图制作Skill
        registerSkill(SkillDefinition.builder()
                .id("gis-map-making")
                .name("地图制作")
                .description("创建和配置专业地图，包括底图、图层、样式等")
                .category("gis")
                .version("1.0.0")
                .builtin(true)
                .definition(Map.of(
                        "name", "map_making",
                        "description", "制作专业地图"
                ))
                .capabilities(List.of("2d-map", "3d-map", "choropleth", "heat-map", "cluster"))
                .build());
        
        // 数据转换Skill
        registerSkill(SkillDefinition.builder()
                .id("gis-data-convert")
                .name("数据转换")
                .description("GIS数据格式转换，如GeoJSON、Shapefile、KML等")
                .category("gis")
                .version("1.0.0")
                .builtin(true)
                .capabilities(List.of("geojson", "shapefile", "kml", "gml", "topojson", "csv"))
                .build());
        
        // 坐标转换Skill
        registerSkill(SkillDefinition.builder()
                .id("gis-coord-transform")
                .name("坐标转换")
                .description("不同坐标系统之间的转换")
                .category("gis")
                .version("1.0.0")
                .builtin(true)
                .capabilities(List.of("wgs84", "gcj02", "bd09", "epsg:3857", "epsg:4490"))
                .build());
        
        // 地理编码Skill
        registerSkill(SkillDefinition.builder()
                .id("gis-geocoding")
                .name("地理编码")
                .description("地址到坐标的转换，以及反向地理编码")
                .category("gis")
                .version("1.0.0")
                .builtin(true)
                .capabilities(List.of("forward", "reverse", "batch"))
                .build());
        
        // 路径规划Skill
        registerSkill(SkillDefinition.builder()
                .id("gis-routing")
                .name("路径规划")
                .description("计算最优路径，支持驾车、步行、骑行等")
                .category("gis")
                .version("1.0.0")
                .builtin(true)
                .capabilities(List.of("driving", "walking", "cycling", "multi-modal"))
                .build());
        
        // 天气查询Skill
        registerSkill(SkillDefinition.builder()
                .id("weather-query")
                .name("天气查询")
                .description("查询指定位置的天气预报")
                .category("utility")
                .version("1.0.0")
                .builtin(true)
                .capabilities(List.of("current", "forecast", "hourly", "daily", "historical"))
                .build());
        
        // 文件处理Skill
        registerSkill(SkillDefinition.builder()
                .id("file-processing")
                .name("文件处理")
                .description("处理各种GIS文件格式")
                .category("utility")
                .version("1.0.0")
                .builtin(true)
                .capabilities(List.of("upload", "download", "validate", "compress"))
                .build());
        
        // 搜索Skill
        registerSkill(SkillDefinition.builder()
                .id("web-search")
                .name("网络搜索")
                .description("搜索互联网获取相关信息")
                .category("utility")
                .version("1.0.0")
                .builtin(true)
                .capabilities(List.of("general", "news", "academic", "image"))
                .build());
        
        // 代码执行Skill
        registerSkill(SkillDefinition.builder()
                .id("code-execution")
                .name("代码执行")
                .description("执行Python/JavaScript代码进行分析")
                .category("developer")
                .version("1.0.0")
                .builtin(true)
                .capabilities(List.of("python", "javascript", "sandbox"))
                .build());
    }
    
    // ==================== Skill注册 ====================
    
    /**
     * 注册Skill定义
     */
    public SkillDefinition registerSkill(SkillDefinition skill) {
        skill.setId(skill.getId() != null ? skill.getId() : UUID.randomUUID().toString());
        skill.setRegisteredAt(System.currentTimeMillis());
        
        skillRegistry.put(skill.getId(), skill);
        
        String key = SKILL_PREFIX + skill.getId();
        cacheService.set(key, skill);
        
        log.info("注册Skill: id={}, name={}", skill.getId(), skill.getName());
        
        return skill;
    }
    
    /**
     * 获取Skill定义
     */
    public SkillDefinition getSkill(String skillId) {
        SkillDefinition skill = skillRegistry.get(skillId);
        if (skill == null) {
            String key = SKILL_PREFIX + skillId;
            skill = (SkillDefinition) cacheService.get(key);
        }
        
        if (skill == null) {
            throw new SkillNotFoundException("Skill不存在: " + skillId);
        }
        
        return skill;
    }
    
    /**
     * 获取所有Skill
     */
    public List<SkillDefinition> getAllSkills() {
        return new ArrayList<>(skillRegistry.values());
    }
    
    /**
     * 按分类获取Skill
     */
    public List<SkillDefinition> getSkillsByCategory(String category) {
        List<SkillDefinition> skills = new ArrayList<>();
        for (SkillDefinition skill : skillRegistry.values()) {
            if (skill.getCategory().equals(category)) {
                skills.add(skill);
            }
        }
        return skills;
    }
    
    /**
     * 更新Skill定义
     */
    public SkillDefinition updateSkill(String skillId, SkillDefinition skill) {
        SkillDefinition existing = getSkill(skillId);
        
        if (existing.isBuiltin() && !skill.getDefinition().equals(existing.getDefinition())) {
            throw new IllegalArgumentException("不能修改内置Skill的定义");
        }
        
        skill.setId(skillId);
        skill.setBuiltin(existing.isBuiltin());
        skill.setRegisteredAt(existing.getRegisteredAt());
        skill.setUpdatedAt(System.currentTimeMillis());
        
        skillRegistry.put(skillId, skill);
        
        String key = SKILL_PREFIX + skillId;
        cacheService.set(key, skill);
        
        return skill;
    }
    
    /**
     * 删除Skill
     */
    public void unregisterSkill(String skillId) {
        SkillDefinition skill = getSkill(skillId);
        
        if (skill.isBuiltin()) {
            throw new IllegalArgumentException("不能删除内置Skill");
        }
        
        skillRegistry.remove(skillId);
        
        String key = SKILL_PREFIX + skillId;
        cacheService.delete(key);
        
        log.info("注销Skill: id={}", skillId);
    }
    
    // ==================== Skill导入/导出 ====================
    
    /**
     * 导入Skill（从JSON/Markdown）
     */
    public SkillDefinition importSkill(String content, String format) {
        try {
            SkillDefinition skill;
            
            if ("json".equalsIgnoreCase(format)) {
                skill = parseJsonSkill(content);
            } else if ("markdown".equalsIgnoreCase(format) || "md".equalsIgnoreCase(format)) {
                skill = parseMarkdownSkill(content);
            } else {
                throw new IllegalArgumentException("不支持的格式: " + format);
            }
            
            return registerSkill(skill);
            
        } catch (Exception e) {
            log.error("导入Skill失败", e);
            throw new SkillImportException("导入失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出Skill
     */
    public String exportSkill(String skillId, String format) {
        SkillDefinition skill = getSkill(skillId);
        
        if ("json".equalsIgnoreCase(format)) {
            return toJson(skill);
        } else if ("markdown".equalsIgnoreCase(format) || "md".equalsIgnoreCase(format)) {
            return toMarkdown(skill);
        } else {
            throw new IllegalArgumentException("不支持的格式: " + format);
        }
    }
    
    /**
     * 批量导入Skills
     */
    public ImportResult batchImport(List<String> contents, String format) {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        
        for (String content : contents) {
            try {
                importSkill(content, format);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add(e.getMessage());
            }
        }
        
        return ImportResult.builder()
                .total(contents.size())
                .success(success)
                .failed(failed)
                .errors(errors)
                .build();
    }
    
    // ==================== 用户Skill实例管理 ====================
    
    /**
     * 为用户创建Skill实例
     */
    public SkillInstance createUserSkillInstance(String userId, String skillId, Map<String, Object> config) {
        SkillDefinition skill = getSkill(skillId);
        
        SkillInstance instance = SkillInstance.builder()
                .id(UUID.randomUUID().toString())
                .skillId(skillId)
                .skillName(skill.getName())
                .userId(userId)
                .config(config)
                .enabled(true)
                .createdAt(System.currentTimeMillis())
                .build();
        
        String key = USER_SKILL_PREFIX + userId + ":" + instance.getId();
        cacheService.set(key, instance);
        
        userSkills.put(key, instance);
        
        log.info("创建用户Skill实例: userId={}, skillId={}, instanceId={}", 
                userId, skillId, instance.getId());
        
        return instance;
    }
    
    /**
     * 获取用户的Skill实例
     */
    public List<SkillInstance> getUserSkillInstances(String userId) {
        List<SkillInstance> instances = new ArrayList<>();
        
        for (SkillInstance instance : userSkills.values()) {
            if (instance.getUserId().equals(userId)) {
                instances.add(instance);
            }
        }
        
        return instances;
    }
    
    /**
     * 执行Skill
     */
    public SkillExecutionResult executeSkill(String instanceId, Map<String, Object> input) {
        // 获取Skill实例
        SkillInstance instance = null;
        for (SkillInstance si : userSkills.values()) {
            if (si.getId().equals(instanceId)) {
                instance = si;
                break;
            }
        }
        
        if (instance == null) {
            throw new SkillNotFoundException("Skill实例不存在: " + instanceId);
        }
        
        SkillDefinition skill = getSkill(instance.getSkillId());
        
        // 执行Skill逻辑
        // 这里是简化实现，实际应根据skill.type调用不同handler
        
        SkillExecutionResult result = SkillExecutionResult.builder()
                .executionId(UUID.randomUUID().toString())
                .skillId(skill.getId())
                .instanceId(instanceId)
                .success(true)
                .startTime(System.currentTimeMillis())
                .input(input)
                .output(Map.of("result", "Skill执行成功"))
                .build();
        
        result.setEndTime(System.currentTimeMillis());
        result.setDurationMs(result.getEndTime() - result.getStartTime());
        
        return result;
    }
    
    // ==================== 解析方法 ====================
    
    private SkillDefinition parseJsonSkill(String content) {
        // 简化实现，实际应使用Jackson
        Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                content, Map.class);
        
        return SkillDefinition.builder()
                .id((String) map.get("id"))
                .name((String) map.get("name"))
                .description((String) map.get("description"))
                .category((String) map.getOrDefault("category", "custom"))
                .version((String) map.getOrDefault("version", "1.0.0"))
                .definition((Map<String, Object>) map.get("definition"))
                .capabilities((List<String>) map.get("capabilities"))
                .builtin(false)
                .build();
    }
    
    private SkillDefinition parseMarkdownSkill(String content) {
        // 解析Markdown格式的Skill定义
        // 格式: # Skill Name\n\n## Description\n\n## Capabilities\n\n- ...
        
        String[] lines = content.split("\n");
        StringBuilder description = new StringBuilder();
        List<String> capabilities = new ArrayList<>();
        String currentSection = "";
        
        for (String line : lines) {
            if (line.startsWith("# ")) {
                // 名称已在元数据中
            } else if (line.startsWith("## ")) {
                currentSection = line.substring(3).trim().toLowerCase();
            } else if (line.startsWith("- ")) {
                capabilities.add(line.substring(2).trim());
            } else if (!line.trim().isEmpty()) {
                description.append(line).append("\n");
            }
        }
        
        return SkillDefinition.builder()
                .name("Imported Skill")
                .description(description.toString().trim())
                .category("custom")
                .version("1.0.0")
                .capabilities(capabilities)
                .builtin(false)
                .build();
    }
    
    private String toJson(SkillDefinition skill) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(skill);
        } catch (Exception e) {
            throw new RuntimeException("序列化失败", e);
        }
    }
    
    private String toMarkdown(SkillDefinition skill) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(skill.getName()).append("\n\n");
        md.append("**ID:** `").append(skill.getId()).append("`\n\n");
        md.append("**版本:** ").append(skill.getVersion()).append("\n\n");
        md.append("**分类:** ").append(skill.getCategory()).append("\n\n");
        md.append("## 描述\n\n").append(skill.getDescription()).append("\n\n");
        
        if (skill.getCapabilities() != null && !skill.getCapabilities().isEmpty()) {
            md.append("## 功能\n\n");
            for (String cap : skill.getCapabilities()) {
                md.append("- ").append(cap).append("\n");
            }
            md.append("\n");
        }
        
        if (skill.getDefinition() != null) {
            md.append("## 定义\n\n```json\n");
            try {
                md.append(new com.fasterxml.jackson.databind.ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(skill.getDefinition()));
            } catch (Exception e) {
                md.append(skill.getDefinition().toString());
            }
            md.append("\n```\n");
        }
        
        return md.toString();
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class SkillDefinition {
        private String id;
        private String name;
        private String description;
        private String category; // gis, utility, developer, custom
        private String version;
        private boolean builtin;
        private Map<String, Object> definition; // OpenAPI/Skill Schema格式
        private List<String> capabilities;
        private Long registeredAt;
        private Long updatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SkillInstance {
        private String id;
        private String skillId;
        private String skillName;
        private String userId;
        private Map<String, Object> config;
        private boolean enabled;
        private Long createdAt;
        private Long lastUsedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SkillExecutionResult {
        private String executionId;
        private String skillId;
        private String instanceId;
        private boolean success;
        private Long startTime;
        private Long endTime;
        private Long durationMs;
        private Map<String, Object> input;
        private Map<String, Object> output;
        private String error;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ImportResult {
        private int total;
        private int success;
        private int failed;
        private List<String> errors;
    }
    
    public static class SkillNotFoundException extends RuntimeException {
        public SkillNotFoundException(String message) { super(message); }
    }
    
    public static class SkillImportException extends RuntimeException {
        public SkillImportException(String message) { super(message); }
    }
}

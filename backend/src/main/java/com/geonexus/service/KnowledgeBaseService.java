package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/**
 * GIS知识库服务 - 专业地理信息知识库与知识图谱
 */
@Slf4j
@Service
public class KnowledgeBaseService {
    
    private final CacheService cacheService;
    private final RestTemplate restTemplate;
    
    // 知识库存储
    private static final String KB_PREFIX = "kb:";
    private static final String KG_PREFIX = "kg:";
    
    public KnowledgeBaseService(CacheService cacheService) {
        this.cacheService = cacheService;
        this.restTemplate = new RestTemplate();
    }
    
    // ==================== 知识库管理 ====================
    
    /**
     * 添加知识条目
     */
    public KnowledgeEntry addEntry(KnowledgeEntry entry) {
        entry.setId(UUID.randomUUID().toString());
        entry.setCreatedAt(System.currentTimeMillis());
        entry.setUpdatedAt(entry.getCreatedAt());
        
        String key = KB_PREFIX + entry.getId();
        cacheService.set(key, entry);
        
        // 更新分类索引
        updateCategoryIndex(entry.getCategory(), entry.getId());
        
        // 更新标签索引
        if (entry.getTags() != null) {
            for (String tag : entry.getTags()) {
                updateTagIndex(tag, entry.getId());
            }
        }
        
        log.info("添加知识条目: id={}, title={}", entry.getId(), entry.getTitle());
        return entry;
    }
    
    /**
     * 更新知识条目
     */
    public KnowledgeEntry updateEntry(String id, KnowledgeEntry entry) {
        String key = KB_PREFIX + id;
        KnowledgeEntry existing = (KnowledgeEntry) cacheService.get(key);
        
        if (existing == null) {
            throw new KnowledgeNotFoundException("知识条目不存在: " + id);
        }
        
        entry.setId(id);
        entry.setCreatedAt(existing.getCreatedAt());
        entry.setUpdatedAt(System.currentTimeMillis());
        
        cacheService.set(key, entry);
        return entry;
    }
    
    /**
     * 获取知识条目
     */
    public KnowledgeEntry getEntry(String id) {
        String key = KB_PREFIX + id;
        KnowledgeEntry entry = (KnowledgeEntry) cacheService.get(key);
        
        if (entry == null) {
            throw new KnowledgeNotFoundException("知识条目不存在: " + id);
        }
        
        return entry;
    }
    
    /**
     * 删除知识条目
     */
    public void deleteEntry(String id) {
        String key = KB_PREFIX + id;
        KnowledgeEntry entry = (KnowledgeEntry) cacheService.get(key);
        
        if (entry != null) {
            // 从分类索引移除
            removeFromCategoryIndex(entry.getCategory(), id);
            
            // 从标签索引移除
            if (entry.getTags() != null) {
                for (String tag : entry.getTags()) {
                    removeFromTagIndex(tag, id);
                }
            }
        }
        
        cacheService.delete(key);
        log.info("删除知识条目: id={}", id);
    }
    
    /**
     * 搜索知识
     */
    public List<KnowledgeEntry> search(SearchQuery query) {
        List<KnowledgeEntry> results = new ArrayList<>();
        
        // 关键词搜索
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            String keyword = query.getKeyword().toLowerCase();
            
            // 遍历所有知识条目（简化实现）
            // 实际应使用倒排索引
            Set<String> matchedIds = new HashSet<>();
            
            // 分类筛选
            if (query.getCategory() != null) {
                Set<String> categoryIds = getCategoryIndex(query.getCategory());
                matchedIds.addAll(categoryIds);
            }
            
            // 标签筛选
            if (query.getTags() != null && !query.getTags().isEmpty()) {
                Set<String> tagMatchedIds = new HashSet<>();
                for (String tag : query.getTags()) {
                    Set<String> tagIds = getTagIndex(tag);
                    if (tagMatchedIds.isEmpty()) {
                        tagMatchedIds.addAll(tagIds);
                    } else {
                        tagMatchedIds.retainAll(tagIds);
                    }
                }
                if (matchedIds.isEmpty()) {
                    matchedIds.addAll(tagMatchedIds);
                } else {
                    matchedIds.retainAll(tagMatchedIds);
                }
            }
            
            // 如果没有筛选条件，返回所有
            if (query.getCategory() == null && (query.getTags() == null || query.getTags().isEmpty())) {
                matchedIds = null; // 表示搜索全部
            }
            
            // 搜索标题和内容
            if (matchedIds == null || !matchedIds.isEmpty()) {
                // 简化：实际需要遍历索引
                List<KnowledgeEntry> allEntries = getAllEntries();
                for (KnowledgeEntry entry : allEntries) {
                    if (matchedIds != null && !matchedIds.contains(entry.getId())) {
                        continue;
                    }
                    
                    boolean matches = entry.getTitle().toLowerCase().contains(keyword)
                            || entry.getContent().toLowerCase().contains(keyword)
                            || (entry.getDescription() != null && entry.getDescription().toLowerCase().contains(keyword));
                    
                    if (matches) {
                        results.add(entry);
                    }
                }
            }
        } else {
            // 无关键词，返回分类或标签筛选结果
            if (query.getCategory() != null) {
                Set<String> ids = getCategoryIndex(query.getCategory());
                for (String id : ids) {
                    results.add(getEntry(id));
                }
            }
        }
        
        // 排序
        if (query.getSortBy() != null) {
            switch (query.getSortBy()) {
                case "relevance":
                    // 按相关性排序（简化）
                    break;
                case "updated":
                    results.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
                    break;
                case "created":
                default:
                    results.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    break;
            }
        }
        
        // 分页
        int start = query.getPage() * query.getPageSize();
        int end = Math.min(start + query.getPageSize(), results.size());
        
        if (start >= results.size()) {
            return new ArrayList<>();
        }
        
        return results.subList(start, end);
    }
    
    // ==================== 知识图谱管理 ====================
    
    /**
     * 添加实体
     */
    public KGEntity addEntity(KGEntity entity) {
        entity.setId(UUID.randomUUID().toString());
        entity.setCreatedAt(System.currentTimeMillis());
        
        String key = KG_PREFIX + "entity:" + entity.getId();
        cacheService.set(key, entity);
        
        // 更新类型索引
        updateEntityTypeIndex(entity.getType(), entity.getId());
        
        log.info("添加知识图谱实体: id={}, type={}, name={}", 
                entity.getId(), entity.getType(), entity.getName());
        
        return entity;
    }
    
    /**
     * 添加关系
     */
    public KGRelation addRelation(KGRelation relation) {
        relation.setId(UUID.randomUUID().toString());
        relation.setCreatedAt(System.currentTimeMillis());
        
        String key = KG_PREFIX + "relation:" + relation.getId();
        cacheService.set(key, relation);
        
        // 更新实体关系索引
        updateEntityRelationsIndex(relation.getSourceId(), relation.getId());
        updateEntityRelationsIndex(relation.getTargetId(), relation.getId());
        
        log.info("添加知识图谱关系: id={}, type={}", 
                relation.getId(), relation.getType());
        
        return relation;
    }
    
    /**
     * 查询实体关系
     */
    public List<KGRelation> getEntityRelations(String entityId) {
        Set<String> relationIds = getEntityRelationsIndex(entityId);
        List<KGRelation> relations = new ArrayList<>();
        
        for (String rid : relationIds) {
            String key = KG_PREFIX + "relation:" + rid;
            KGRelation relation = (KGRelation) cacheService.get(key);
            if (relation != null) {
                relations.add(relation);
            }
        }
        
        return relations;
    }
    
    /**
     * 实体链接（Entity Linking）- 将文本中的实体链接到知识图谱
     */
    public List<EntityLink> linkEntities(String text, String entityType) {
        List<EntityLink> links = new ArrayList<>();
        
        List<KGEntity> entities;
        if (entityType != null) {
            entities = getEntitiesByType(entityType);
        } else {
            entities = getAllEntities();
        }
        
        // 简单的关键词匹配
        for (KGEntity entity : entities) {
            String name = entity.getName();
            int index = text.indexOf(name);
            
            if (index >= 0) {
                EntityLink link = new EntityLink();
                link.setEntityId(entity.getId());
                link.setEntityName(name);
                link.setStartPos(index);
                link.setEndPos(index + name.length());
                link.setConfidence(0.9); // 简化
                links.add(link);
            }
        }
        
        return links;
    }
    
    /**
     * 知识推理 - 基于图谱的关系推理
     */
    public List<InferenceResult> infer(String sourceEntityId, String targetType, int depth) {
        List<InferenceResult> results = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        inferRecursive(sourceEntityId, targetType, depth, visited, results, new ArrayList<>());
        
        return results;
    }
    
    private void inferRecursive(String entityId, String targetType, int depth,
                                Set<String> visited, List<InferenceResult> results,
                                List<KGRelation> path) {
        if (depth == 0 || visited.contains(entityId)) {
            return;
        }
        
        visited.add(entityId);
        
        List<KGRelation> relations = getEntityRelations(entityId);
        
        for (KGRelation relation : relations) {
            List<KGRelation> newPath = new ArrayList<>(path);
            newPath.add(relation);
            
            KGEntity targetEntity = getEntity(relation.getTargetId());
            
            if (targetEntity.getType().equals(targetType)) {
                InferenceResult result = new InferenceResult();
                result.setPath(newPath);
                result.setConfidence(calculatePathConfidence(newPath));
                results.add(result);
            }
            
            if (depth > 1) {
                inferRecursive(relation.getTargetId(), targetType, depth - 1,
                        visited, results, newPath);
            }
        }
    }
    
    private double calculatePathConfidence(List<KGRelation> path) {
        // 简化：路径越长，置信度越低
        return Math.max(0.1, 1.0 - path.size() * 0.2);
    }
    
    /**
     * 获取实体
     */
    private KGEntity getEntity(String id) {
        String key = KG_PREFIX + "entity:" + id;
        return (KGEntity) cacheService.get(key);
    }
    
    /**
     * 获取所有实体
     */
    private List<KGEntity> getAllEntities() {
        List<KGEntity> entities = new ArrayList<>();
        // 实际需要遍历或使用索引
        return entities;
    }
    
    /**
     * 按类型获取实体
     */
    private List<KGEntity> getEntitiesByType(String type) {
        Set<String> ids = getEntityTypeIndex(type);
        List<KGEntity> entities = new ArrayList<>();
        
        for (String id : ids) {
            KGEntity entity = getEntity(id);
            if (entity != null) {
                entities.add(entity);
            }
        }
        
        return entities;
    }
    
    /**
     * 获取所有知识条目
     */
    private List<KnowledgeEntry> getAllEntries() {
        List<KnowledgeEntry> entries = new ArrayList<>();
        // 实际需要遍历或使用索引
        return entries;
    }
    
    // ==================== 索引管理 ====================
    
    private void updateCategoryIndex(String category, String entryId) {
        String key = KB_PREFIX + "index:category:" + category;
        Set<String> ids = (Set<String>) cacheService.get(key);
        if (ids == null) {
            ids = new HashSet<>();
        }
        ids.add(entryId);
        cacheService.set(key, ids);
    }
    
    private void removeFromCategoryIndex(String category, String entryId) {
        String key = KB_PREFIX + "index:category:" + category;
        Set<String> ids = (Set<String>) cacheService.get(key);
        if (ids != null) {
            ids.remove(entryId);
            cacheService.set(key, ids);
        }
    }
    
    private Set<String> getCategoryIndex(String category) {
        String key = KB_PREFIX + "index:category:" + category;
        Set<String> ids = (Set<String>) cacheService.get(key);
        return ids != null ? ids : new HashSet<>();
    }
    
    private void updateTagIndex(String tag, String entryId) {
        String key = KB_PREFIX + "index:tag:" + tag;
        Set<String> ids = (Set<String>) cacheService.get(key);
        if (ids == null) {
            ids = new HashSet<>();
        }
        ids.add(entryId);
        cacheService.set(key, ids);
    }
    
    private void removeFromTagIndex(String tag, String entryId) {
        String key = KB_PREFIX + "index:tag:" + tag;
        Set<String> ids = (Set<String>) cacheService.get(key);
        if (ids != null) {
            ids.remove(entryId);
            cacheService.set(key, ids);
        }
    }
    
    private Set<String> getTagIndex(String tag) {
        String key = KB_PREFIX + "index:tag:" + tag;
        Set<String> ids = (Set<String>) cacheService.get(key);
        return ids != null ? ids : new HashSet<>();
    }
    
    private void updateEntityTypeIndex(String type, String entityId) {
        String key = KG_PREFIX + "index:entity:type:" + type;
        Set<String> ids = (Set<String>) cacheService.get(key);
        if (ids == null) {
            ids = new HashSet<>();
        }
        ids.add(entityId);
        cacheService.set(key, ids);
    }
    
    private Set<String> getEntityTypeIndex(String type) {
        String key = KG_PREFIX + "index:entity:type:" + type;
        Set<String> ids = (Set<String>) cacheService.get(key);
        return ids != null ? ids : new HashSet<>();
    }
    
    private void updateEntityRelationsIndex(String entityId, String relationId) {
        String key = KG_PREFIX + "index:entity:relations:" + entityId;
        Set<String> ids = (Set<String>) cacheService.get(key);
        if (ids == null) {
            ids = new HashSet<>();
        }
        ids.add(relationId);
        cacheService.set(key, ids);
    }
    
    private Set<String> getEntityRelationsIndex(String entityId) {
        String key = KG_PREFIX + "index:entity:relations:" + entityId;
        Set<String> ids = (Set<String>) cacheService.get(key);
        return ids != null ? ids : new HashSet<>();
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    public static class KnowledgeEntry {
        private String id;
        private String title;
        private String content;
        private String description;
        private String category; // GIS基础/空间分析/数据管理/开发指南/行业应用
        private List<String> tags;
        private Map<String, Object> metadata;
        private Long createdAt;
        private Long updatedAt;
        private String author;
        private int viewCount;
        private double rating;
    }
    
    @lombok.Data
    public static class SearchQuery {
        private String keyword;
        private String category;
        private List<String> tags;
        private String sortBy = "relevance"; // relevance, updated, created
        private int page = 0;
        private int pageSize = 20;
    }
    
    @lombok.Data
    public static class KGEntity {
        private String id;
        private String name;
        private String type; // 城市/河流/山脉/建筑/事件/概念
        private Map<String, Object> properties;
        private Long createdAt;
    }
    
    @lombok.Data
    public static class KGRelation {
        private String id;
        private String sourceId;
        private String targetId;
        private String type; // 位于/流入/相邻/属于/相关
        private Map<String, Object> properties;
        private Long createdAt;
    }
    
    @lombok.Data
    public static class EntityLink {
        private String entityId;
        private String entityName;
        private int startPos;
        private int endPos;
        private double confidence;
    }
    
    @lombok.Data
    public static class InferenceResult {
        private List<KGRelation> path;
        private double confidence;
    }
    
    public static class KnowledgeNotFoundException extends RuntimeException {
        public KnowledgeNotFoundException(String message) { super(message); }
    }
}

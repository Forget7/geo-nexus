package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 协作系统 - 多用户实时协作与评论
 */
@Slf4j
@Service
public class CollaborationService {
    
    private final CacheService cacheService;
    
    // 协作文档
    private final Map<String, CollaborationDoc> documents = new ConcurrentHashMap<>();
    
    // 在线用户
    private final Map<String, Set<String>> documentParticipants = new ConcurrentHashMap<>();
    
    // 实时消息
    private final List<CollabMessage> messages = new CopyOnWriteArrayList<>();
    
    private static final String DOC_PREFIX = "collab:doc:";
    private static final String MSG_PREFIX = "collab:msg:";
    
    public CollaborationService(CacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    // ==================== 协作文档 ====================
    
    /**
     * 创建协作文档
     */
    public CollaborationDoc createDocument(CollaborationDoc doc) {
        doc.setId(UUID.randomUUID().toString());
        doc.setCreatedAt(System.currentTimeMillis());
        doc.setUpdatedAt(doc.getCreatedAt());
        doc.setStatus("active");
        
        // 初始化版本
        doc.setVersion(1L);
        
        documents.put(doc.getId(), doc);
        
        // 添加创建者为参与者
        addParticipant(doc.getId(), doc.getOwnerId());
        
        // 保存到缓存
        String key = DOC_PREFIX + doc.getId();
        cacheService.set(key, doc);
        
        log.info("创建协作文档: id={}, name={}, owner={}", 
                doc.getId(), doc.getName(), doc.getOwnerId());
        
        return doc;
    }
    
    /**
     * 获取协作文档
     */
    public CollaborationDoc getDocument(String docId) {
        CollaborationDoc doc = documents.get(docId);
        if (doc == null) {
            String key = DOC_PREFIX + docId;
            doc = (CollaborationDoc) cacheService.get(key);
        }
        
        if (doc == null) {
            throw new DocumentNotFoundException("文档不存在: " + docId);
        }
        
        return doc;
    }
    
    /**
     * 更新协作文档
     */
    public CollaborationDoc updateDocument(String docId, String userId, UpdateRequest update) {
        CollaborationDoc doc = getDocument(docId);
        
        // 验证权限
        if (!canEdit(docId, userId)) {
            throw new PermissionDeniedException("无权编辑此文档");
        }
        
        // 乐观锁检查版本
        if (!doc.getVersion().equals(update.getExpectedVersion())) {
            throw new VersionConflictException("文档版本冲突，请刷新后重试");
        }
        
        // 应用更新
        if (update.getContent() != null) {
            doc.setContent(update.getContent());
        }
        if (update.getTitle() != null) {
            doc.setTitle(update.getTitle());
        }
        
        doc.setUpdatedAt(System.currentTimeMillis());
        doc.setVersion(doc.getVersion() + 1);
        
        // 记录更新历史
        doc.getUpdateHistory().add(DocUpdate.builder()
                .userId(userId)
                .timestamp(doc.getUpdatedAt())
                .changeSummary(update.getChangeSummary())
                .build());
        
        // 限制历史记录数量
        if (doc.getUpdateHistory().size() > 100) {
            doc.getUpdateHistory().remove(0);
        }
        
        documents.put(docId, doc);
        
        log.info("更新协作文档: id={}, version={}, user={}", 
                docId, doc.getVersion(), userId);
        
        return doc;
    }
    
    /**
     * 锁定文档进行编辑
     */
    public LockInfo lockDocument(String docId, String userId) {
        CollaborationDoc doc = getDocument(docId);
        
        if (doc.getLockInfo() != null && 
                !doc.getLockInfo().getLockedBy().equals(userId) &&
                System.currentTimeMillis() < doc.getLockInfo().getExpiresAt()) {
            throw new DocumentLockedException("文档已被 " + doc.getLockInfo().getLockedByName() + " 锁定");
        }
        
        LockInfo lock = LockInfo.builder()
                .lockedBy(userId)
                .lockedByName(getUserName(userId))
                .lockedAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + 30 * 60 * 1000) // 30分钟超时
                .build();
        
        doc.setLockInfo(lock);
        documents.put(docId, doc);
        
        log.info("锁定文档: docId={}, user={}", docId, userId);
        
        return lock;
    }
    
    /**
     * 解锁文档
     */
    public void unlockDocument(String docId, String userId) {
        CollaborationDoc doc = getDocument(docId);
        
        if (doc.getLockInfo() != null && 
                doc.getLockInfo().getLockedBy().equals(userId)) {
            doc.setLockInfo(null);
            documents.put(docId, doc);
            log.info("解锁文档: docId={}, user={}", docId, userId);
        }
    }
    
    // ==================== 参与者管理 ====================
    
    /**
     * 添加参与者
     */
    public void addParticipant(String docId, String userId) {
        documentParticipants.computeIfAbsent(docId, k -> new HashSet<>()).add(userId);
        
        CollaborationDoc doc = getDocument(docId);
        if (!doc.getParticipants().contains(userId)) {
            doc.getParticipants().add(userId);
            documents.put(docId, doc);
        }
        
        log.info("添加参与者: docId={}, user={}", docId, userId);
    }
    
    /**
     * 移除参与者
     */
    public void removeParticipant(String docId, String userId) {
        Set<String> participants = documentParticipants.get(docId);
        if (participants != null) {
            participants.remove(userId);
        }
        
        CollaborationDoc doc = getDocument(docId);
        doc.getParticipants().remove(userId);
        documents.put(docId, doc);
        
        log.info("移除参与者: docId={}, user={}", docId, userId);
    }
    
    /**
     * 获取在线参与者
     */
    public Set<String> getOnlineParticipants(String docId) {
        return documentParticipants.getOrDefault(docId, new HashSet<>());
    }
    
    /**
     * 检查编辑权限
     */
    public boolean canEdit(String docId, String userId) {
        CollaborationDoc doc = getDocument(docId);
        return doc.getOwnerId().equals(userId) 
                || doc.getEditors().contains(userId)
                || doc.getParticipants().contains(userId);
    }
    
    // ==================== 实时评论 ====================
    
    /**
     * 添加评论
     */
    public Comment addComment(String docId, String userId, String content, String parentId) {
        CollaborationDoc doc = getDocument(docId);
        
        Comment comment = Comment.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .userName(getUserName(userId))
                .content(content)
                .parentId(parentId)
                .createdAt(System.currentTimeMillis())
                .build();
        
        doc.getComments().add(comment);
        documents.put(docId, doc);
        
        log.info("添加评论: docId={}, user={}, commentId={}", 
                docId, userId, comment.getId());
        
        return comment;
    }
    
    /**
     * 回复评论
     */
    public Comment replyToComment(String docId, String commentId, String userId, String content) {
        return addComment(docId, userId, content, commentId);
    }
    
    /**
     * 获取评论
     */
    public List<Comment> getComments(String docId) {
        CollaborationDoc doc = getDocument(docId);
        return new ArrayList<>(doc.getComments());
    }
    
    // ==================== 标注 ====================
    
    /**
     * 添加地图标注
     */
    public Annotation addAnnotation(String docId, String userId, Annotation annotation) {
        CollaborationDoc doc = getDocument(docId);
        
        annotation.setId(UUID.randomUUID().toString());
        annotation.setUserId(userId);
        annotation.setCreatedAt(System.currentTimeMillis());
        
        doc.getAnnotations().add(annotation);
        documents.put(docId, doc);
        
        log.info("添加标注: docId={}, user={}, annotationId={}", 
                docId, userId, annotation.getId());
        
        return annotation;
    }
    
    /**
     * 获取地图标注
     */
    public List<Annotation> getAnnotations(String docId) {
        CollaborationDoc doc = getDocument(docId);
        return new ArrayList<>(doc.getAnnotations());
    }
    
    /**
     * 删除标注
     */
    public void deleteAnnotation(String docId, String annotationId, String userId) {
        CollaborationDoc doc = getDocument(docId);
        
        doc.getAnnotations().removeIf(a -> 
                a.getId().equals(annotationId) && a.getUserId().equals(userId));
        
        documents.put(docId, doc);
        
        log.info("删除标注: docId={}, annotationId={}, user={}", docId, annotationId, userId);
    }
    
    // ==================== 版本历史 ====================
    
    /**
     * 获取版本历史
     */
    public List<DocUpdate> getVersionHistory(String docId) {
        CollaborationDoc doc = getDocument(docId);
        return new ArrayList<>(doc.getUpdateHistory());
    }
    
    /**
     * 回滚到指定版本
     */
    public CollaborationDoc rollbackToVersion(String docId, String userId, Long version) {
        CollaborationDoc doc = getDocument(docId);
        
        if (!canEdit(docId, userId)) {
            throw new PermissionDeniedException("无权编辑此文档");
        }
        
        // 找到指定版本的更新
        DocUpdate targetUpdate = null;
        for (DocUpdate update : doc.getUpdateHistory()) {
            if (update.getTimestamp().equals(version)) {
                targetUpdate = update;
                break;
            }
        }
        
        if (targetUpdate == null) {
            throw new VersionNotFoundException("版本不存在: " + version);
        }
        
        // 更新文档
        doc.setTitle(doc.getTitle() + " (回滚到 v" + version + ")");
        doc.setUpdatedAt(System.currentTimeMillis());
        doc.setVersion(doc.getVersion() + 1);
        
        documents.put(docId, doc);
        
        log.info("回滚文档: docId={}, toVersion={}, user={}", docId, version, userId);
        
        return doc;
    }
    
    // ==================== 辅助方法 ====================
    
    private String getUserName(String userId) {
        // 简化实现
        return "User-" + userId.substring(0, 8);
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class CollaborationDoc {
        private String id;
        private String name;
        private String title;
        private Object content; // GeoJSON or Map Config
        private String ownerId;
        private String ownerName;
        private Set<String> editors;
        private Set<String> participants;
        private String status;
        private Long version;
        private Long createdAt;
        private Long updatedAt;
        private LockInfo lockInfo;
        private List<Comment> comments;
        private List<Annotation> annotations;
        private List<DocUpdate> updateHistory;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class LockInfo {
        private String lockedBy;
        private String lockedByName;
        private Long lockedAt;
        private Long expiresAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Comment {
        private String id;
        private String userId;
        private String userName;
        private String content;
        private String parentId;
        private Long createdAt;
        private List<Comment> replies;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Annotation {
        private String id;
        private String userId;
        private String type; // point, line, polygon, text
        private Object geometry; // GeoJSON geometry
        private Object properties;
        private String style;
        private Long createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class DocUpdate {
        private String userId;
        private Long timestamp;
        private String changeSummary;
    }
    
    @lombok.Data
    public static class UpdateRequest {
        private Object content;
        private String title;
        private Long expectedVersion;
        private String changeSummary;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class CollabMessage {
        private String id;
        private String docId;
        private String userId;
        private String userName;
        private String type; // cursor, edit, comment, annotation
        private Object payload;
        private Long timestamp;
    }
    
    // 异常
    public static class DocumentNotFoundException extends RuntimeException {
        public DocumentNotFoundException(String msg) { super(msg); }
    }
    
    public static class PermissionDeniedException extends RuntimeException {
        public PermissionDeniedException(String msg) { super(msg); }
    }
    
    public static class VersionConflictException extends RuntimeException {
        public VersionConflictException(String msg) { super(msg); }
    }
    
    public static class DocumentLockedException extends RuntimeException {
        public DocumentLockedException(String msg) { super(msg); }
    }
    
    public static class VersionNotFoundException extends RuntimeException {
        public VersionNotFoundException(String msg) { super(msg); }
    }
}

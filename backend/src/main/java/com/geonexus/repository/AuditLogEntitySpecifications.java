package com.geonexus.repository;

import com.geonexus.domain.AuditLog;
import org.springframework.data.jpa.domain.Specification;

/**
 * 审计日志查询规格
 */
public class AuditLogEntitySpecifications {

    /**
     * 按操作类型过滤
     */
    public static Specification<AuditLog> hasAction(String action) {
        return (root, query, cb) -> {
            if (action == null || action.isEmpty()) return null;
            return cb.equal(root.get("action"), action);
        };
    }

    /**
     * 按用户ID过滤
     */
    public static Specification<AuditLog> byUser(String userId) {
        return (root, query, cb) -> {
            if (userId == null || userId.isEmpty()) return null;
            return cb.equal(root.get("userId"), userId);
        };
    }

    /**
     * 按资源类型过滤
     */
    public static Specification<AuditLog> hasResourceType(String resourceType) {
        return (root, query, cb) -> {
            if (resourceType == null || resourceType.isEmpty()) return null;
            return cb.equal(root.get("resourceType"), resourceType);
        };
    }

    /**
     * 按资源ID过滤
     */
    public static Specification<AuditLog> hasResourceId(String resourceId) {
        return (root, query, cb) -> {
            if (resourceId == null || resourceId.isEmpty()) return null;
            return cb.equal(root.get("resourceId"), resourceId);
        };
    }
}

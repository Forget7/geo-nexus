package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 细粒度资源权限服务
 *
 * 提供基于用户、资源、权限类型的细粒度访问控制
 *
 * 权限类型：READ, WRITE, ADMIN, DELETE
 *
 * 使用方式：
 * if (!permissionService.hasPermission(userId, resourceId, PermissionService.Permission.READ)) {
 *     throw new AccessDeniedException("无访问权限");
 * }
 */
@Slf4j
@Service
public class PermissionService {

    /** 权限类型 */
    public enum Permission {
        READ,    // 读取
        WRITE,   // 写入/编辑
        ADMIN,   // 管理
        DELETE   // 删除
    }

    /** 资源权限项 */
    public record ResourcePermission(
            String id,
            String userId,
            String resourceId,
            Permission permission,
            Map<String, Object> rowFilter,  // 行级过滤条件
            Date createdAt
    ) {}

    /** 用户权限上下文 */
    public record PermissionContext(
            String userId,
            String resourceId,
            Permission permission,
            Map<String, String> rowFilters  // 已解析的行级过滤
    ) {}

    private final Map<String, List<ResourcePermission>> permissions = new HashMap<>();

    /**
     * 检查用户是否有指定资源的指定权限
     */
    public boolean hasPermission(String userId, String resourceId, Permission permission) {
        List<ResourcePermission> userPerms = permissions.get(userId);
        if (userPerms == null) {
            log.debug("[Permission] 用户 {} 无任何权限记录", userId);
            return false;
        }

        return userPerms.stream()
                .filter(p -> p.resourceId().equals(resourceId))
                .anyMatch(p -> hasSufficientPermission(p.permission(), permission));
    }

    /**
     * 检查权限是否足够（ADMIN > DELETE > WRITE > READ）
     */
    private boolean hasSufficientPermission(Permission granted, Permission required) {
        return switch (required) {
            case READ -> granted == Permission.READ
                    || granted == Permission.WRITE
                    || granted == Permission.ADMIN
                    || granted == Permission.DELETE;
            case WRITE -> granted == Permission.WRITE
                    || granted == Permission.ADMIN
                    || granted == Permission.DELETE;
            case DELETE -> granted == Permission.DELETE || granted == Permission.ADMIN;
            case ADMIN -> granted == Permission.ADMIN;
        };
    }

    /**
     * 授予权限
     */
    public ResourcePermission grantPermission(String userId, String resourceId, Permission permission) {
        ResourcePermission rp = new ResourcePermission(
                UUID.randomUUID().toString(),
                userId,
                resourceId,
                permission,
                null,
                new Date()
        );

        permissions.computeIfAbsent(userId, k -> new ArrayList<>()).add(rp);
        log.info("[Permission] 授予权限: user={}, resource={}, permission={}", userId, resourceId, permission);
        return rp;
    }

    /**
     * 授予带行级过滤的权限
     */
    public ResourcePermission grantPermissionWithRowFilter(
            String userId, String resourceId, Permission permission, Map<String, Object> rowFilter) {
        ResourcePermission rp = new ResourcePermission(
                UUID.randomUUID().toString(),
                userId,
                resourceId,
                permission,
                rowFilter,
                new Date()
        );

        permissions.computeIfAbsent(userId, k -> new ArrayList<>()).add(rp);
        log.info("[Permission] 授予带行过滤权限: user={}, resource={}, permission={}, filter={}",
                userId, resourceId, permission, rowFilter);
        return rp;
    }

    /**
     * 撤销权限
     */
    public boolean revokePermission(String userId, String resourceId, Permission permission) {
        List<ResourcePermission> userPerms = permissions.get(userId);
        if (userPerms == null) return false;

        boolean removed = userPerms.removeIf(
                p -> p.resourceId().equals(resourceId) && p.permission() == permission
        );

        if (removed) {
            log.info("[Permission] 撤销权限: user={}, resource={}, permission={}", userId, resourceId, permission);
        }
        return removed;
    }

    /**
     * 撤销用户对资源的所有权限
     */
    public void revokeAllPermissions(String userId, String resourceId) {
        List<ResourcePermission> userPerms = permissions.get(userId);
        if (userPerms != null) {
            int count = userPerms.size();
            userPerms.removeIf(p -> p.resourceId().equals(resourceId));
            log.info("[Permission] 撤销所有权限: user={}, resource={}, count={}", userId, resourceId, count);
        }
    }

    /**
     * 获取用户对指定资源的所有权限
     */
    public List<Permission> getPermissions(String userId, String resourceId) {
        List<ResourcePermission> userPerms = permissions.get(userId);
        if (userPerms == null) return Collections.emptyList();

        return userPerms.stream()
                .filter(p -> p.resourceId().equals(resourceId))
                .map(ResourcePermission::permission)
                .toList();
    }

    /**
     * 获取用户的权限上下文（包含行级过滤）
     */
    public Optional<PermissionContext> getPermissionContext(String userId, String resourceId, Permission permission) {
        List<ResourcePermission> userPerms = permissions.get(userId);
        if (userPerms == null) return Optional.empty();

        return userPerms.stream()
                .filter(p -> p.resourceId().equals(resourceId))
                .filter(p -> hasSufficientPermission(p.permission(), permission))
                .map(p -> new PermissionContext(userId, resourceId, permission, parseRowFilters(p.rowFilter())))
                .findFirst();
    }

    /**
     * 解析行级过滤条件
     */
    private Map<String, String> parseRowFilters(Map<String, Object> rowFilter) {
        if (rowFilter == null) return Collections.emptyMap();
        Map<String, String> result = new HashMap<>();
        rowFilter.forEach((k, v) -> result.put(k, String.valueOf(v)));
        return result;
    }

    /**
     * 检查行级过滤条件
     */
    public boolean matchesRowFilter(Map<String, Object> row, Map<String, String> filter) {
        if (filter == null || filter.isEmpty()) return true;

        for (Map.Entry<String, String> entry : filter.entrySet()) {
            Object value = row.get(entry.getKey());
            if (value == null || !value.toString().equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取用户可访问的所有资源ID
     */
    public Set<String> getAccessibleResources(String userId, Permission minPermission) {
        List<ResourcePermission> userPerms = permissions.get(userId);
        if (userPerms == null) return Collections.emptySet();

        Set<String> resources = new HashSet<>();
        for (ResourcePermission p : userPerms) {
            if (hasSufficientPermission(p.permission(), minPermission)) {
                resources.add(p.resourceId());
            }
        }
        return resources;
    }
}

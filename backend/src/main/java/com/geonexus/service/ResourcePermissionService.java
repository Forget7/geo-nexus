package com.geonexus.service;

import com.geonexus.domain.ResourcePermission;
import com.geonexus.domain.ResourcePermission.Permission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 细粒度资源权限服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourcePermissionService {

    // 模拟存储（生产应使用 Repository + DB）
    private final Map<String, ResourcePermission> permissionStore = new LinkedHashMap<>();

    // 资源类型
    public enum ResourceType {
        LAYER, DATASET, MAP, WORKSPACE
    }

    /**
     * 检查用户是否有指定权限
     */
    public boolean checkPermission(String userId, ResourceType resourceType,
                                   String resourceId, Permission action) {
        return permissionStore.values().stream()
                .filter(p -> p.getUserId().equals(userId))
                .filter(p -> {
                    String key = resourceType.name() + ":" + resourceId;
                    String storedKey = extractResourceKey(p);
                    return storedKey.equals(key);
                })
                .anyMatch(p -> hasPermissionLevel(p.getPermission(), action));
    }

    /**
     * 授予权限
     */
    @Transactional
    public ResourcePermission grantPermission(String userId, ResourceType resourceType,
                                               String resourceId, Permission permission) {
        String key = resourceType.name() + ":" + resourceId;

        // 检查是否已有相同权限，有则更新
        Optional<ResourcePermission> existing = permissionStore.values().stream()
                .filter(p -> p.getUserId().equals(userId))
                .filter(p -> extractResourceKey(p).equals(key))
                .filter(p -> p.getPermission() == permission)
                .findFirst();

        if (existing.isPresent()) {
            return existing.get();
        }

        ResourcePermission rp = ResourcePermission.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .resourceId(key)
                .permission(permission)
                .createdAt(Instant.now())
                .build();

        permissionStore.put(rp.getId(), rp);
        log.info("授予权限: userId={}, resource={}, permission={}", userId, key, permission);
        return rp;
    }

    /**
     * 撤销权限
     */
    @Transactional
    public boolean revokePermission(String userId, ResourceType resourceType,
                                     String resourceId, Permission permission) {
        String key = resourceType.name() + ":" + resourceId;

        List<String> toRemove = permissionStore.entrySet().stream()
                .filter(e -> e.getValue().getUserId().equals(userId))
                .filter(e -> extractResourceKey(e.getValue()).equals(key))
                .filter(e -> e.getValue().getPermission() == permission)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (toRemove.isEmpty()) {
            return false;
        }

        toRemove.forEach(permissionStore::remove);
        log.info("撤销权限: userId={}, resource={}, permission={}", userId, key, permission);
        return true;
    }

    /**
     * 按ID撤销权限
     */
    @Transactional
    public boolean revokeById(String permissionId) {
        ResourcePermission removed = permissionStore.remove(permissionId);
        if (removed != null) {
            log.info("撤销权限(按ID): id={}", permissionId);
            return true;
        }
        return false;
    }

    /**
     * 获取资源的所有权限列表
     */
    public List<ResourcePermission> getResourcePermissions(ResourceType resourceType, String resourceId) {
        String key = resourceType.name() + ":" + resourceId;
        return permissionStore.values().stream()
                .filter(p -> extractResourceKey(p).equals(key))
                .collect(Collectors.toList());
    }

    /**
     * 获取用户可访问的资源
     */
    public List<ResourcePermission> getAccessibleResources(String userId, ResourceType resourceType) {
        return permissionStore.values().stream()
                .filter(p -> p.getUserId().equals(userId))
                .filter(p -> {
                    if (resourceType == null) return true;
                    String storedKey = extractResourceKey(p);
                    return storedKey.startsWith(resourceType.name() + ":");
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取当前用户的全部权限
     */
    public List<ResourcePermission> getCurrentUserPermissions(String userId) {
        return getAccessibleResources(userId, null);
    }

    // ── 内部工具 ─────────────────────────────────────

    private String extractResourceKey(ResourcePermission p) {
        return p.getResourceId(); // 存储时已格式化为 "TYPE:id"
    }

    /**
     * 权限级别比较：ADMIN > DELETE > WRITE > READ
     */
    private boolean hasPermissionLevel(Permission granted, Permission required) {
        return granted.ordinal() >= required.ordinal();
    }
}

package com.geonexus.service;

import com.geonexus.domain.AuditLog;
import com.geonexus.domain.ChatSessionEntity;
import com.geonexus.domain.GISDataEntity;
import com.geonexus.domain.UserEntity;
import com.geonexus.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理员服务 - 系统统计、用户管理、数据集管理、审计日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final GISDataRepository gisDataRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final AuditLogRepository auditLogRepository;

    // ===== System Stats =====

    /**
     * 获取系统全局统计数据
     */
    public SystemStats getSystemStats() {
        long totalUsers = userRepository.count();
        long totalDatasets = gisDataRepository.count();
        long totalChatSessions = chatSessionRepository.count();
        long totalStorageMB = calculateStorageSize();
        return new SystemStats(totalUsers, totalDatasets, totalChatSessions, totalStorageMB);
    }

    private long calculateStorageSize() {
        return gisDataRepository.findAll().stream()
                .mapToLong(g -> g.getSize() != null ? g.getSize() : 0L)
                .sum() / (1024 * 1024);
    }

    // ===== User Management =====

    /**
     * 分页获取用户列表
     */
    public List<UserSummary> listUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .stream()
                .map(u -> new UserSummary(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getRole(),
                        u.getCreatedAt() != null ? u.getCreatedAt().toString() : null,
                        u.getLastLogin() != null ? u.getLastLogin().toString() : null
                ))
                .toList();
    }

    /**
     * 设置用户角色
     */
    public void setUserRole(String userId, String role) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setRole(role);
        userRepository.save(user);
        log.info("User {} role changed to {}", userId, role);
    }

    /**
     * 禁用用户
     */
    public void disableUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setStatus(UserEntity.UserStatus.DISABLED);
        userRepository.save(user);
        log.info("User {} has been disabled", userId);
    }

    /**
     * 启用用户
     */
    public void enableUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User {} has been enabled", userId);
    }

    // ===== GIS Data Stats =====

    /**
     * 获取数据集统计信息
     */
    public List<DatasetStats> getDatasetStats() {
        return gisDataRepository.findAll(PageRequest.of(0, 100, Sort.by("createdAt").descending()))
                .stream()
                .map(g -> new DatasetStats(
                        g.getId(),
                        g.getFilename(),
                        g.getFormat() != null ? g.getFormat().name() : null,
                        g.getSize() != null ? g.getSize() / (1024 * 1024) : 0,
                        g.getCreatedAt() != null ? g.getCreatedAt().toString() : null,
                        g.getCrs()
                ))
                .toList();
    }

    // ===== Audit Logs =====

    /**
     * 分页获取审计日志
     */
    public List<AuditLogEntry> getAuditLogs(int page, int size, String level, String userId) {
        // level maps to action in AuditLog
        Specification<AuditLog> spec = AuditLogEntitySpecifications.hasAction(level).and(
                AuditLogEntitySpecifications.byUser(userId)
        );
        Page<AuditLog> result = auditLogRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by("timestamp").descending())
        );
        return result.getContent().stream()
                .map(a -> new AuditLogEntry(
                        a.getId(),
                        a.getTimestamp() != null ? a.getTimestamp().toString() : null,
                        a.getAction(), // using action as level
                        a.getUserId(),
                        a.getAction(),
                        a.getResourceType(),
                        a.getDetails() != null ? a.getDetails().toString() : null
                ))
                .toList();
    }

    // ===== DTOs =====

    public record SystemStats(
            long totalUsers,
            long totalDatasets,
            long totalChatSessions,
            long storageMB
    ) {}

    public record UserSummary(
            String id,
            String username,
            String email,
            String role,
            String createdAt,
            String lastLogin
    ) {}

    public record DatasetStats(
            String id,
            String name,
            String type,
            long sizeMB,
            String createdAt,
            String crs
    ) {}

    public record AuditLogEntry(
            String id,
            String timestamp,
            String level,
            String userId,
            String action,
            String resource,
            String message
    ) {}
}

package com.geonexus.repository;

import com.geonexus.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    /** 按用户查询审计日志 */
    List<AuditLog> findByUserIdOrderByTimestampDesc(String userId);

    /** 按资源类型和资源ID查询 */
    List<AuditLog> findByResourceTypeAndResourceIdOrderByTimestampDesc(
            String resourceType, String resourceId);

    /** 分页查询审计日志 */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    /** 按时间范围查询 */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimeRange(
            @Param("start") Instant start,
            @Param("end") Instant end);

    /** 组合条件查询 */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:resourceType IS NULL OR a.resourceType = :resourceType) AND " +
            "(:startTime IS NULL OR a.timestamp >= :startTime) AND " +
            "(:endTime IS NULL OR a.timestamp <= :endTime) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> searchAuditLogs(
            @Param("userId") String userId,
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}

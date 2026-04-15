package com.geonexus.repository;

import com.geonexus.domain.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓库
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    /**
     * 根据用户名查找
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * 根据邮箱查找
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 分页查找所有用户
     */
    Page<UserEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 按状态查找用户
     */
    Page<UserEntity> findByStatusOrderByCreatedAtDesc(
            UserEntity.UserStatus status, Pageable pageable);

    /**
     * 按角色查找用户
     */
    Page<UserEntity> findByRoleOrderByCreatedAtDesc(String role, Pageable pageable);

    /**
     * 搜索用户（按用户名或邮箱）
     */
    @Query("SELECT u FROM UserEntity u WHERE " +
            "(:keyword IS NULL OR u.username LIKE %:keyword% OR u.email LIKE %:keyword%) AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:status IS NULL OR u.status = :status) " +
            "ORDER BY u.createdAt DESC")
    Page<UserEntity> searchUsers(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("status") UserEntity.UserStatus status,
            Pageable pageable);

    /**
     * 统计用户总数
     */
    long countByStatus(UserEntity.UserStatus status);
}

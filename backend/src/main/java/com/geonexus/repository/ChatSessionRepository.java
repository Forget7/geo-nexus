package com.geonexus.repository;

import com.geonexus.domain.ChatSessionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * 聊天会话仓库
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, String> {
    
    /**
     * 查找用户的会话列表
     */
    Page<ChatSessionEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * 查找活跃会话
     */
    Page<ChatSessionEntity> findByUserIdOrderByUpdatedAtDesc(String userId, Pageable pageable);
    
    Page<ChatSessionEntity> findByUserIdAndStatusOrderByCreatedAtDesc(
            String userId, 
            ChatSessionEntity.SessionStatus status, 
            Pageable pageable);
    
    /**
     * 按ID和用户查找（预加载 messages 避免 N+1）
     */
    @EntityGraph(attributePaths = {"messages"})
    Optional<ChatSessionEntity> findByIdAndUserId(String id, String userId);
    
    /**
     * 统计用户会话数
     */
    long countByUserId(String userId);
    
    /**
     * 删除会话（软删除）
     */
    @Query("UPDATE ChatSessionEntity s SET s.status = 'DELETED' WHERE s.id = :id AND s.userId = :userId")
    void softDelete(@Param("id") String id, @Param("userId") String userId);
}

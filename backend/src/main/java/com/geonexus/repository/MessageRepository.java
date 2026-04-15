package com.geonexus.repository;

import com.geonexus.domain.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息仓库
 */
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, String> {
    
    /**
     * 查找会话的所有消息
     */
    List<MessageEntity> findBySessionIdOrderByTimestampAsc(String sessionId);
    
    /**
     * 分页查找会话消息
     */
    Page<MessageEntity> findBySessionIdOrderByTimestampDesc(String sessionId, Pageable pageable);
    
    /**
     * 查找会话的最新消息
     */
    @Query(value = "SELECT * FROM messages WHERE session_id = :sessionId ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    MessageEntity findLatestBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 统计会话消息数
     */
    long countBySessionId(String sessionId);
    
    /**
     * 删除会话的所有消息
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * 查找时间范围内的消息
     */
    @Query("SELECT m FROM MessageEntity m WHERE m.session.id = :sessionId AND m.timestamp BETWEEN :start AND :end ORDER BY m.timestamp ASC")
    List<MessageEntity> findBySessionIdAndTimeRange(
            @Param("sessionId") String sessionId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}

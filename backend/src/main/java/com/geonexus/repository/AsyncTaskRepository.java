package com.geonexus.repository;

import com.geonexus.domain.AsyncTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 异步任务仓库
 */
@Repository
public interface AsyncTaskRepository extends JpaRepository<AsyncTaskEntity, String> {
    
    /**
     * 查找用户的所有任务
     */
    Page<AsyncTaskEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * 查找用户指定状态的任务
     */
    Page<AsyncTaskEntity> findByUserIdAndStatusOrderByCreatedAtDesc(
            String userId, 
            AsyncTaskEntity.TaskStatus status, 
            Pageable pageable);
    
    /**
     * 查找用户指定类型的任务
     */
    Page<AsyncTaskEntity> findByUserIdAndTypeOrderByCreatedAtDesc(
            String userId, 
            AsyncTaskEntity.TaskType type, 
            Pageable pageable);
    
    /**
     * 按ID和用户查找
     */
    Optional<AsyncTaskEntity> findByIdAndUserId(String id, String userId);
    
    /**
     * 查找待处理任务（按创建时间）
     */
    List<AsyncTaskEntity> findByStatusOrderByCreatedAtAsc(AsyncTaskEntity.TaskStatus status);
    
    /**
     * 查找超时的任务
     */
    @Query("SELECT t FROM AsyncTaskEntity t WHERE t.status = 'PROCESSING' AND t.startedAt < :timeout")
    List<AsyncTaskEntity> findTimedOutTasks(@Param("timeout") LocalDateTime timeout);
    
    /**
     * 统计用户各状态任务数
     */
    long countByUserIdAndStatus(String userId, AsyncTaskEntity.TaskStatus status);
    
    /**
     * 删除用户的已完成任务（保留N天）
     */
    @Query("DELETE FROM AsyncTaskEntity t WHERE t.userId = :userId AND t.status IN ('COMPLETED', 'FAILED') AND t.completedAt < :before")
    void deleteOldCompletedTasks(@Param("userId") String userId, @Param("before") LocalDateTime before);
}

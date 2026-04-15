package com.geonexus.repository;

import com.geonexus.domain.StoryMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * 叙事地图仓库
 */
@Repository
public interface StoryMapRepository extends JpaRepository<StoryMapEntity, String> {

    /**
     * 按作者查找，按更新时间倒序
     */
    List<StoryMapEntity> findByAuthorIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String authorId);

    /**
     * 按状态查找已发布的，按发布时间倒序
     */
    @Query("SELECT s FROM StoryMapEntity s WHERE s.status = :status AND s.deletedAt IS NULL ORDER BY s.publishedAt DESC")
    List<StoryMapEntity> findByStatusOrderByPublishedAtDesc(@Param("status") String status);

    /**
     * 按分享码查找
     */
    Optional<StoryMapEntity> findByShareTokenAndDeletedAtIsNull(String shareToken);
}

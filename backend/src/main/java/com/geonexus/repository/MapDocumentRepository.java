package com.geonexus.repository;

import com.geonexus.domain.MapDocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * 地图文档仓库
 */
@Repository
public interface MapDocumentRepository extends JpaRepository<MapDocumentEntity, String> {
    
    /**
     * 查找用户的地图
     */
    Page<MapDocumentEntity> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);
    
    /**
     * 查找用户的地图（按模式）
     */
    Page<MapDocumentEntity> findByCreatedByAndModeOrderByCreatedAtDesc(
            String createdBy, 
            MapDocumentEntity.MapMode mode, 
            Pageable pageable);
    
    /**
     * 按ID和用户查找
     */
    Optional<MapDocumentEntity> findByIdAndCreatedBy(String id, String createdBy);
    
    /**
     * 搜索地图名称
     */
    @Query("SELECT m FROM MapDocumentEntity m WHERE m.createdBy = :userId AND (m.name LIKE %:keyword% OR m.description LIKE %:keyword%) ORDER BY m.createdAt DESC")
    Page<MapDocumentEntity> searchByKeyword(@Param("userId") String userId, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 统计用户地图数
     */
    long countByCreatedBy(String createdBy);
    
    /**
     * 删除用户的地图
     */
    void deleteByIdAndCreatedBy(String id, String createdBy);
}

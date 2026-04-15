package com.geonexus.repository;

import com.geonexus.domain.GISDataEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * GIS数据仓库
 */
@Repository
public interface GISDataRepository extends JpaRepository<GISDataEntity, String> {

    /**
     * 查找用户的数据集
     */
    Page<GISDataEntity> findByUploadedByOrderByCreatedAtDesc(String uploadedBy, Pageable pageable);

    /**
     * 按格式查找
     */
    List<GISDataEntity> findByFormatOrderByCreatedAtDesc(GISDataEntity.DataFormat format);

    /**
     * 按格式分页查找
     */
    Page<GISDataEntity> findByFormatOrderByCreatedAtDesc(
            GISDataEntity.DataFormat format, Pageable pageable);

    /**
     * 按坐标系查找
     */
    List<GISDataEntity> findByCrs(String crs);

    /**
     * 统计用户的 dataset 总数
     */
    long countByUploadedBy(String uploadedBy);

    /**
     * 搜索数据集（按文件名或原始文件名）
     */
    @Query("SELECT g FROM GISDataEntity g WHERE " +
            "(:keyword IS NULL OR g.filename LIKE %:keyword% OR g.originalFilename LIKE %:keyword%) AND " +
            "(:format IS NULL OR g.format = :format) AND " +
            "(:crs IS NULL OR g.crs = :crs) " +
            "ORDER BY g.createdAt DESC")
    Page<GISDataEntity> searchDatasets(
            @Param("keyword") String keyword,
            @Param("format") GISDataEntity.DataFormat format,
            @Param("crs") String crs,
            Pageable pageable);

    /**
     * 查找用户最近上传的数据
     */
    List<GISDataEntity> findTop10ByUploadedByOrderByCreatedAtDesc(String uploadedBy);
}

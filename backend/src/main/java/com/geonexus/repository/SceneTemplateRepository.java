package com.geonexus.repository;

import com.geonexus.domain.SceneTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 场景模板仓库
 */
@Repository
public interface SceneTemplateRepository extends JpaRepository<SceneTemplate, String> {

    /**
     * 分页查询所有模板
     */
    Page<SceneTemplate> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /**
     * 按名称模糊查询
     */
    Page<SceneTemplate> findByTenantIdAndNameContainingOrderByCreatedAtDesc(
            String tenantId, String name, Pageable pageable);

    /**
     * 根据ID和租户查询
     */
    Optional<SceneTemplate> findByIdAndTenantId(String id, String tenantId);
}

package com.geonexus.repository;

import com.geonexus.domain.PluginManifestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 插件清单仓库
 */
@Repository
public interface PluginManifestRepository extends JpaRepository<PluginManifestEntity, String> {

    Optional<PluginManifestEntity> findByPluginId(String pluginId);

    List<PluginManifestEntity> findByStatusOrderByInstallCountDesc(String status);

    List<PluginManifestEntity> findByCategoryAndStatusOrderByInstallCountDesc(String category, String status);

    List<PluginManifestEntity> findByTagsContainingAndStatusOrderByInstallCountDesc(String tag, String status);

    List<PluginManifestEntity> findByAuthorNameOrderByCreatedAtDesc(String authorName);

    boolean existsByPluginId(String pluginId);
}

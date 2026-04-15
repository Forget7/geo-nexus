package com.geonexus.repository;

import com.geonexus.domain.PluginInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 插件实例仓库
 */
@Repository
public interface PluginInstanceRepository extends JpaRepository<PluginInstanceEntity, String> {

    List<PluginInstanceEntity> findByUserIdAndStatus(String userId, String status);

    Optional<PluginInstanceEntity> findByPluginIdAndUserId(String pluginId, String userId);

    boolean existsByPluginIdAndUserId(String pluginId, String userId);

    void deleteByPluginIdAndUserId(String pluginId, String userId);
}

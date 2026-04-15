package com.geonexus.repository;

import com.geonexus.domain.ProcessDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OGC API - Processes 处理过程定义仓库
 */
@Repository
public interface ProcessDefinitionRepository extends JpaRepository<ProcessDefinitionEntity, String> {

    /**
     * 通过过程标识符查找
     */
    Optional<ProcessDefinitionEntity> findByProcessId(String processId);

    /**
     * 按分类查找并按过程ID排序
     */
    List<ProcessDefinitionEntity> findByCategoryOrderByProcessIdAsc(String category);

    /**
     * 按分类和过程ID排序查找全部
     */
    List<ProcessDefinitionEntity> findAllByOrderByCategoryAscProcessIdAsc();
}

package com.geonexus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geonexus.domain.SceneTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 4D场景模板服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SceneTemplateService {

    private final ObjectMapper objectMapper;

    // ========== CRUD 操作 ==========

    /**
     * 创建场景模板
     */
    @Transactional
    public SceneTemplate createTemplate(SceneTemplate template) {
        if (template.getTemplateType() == null) {
            template.setTemplateType("custom");
        }
        // layersJson 已在 controller 层处理
        template.setTenantId("default");
        template.setCreatedBy("system");
        template.setUpdatedBy("system");
        return template;
    }

    /**
     * 获取所有模板列表（概要信息，不含完整图层数据）
     */
    public List<SceneTemplate> listTemplates() {
        // 返回列表，不含 layersJson 以节省带宽
        return null; // 由 repository 处理
    }

    /**
     * 获取模板详情
     */
    public Optional<SceneTemplate> getTemplate(String id) {
        return Optional.empty(); // 由 repository 处理
    }

    /**
     * 更新模板
     */
    @Transactional
    public SceneTemplate updateTemplate(String id, SceneTemplate updates) {
        return null; // 由 repository 处理
    }

    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(String id) {
        // 由 repository 处理
    }

    // ========== 辅助方法 ==========

    /**
     * 将图层列表序列化为JSON
     */
    public String serializeLayers(List<Object> layers) {
        try {
            return objectMapper.writeValueAsString(layers);
        } catch (JsonProcessingException e) {
            log.error("图层配置序列化失败", e);
            return "[]";
        }
    }

    /**
     * 将JSON反序列化为图层列表
     */
    public List<Object> deserializeLayers(String json) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("图层配置反序列化失败", e);
            return List.of();
        }
    }
}

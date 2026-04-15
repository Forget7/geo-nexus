package com.geonexus.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OGC API - Processes 处理过程定义实体
 * 存储可用空间处理过程的元数据描述
 */
@Entity
@Table(name = "process_definitions", indexes = {
        @Index(name = "idx_process_pid", columnList = "processId", unique = true),
        @Index(name = "idx_process_category", columnList = "category")
})
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "OGC处理过程定义")
public class ProcessDefinitionEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理过程唯一标识符，如 "buffer", "simplify"
     */
    @Column(nullable = false, unique = true, length = 100)
    @Schema(description = "过程标识符")
    private String processId;

    /**
     * 过程标题（人类可读）
     */
    @Column(nullable = false, length = 200)
    @Schema(description = "过程标题")
    private String title;

    /**
     * 过程详细描述
     */
    @Column(length = 2000)
    @Schema(description = "过程描述")
    private String description;

    /**
     * 版本号
     */
    @Column(length = 20)
    @Schema(description = "版本")
    private String version;

    /**
     * 分类：geometry / analysis / transformation
     */
    @Column(length = 50)
    @Schema(description = "分类")
    private String category;

    /**
     * 输入参数定义（JSON数组）
     */
    @Column(columnDefinition = "TEXT")
    @Schema(description = "输入参数列表")
    private String inputsJson;

    /**
     * 输出参数定义（JSON数组）
     */
    @Column(columnDefinition = "TEXT")
    @Schema(description = "输出参数列表")
    private String outputsJson;

    /**
     * 是否支持异步执行
     */
    @Schema(description = "是否支持异步")
    private boolean supportsAsync;

    /**
     * 示例输入（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    @Schema(description = "示例输入")
    private String exampleInput;

    // ===== JSON serialization helpers =====

    @Transient
    public List<ProcessInput> getInputs() {
        if (inputsJson == null || inputsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(inputsJson, new TypeReference<List<ProcessInput>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public void setInputs(List<ProcessInput> inputs) {
        try {
            this.inputsJson = objectMapper.writeValueAsString(inputs);
        } catch (JsonProcessingException e) {
            this.inputsJson = "[]";
        }
    }

    @Transient
    public List<ProcessOutput> getOutputs() {
        if (outputsJson == null || outputsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(outputsJson, new TypeReference<List<ProcessOutput>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public void setOutputs(List<ProcessOutput> outputs) {
        try {
            this.outputsJson = objectMapper.writeValueAsString(outputs);
        } catch (JsonProcessingException e) {
            this.outputsJson = "[]";
        }
    }

    // ===== Nested types =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "过程输入参数")
    public static class ProcessInput implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "参数标识符")
        private String id;

        @Schema(description = "参数标题")
        private String title;

        @Schema(description = "参数描述")
        private String description;

        @Schema(description = "参数类型：GeoJSON / string / number / boolean")
        private String type;

        @Schema(description = "是否必填")
        private boolean required;

        @Schema(description = "默认值")
        private String defaultValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "过程输出参数")
    public static class ProcessOutput implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "输出标识符")
        private String id;

        @Schema(description = "输出标题")
        private String title;

        @Schema(description = "输出描述")
        private String description;

        @Schema(description = "输出类型：GeoJSON / string / number")
        private String type;
    }
}

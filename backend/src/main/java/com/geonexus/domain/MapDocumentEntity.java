package com.geonexus.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 地图文档实体
 */
@Entity
@Table(name = "map_documents", indexes = {
        @Index(name = "idx_map_user", columnList = "createdBy"),
        @Index(name = "idx_map_created", columnList = "createdAt")
})
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapDocumentEntity extends BaseEntity {
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "JSONB")
    private String geojson;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MapMode mode;
    
    @Column(length = 50)
    private String renderer;
    
    private String thumbnailUrl;
    
    @Column(length = 100)
    private String tileType;
    
    @Column(columnDefinition = "TEXT")
    private String center; // JSON: [lat, lon]
    
    private Integer zoom;
    
    public enum MapMode {
        TWO_D, THREE_D
    }
}

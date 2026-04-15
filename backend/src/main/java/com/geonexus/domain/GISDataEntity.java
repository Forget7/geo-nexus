package com.geonexus.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * GIS数据实体
 */
@Entity
@Table(name = "gis_data", indexes = {
        @Index(name = "idx_data_user", columnList = "uploadedBy"),
        @Index(name = "idx_data_format", columnList = "format"),
        @Index(name = "idx_data_crs", columnList = "crs")
})
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GISDataEntity extends BaseEntity {
    
    @Column(nullable = false)
    private String filename;
    
    @Column(nullable = false)
    private String originalFilename;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataFormat format;
    
    @Column(length = 50)
    private String crs;
    
    private Long size;
    
    @Column(nullable = false)
    private String storagePath;
    
    @Column(columnDefinition = "TEXT")
    private String metadataJson;
    
    public enum DataFormat {
        GEOJSON, SHP, KML, GML, GPX, GEOTIFF, CSV
    }
}

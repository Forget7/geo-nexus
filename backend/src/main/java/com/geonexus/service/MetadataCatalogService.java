package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 元数据目录服务 - ISO 19115 标准支持
 */
@Slf4j
@Service
public class MetadataCatalogService {
    
    private final CacheService cacheService;
    
    // 元数据记录
    private final Map<String, MetadataRecord> records = new ConcurrentHashMap<>();
    
    // 分类
    private final Map<String, Category> categories = new ConcurrentHashMap<>();
    
    public MetadataCatalogService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeCategories();
    }
    
    // ==================== 初始化 ====================
    
    private void initializeCategories() {
        categories.put("imagery", Category.builder()
                .id("imagery").name("影像").description("卫星和航空影像").build());
        categories.put("vector", Category.builder()
                .id("vector").name("矢量数据").description("矢量地理数据").build());
        categories.put("terrain", Category.builder()
                .id("terrain").name("地形数据").description("DEM/DSM等").build());
        categories.put("basemap", Category.builder()
                .id("basemap").name("底图").description("基础底图").build());
        categories.put("analysis", Category.builder()
                .id("analysis").name("分析结果").description("分析输出").build());
    }
    
    // ==================== 元数据管理 ====================
    
    /**
     * 创建元数据
     */
    public MetadataRecord createMetadata(MetadataRecord metadata) {
        metadata.setId(UUID.randomUUID().toString());
        metadata.setCreatedAt(System.currentTimeMillis());
        metadata.setUpdatedAt(metadata.getCreatedAt());
        
        // 生成唯一标识符
        if (metadata.getIdentifier() == null) {
            metadata.setIdentifier("urn:uuid:" + metadata.getId());
        }
        
        records.put(metadata.getId(), metadata);
        
        // 更新分类
        if (metadata.getCategory() != null) {
            Category cat = categories.get(metadata.getCategory());
            if (cat != null && !cat.getRecordIds().contains(metadata.getId())) {
                cat.getRecordIds().add(metadata.getId());
            }
        }
        
        log.info("创建元数据: id={}, title={}", metadata.getId(), metadata.getTitle());
        
        return metadata;
    }
    
    /**
     * 获取元数据
     */
    public MetadataRecord getMetadata(String id) {
        return records.get(id);
    }
    
    /**
     * 更新元数据
     */
    public MetadataRecord updateMetadata(String id, MetadataRecord updates) {
        MetadataRecord existing = records.get(id);
        if (existing == null) {
            throw new MetadataNotFoundException("元数据不存在: " + id);
        }
        
        updates.setId(id);
        updates.setCreatedAt(existing.getCreatedAt());
        updates.setUpdatedAt(System.currentTimeMillis());
        
        records.put(id, updates);
        
        return updates;
    }
    
    /**
     * 删除元数据
     */
    public void deleteMetadata(String id) {
        MetadataRecord metadata = records.get(id);
        if (metadata != null) {
            // 从分类中移除
            Category cat = categories.get(metadata.getCategory());
            if (cat != null) {
                cat.getRecordIds().remove(id);
            }
            
            records.remove(id);
            log.info("删除元数据: id={}", id);
        }
    }
    
    /**
     * 搜索元数据
     */
    public List<MetadataRecord> search(MetadataSearchQuery query) {
        List<MetadataRecord> results = new ArrayList<>();
        
        for (MetadataRecord record : records.values()) {
            if (matchesQuery(record, query)) {
                results.add(record);
            }
        }
        
        // 排序
        results.sort((a, b) -> {
            if ("relevance".equals(query.getSortBy())) {
                return b.getUpdatedAt().compareTo(a.getUpdatedAt());
            } else if ("date".equals(query.getSortBy())) {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            } else if ("title".equals(query.getSortBy())) {
                return a.getTitle().compareToIgnoreCase(b.getTitle());
            }
            return 0;
        });
        
        // 分页
        int start = query.getPage() * query.getPageSize();
        int end = Math.min(start + query.getPageSize(), results.size());
        
        if (start >= results.size()) {
            return new ArrayList<>();
        }
        
        return results.subList(start, end);
    }
    
    private boolean matchesQuery(MetadataRecord record, MetadataSearchQuery query) {
        // 关键词搜索
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            String keyword = query.getKeyword().toLowerCase();
            boolean matches = record.getTitle().toLowerCase().contains(keyword)
                    || (record.getAbstractText() != null && record.getAbstractText().toLowerCase().contains(keyword))
                    || (record.getKeywords() != null && record.getKeywords().stream()
                            .anyMatch(k -> k.toLowerCase().contains(keyword)));
            if (!matches) return false;
        }
        
        // 分类筛选
        if (query.getCategory() != null && !query.getCategory().equals(record.getCategory())) {
            return false;
        }
        
        // 类型筛选
        if (query.getType() != null && !query.getType().equals(record.getType())) {
            return false;
        }
        
        // 格式筛选
        if (query.getFormat() != null && !query.getFormat().equals(record.getFormat())) {
            return false;
        }
        
        // 时间范围
        if (query.getStartDate() != null && record.getCreatedAt() < query.getStartDate()) {
            return false;
        }
        if (query.getEndDate() != null && record.getCreatedAt() > query.getEndDate()) {
            return false;
        }
        
        return true;
    }
    
    // ==================== ISO 19115 导出 ====================
    
    /**
     * 导出为ISO 19115 XML
     */
    public String exportToISO19115(String id) {
        MetadataRecord metadata = getMetadata(id);
        if (metadata == null) {
            throw new MetadataNotFoundException("元数据不存在: " + id);
        }
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<mdb:MD_Metadata xmlns:mdb=\"http://standards.iso.org/iso/19115/-3/mdb/2.0\"\n");
        xml.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("  xmlns:gco=\"http://standards.iso.org/iso/19115/-3/gco/1.0\"\n");
        xml.append("  xmlns:gml=\"http://www.opengis.net/gml/3.2\">\n");
        
        // 标识符
        xml.append("  <mdb:metadataIdentifier>\n");
        xml.append("    <gco:CharacterString>").append(metadata.getIdentifier()).append("</gco:CharacterString>\n");
        xml.append("  </mdb:metadataIdentifier>\n");
        
        // 标题
        xml.append("  <mdb:defaultLocale>\n");
        xml.append("    <mdb:PT_Locale>\n");
        xml.append("      <mdb:language>\n");
        xml.append("        <gco:LanguageCode codeList=\"language\" codeListValue=\"").append(metadata.getLanguage()).append("\"/>\n");
        xml.append("      </mdb:language>\n");
        xml.append("    </mdb:PT_Locale>\n");
        xml.append("  </mdb:defaultLocale>\n");
        
        // 标题
        xml.append("  <mdb:resourceMetadata>\n");
        xml.append("    <mri:MD_Identification>\n");
        xml.append("      <mri:citation>\n");
        xml.append("        <mcc:CI_Citation>\n");
        xml.append("          <mcc:title>\n");
        xml.append("            <gco:CharacterString>").append(metadata.getTitle()).append("</gco:CharacterString>\n");
        xml.append("          </mcc:title>\n");
        xml.append("        </mcc:CI_Citation>\n");
        xml.append("      </mri:citation>\n");
        
        // 摘要
        if (metadata.getAbstractText() != null) {
            xml.append("      <mri:abstract>\n");
            xml.append("        <gco:CharacterString>").append(metadata.getAbstractText()).append("</gco:CharacterString>\n");
            xml.append("      </mri:abstract>\n");
        }
        
        // 关键词
        if (metadata.getKeywords() != null && !metadata.getKeywords().isEmpty()) {
            xml.append("      <mri:descriptiveKeywords>\n");
            xml.append("        <mrd:MD_Keywords>\n");
            for (String keyword : metadata.getKeywords()) {
                xml.append("          <mrd:keyword>\n");
                xml.append("            <gco:CharacterString>").append(keyword).append("</gco:CharacterString>\n");
                xml.append("          </mrd:keyword>\n");
            }
            xml.append("        </mrd:MD_Keywords>\n");
            xml.append("      </mri:descriptiveKeywords>\n");
        }
        
        xml.append("    </mri:MD_Identification>\n");
        xml.append("  </mdb:resourceMetadata>\n");
        
        // 地理范围
        if (metadata.getBoundingBox() != null) {
            xml.append("  <mdb:spatialExtent>\n");
            xml.append("    <gex:EX_Extent>\n");
            xml.append("      <gex:geographicElement>\n");
            xml.append("        <gex:EX_GeographicBoundingBox>\n");
            xml.append("          <gex:westBoundLongitude>\n");
            xml.append("            <gco:Decimal>").append(metadata.getBoundingBox()[0]).append("</gco:Decimal>\n");
            xml.append("          </gex:westBoundLongitude>\n");
            xml.append("          <gex:eastBoundLongitude>\n");
            xml.append("            <gco:Decimal>").append(metadata.getBoundingBox()[2]).append("</gco:Decimal>\n");
            xml.append("          </gex:eastBoundLongitude>\n");
            xml.append("          <gex:southBoundLatitude>\n");
            xml.append("            <gco:Decimal>").append(metadata.getBoundingBox()[1]).append("</gco:Decimal>\n");
            xml.append("          </gex:southBoundLatitude>\n");
            xml.append("          <gex:northBoundLatitude>\n");
            xml.append("            <gco:Decimal>").append(metadata.getBoundingBox()[3]).append("</gco:Decimal>\n");
            xml.append("          </gex:northBoundLatitude>\n");
            xml.append("        </gex:EX_GeographicBoundingBox>\n");
            xml.append("      </gex:geographicElement>\n");
            xml.append("    </gex:EX_Extent>\n");
            xml.append("  </mdb:spatialExtent>\n");
        }
        
        xml.append("</mdb:MD_Metadata>\n");
        
        return xml.toString();
    }
    
    // ==================== 分类管理 ====================
    
    public List<Category> getCategories() {
        return new ArrayList<>(categories.values());
    }
    
    public Category createCategory(Category category) {
        category.setId(category.getId() != null ? category.getId() : UUID.randomUUID().toString());
        categories.put(category.getId(), category);
        return category;
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class MetadataRecord {
        private String id;
        private String identifier;
        private String title;
        private String abstractText;
        private String category;
        private String type;
        private String format;
        private String language;
        private String[] keywords;
        private double[] boundingBox;
        private Contact creator;
        private Contact distributor;
        private String[] availableFormats;
        private String dataSource;
        private Map<String, Object> customFields;
        private Long createdAt;
        private Long updatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Contact {
        private String name;
        private String organization;
        private String email;
        private String phone;
        private String address;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Category {
        private String id;
        private String name;
        private String description;
        private List<String> recordIds;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MetadataSearchQuery {
        private String keyword;
        private String category;
        private String type;
        private String format;
        private Long startDate;
        private Long endDate;
        private String sortBy = "relevance";
        private int page = 0;
        private int pageSize = 20;
    }
    
    public static class MetadataNotFoundException extends RuntimeException {
        public MetadataNotFoundException(String msg) { super(msg); }
    }
}

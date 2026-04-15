package com.geonexus.service;

import com.geonexus.model.DataConvertRequest;
import com.geonexus.model.DataUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataService {
    
    @Value("${geonexus.upload.dir:./data/uploads}")
    private String uploadDir;
    
    private final Map<String, DataMeta> dataStore = new HashMap<>();
    
    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
    }
    
    public Map<String, Object> convertData(DataConvertRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("inputFormat", request.getInputFormat());
        result.put("outputFormat", request.getOutputFormat());
        result.put("data", request.getData());
        
        // 坐标系转换
        if (request.getSourceCrs() != null && request.getTargetCrs() != null) {
            result.put("sourceCrs", request.getSourceCrs());
            result.put("targetCrs", request.getTargetCrs());
            result.put("message", "坐标系转换：需要集成GeoTools");
        } else {
            result.put("message", "数据格式转换完成（简化版）");
        }
        
        return result;
    }
    
    public DataUploadResponse uploadData(MultipartFile file) {
        try {
            String dataId = UUID.randomUUID().toString().substring(0, 8);
            String originalFilename = file.getOriginalFilename();
            // 防止路径穿越攻击：移除文件名中的 ../ 和 ..\ 序列
            String filename = originalFilename != null
                    ? originalFilename.replaceAll("\\.\\.[/\\\\]", "").replaceAll("[/\\\\]", "_")
                    : "unnamed_file";
            String format = detectFormat(filename);
            
            // 安全检查：文件类型白名单验证
            if (!isAllowedFormat(format)) {
                log.warn("Rejected upload of disallowed file format: {}", format);
                return DataUploadResponse.builder()
                        .filename(filename)
                        .format("error")
                        .build();
            }
            
            // 安全检查：文件名白名单验证（不允许无扩展名或危险扩展名）
            if (!isAllowedFilename(filename)) {
                log.warn("Rejected upload with disallowed filename pattern: {}", filename);
                return DataUploadResponse.builder()
                        .filename(filename)
                        .format("error")
                        .build();
            }
            
            // 保存文件
            Path filepath = Paths.get(uploadDir, dataId + "_" + filename);
            Files.write(filepath, file.getBytes());
            
            // 存储元数据
            DataMeta meta = new DataMeta(dataId, filename, format, file.getSize(), filepath.toString());
            dataStore.put(dataId, meta);
            
            return DataUploadResponse.builder()
                    .id(dataId)
                    .filename(filename)
                    .format(format)
                    .size(file.getSize())
                    .url("/api/v1/data/" + dataId)
                    .build();
                    
        } catch (IOException e) {
            log.error("Failed to upload data", e);
            return DataUploadResponse.builder()
                    .filename(file.getOriginalFilename())
                    .format("error")
                    .build();
        }
    }
    
    public Map<String, Object> getData(String dataId, String format) {
        DataMeta meta = dataStore.get(dataId);
        if (meta == null) {
            return Map.of("error", "Data not found");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", meta.id);
        result.put("filename", meta.filename);
        result.put("format", meta.format);
        result.put("size", meta.size);
        result.put("path", meta.path);
        
        return result;
    }
    
    public Map<String, Object> getDataAsGeoJSON(String dataId) {
        DataMeta meta = dataStore.get(dataId);
        if (meta == null) {
            return Map.of("error", "Data not found");
        }
        
        try {
            String content = Files.readString(Paths.get(meta.path));
            return Map.of("geojson", content, "format", meta.format);
        } catch (IOException e) {
            return Map.of("error", e.getMessage());
        }
    }
    
    private String detectFormat(String filename) {
        if (filename == null) return "unknown";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".geojson") || lower.endsWith(".json")) return "geojson";
        if (lower.endsWith(".shp")) return "shapefile";
        if (lower.endsWith(".kml")) return "kml";
        if (lower.endsWith(".gml")) return "gml";
        if (lower.endsWith(".gpx")) return "gpx";
        if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return "geotiff";
        if (lower.endsWith(".csv")) return "csv";
        if (lower.endsWith(".zip")) return "zip";
        return "unknown";
    }
    
    /**
     * 文件格式白名单检查
     */
    private static final Set<String> ALLOWED_FORMATS = Set.of(
            "geojson", "shapefile", "kml", "gml", "gpx", "geotiff", "csv", "zip"
    );
    
    private boolean isAllowedFormat(String format) {
        return format != null && ALLOWED_FORMATS.contains(format.toLowerCase());
    }
    
    /**
     * 文件名安全检查：不允许可执行脚本等危险扩展名
     */
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "sh", "bat", "cmd", "ps1", "vbs", "js", "jsp", "php", "asp", "aspx",
            "html", "htm", "xhtml", "svg", "xml", "xsl"
    );
    
    private boolean isAllowedFilename(String filename) {
        if (filename == null || filename.isBlank()) return false;
        if (filename.contains("..")) return false; // 双重路径穿越
        String lower = filename.toLowerCase();
        for (String ext : BLOCKED_EXTENSIONS) {
            if (lower.endsWith("." + ext)) return false;
        }
        return true;
    }
    
    private record DataMeta(String id, String filename, String format, long size, String path) {}
}

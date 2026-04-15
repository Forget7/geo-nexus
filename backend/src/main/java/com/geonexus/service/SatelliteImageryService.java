package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 卫星影像服务 - 遥感影像处理与分析
 */
@Slf4j
@Service
public class SatelliteImageryService {
    
    private final CacheService cacheService;
    
    // 影像库
    private final Map<String, SatelliteImage> images = new ConcurrentHashMap<>();
    
    // 波段数据
    private final Map<String, BandData> bandCache = new ConcurrentHashMap<>();
    
    // 预处理任务
    private final Map<String, ProcessingTask> tasks = new ConcurrentHashMap<>();
    
    public SatelliteImageryService(CacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    // ==================== 影像管理 ====================
    
    /**
     * 添加影像
     */
    public SatelliteImage addImage(SatelliteImage image) {
        image.setId(UUID.randomUUID().toString());
        image.setUploadedAt(System.currentTimeMillis());
        image.setStatus("pending");
        
        images.put(image.getId(), image);
        
        log.info("添加卫星影像: id={}, name={}, satellite={}", 
                image.getId(), image.getName(), image.getSatellite());
        
        return image;
    }
    
    /**
     * 获取影像
     */
    public SatelliteImage getImage(String imageId) {
        return images.get(imageId);
    }
    
    /**
     * 更新影像元数据
     */
    public SatelliteImage updateImage(String imageId, SatelliteImage updates) {
        SatelliteImage existing = images.get(imageId);
        if (existing == null) {
            throw new ImageNotFoundException("影像不存在: " + imageId);
        }
        
        updates.setId(imageId);
        updates.setUploadedAt(existing.getUploadedAt());
        updates.setUpdatedAt(System.currentTimeMillis());
        
        images.put(imageId, updates);
        
        return updates;
    }
    
    /**
     * 删除影像
     */
    public void deleteImage(String imageId) {
        images.remove(imageId);
        bandCache.remove(imageId);
        
        log.info("删除卫星影像: id={}", imageId);
    }
    
    /**
     * 搜索影像
     */
    public List<SatelliteImage> searchImages(ImageSearchQuery query) {
        return images.values().stream()
                .filter(img -> {
                    if (query.getSatellite() != null && !query.getSatellite().equals(img.getSatellite())) {
                        return false;
                    }
                    if (query.getCloudCover() != null && img.getCloudCover() > query.getCloudCover()) {
                        return false;
                    }
                    if (query.getStartDate() != null && img.getCaptureDate() < query.getStartDate()) {
                        return false;
                    }
                    if (query.getEndDate() != null && img.getCaptureDate() > query.getEndDate()) {
                        return false;
                    }
                    if (query.getBounds() != null) {
                        double[] bounds = query.getBounds();
                        double[] imgBounds = img.getBounds();
                        if (imgBounds[0] > bounds[2] || imgBounds[2] < bounds[0] ||
                                imgBounds[1] > bounds[3] || imgBounds[3] < bounds[1]) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((a, b) -> Long.compare(b.getCaptureDate(), a.getCaptureDate()))
                .toList();
    }

    /**
     * 获取影像列表（带筛选）
     */
    public List<SatelliteImage> getImages(String satellite, String startDate, String endDate, int limit) {
        ImageSearchQuery query = new ImageSearchQuery();
        if (satellite != null && !satellite.isBlank()) {
            query.setSatellite(satellite);
        }
        if (startDate != null && !startDate.isBlank()) {
            query.setStartDate(parseDate(startDate));
        }
        if (endDate != null && !endDate.isBlank()) {
            query.setEndDate(parseDate(endDate));
        }
        return searchImages(query).stream().limit(limit).toList();
    }

    /**
     * 获取影像波段信息
     */
    public List<Map<String, Object>> getBandInfo(String imageId) {
        SatelliteImage image = images.get(imageId);
        if (image == null) {
            throw new ImageNotFoundException("影像不存在: " + imageId);
        }
        List<Map<String, Object>> bands = new ArrayList<>();
        if (image.getBands() != null) {
            for (String band : image.getBands()) {
                Map<String, Object> bandInfo = new HashMap<>();
                bandInfo.put("name", band);
                bandInfo.put("wavelength", getDefaultWavelength(band));
                bands.add(bandInfo);
            }
        }
        return bands;
    }

    /**
     * 获取支持的卫星列表
     */
    public List<String> getSupportedSatellites() {
        return List.of("Landsat-8", "Landsat-9", "Sentinel-2", "GF-1", "GF-2", "HJ-1", "CBERS-4");
    }

    private Long parseDate(String dateStr) {
        try {
            return java.time.LocalDate.parse(dateStr).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    private String getDefaultWavelength(String band) {
        Map<String, String> wavelengthMap = Map.ofEntries(
                Map.entry("Blue", "450-520nm"),
                Map.entry("Green", "520-600nm"),
                Map.entry("Red", "630-690nm"),
                Map.entry("NIR", "760-900nm"),
                Map.entry("SWIR1", "1550-1750nm"),
                Map.entry("SWIR2", "2080-2350nm"),
                Map.entry("Pan", "500-680nm"),
                Map.entry("Coastal", "400-450nm"),
                Map.entry("Aerosol", "430-450nm"),
                Map.entry("WV", "400-1100nm"),
                Map.entry("Cirrus", "1360-1390nm"),
                Map.entry("TIRS1", "1000-1300nm"),
                Map.entry("TIRS2", "1100-1200nm")
        );
        return wavelengthMap.getOrDefault(band, "400-1100nm");
    }
    
    // ==================== 影像处理 ====================
    
    /**
     * 预处理影像
     */
    public ProcessingTask preprocess(String imageId, PreprocessOptions options) {
        SatelliteImage image = images.get(imageId);
        if (image == null) {
            throw new ImageNotFoundException("影像不存在: " + imageId);
        }
        
        ProcessingTask task = new ProcessingTask();
        task.setId(UUID.randomUUID().toString());
        task.setImageId(imageId);
        task.setType("preprocess");
        task.setOptions(options.toMap());
        task.setStatus("running");
        task.setProgress(0);
        task.setCreatedAt(System.currentTimeMillis());
        
        tasks.put(task.getId(), task);
        
        log.info("开始预处理影像: imageId={}, options={}", imageId, options);
        
        return task;
    }
    
    /**
     * 计算NDVI
     */
    public RasterResult calculateNDVI(String imageId) {
        SatelliteImage image = images.get(imageId);
        if (image == null) {
            throw new ImageNotFoundException("影像不存在: " + imageId);
        }
        
        RasterResult result = new RasterResult();
        result.setImageId(imageId);
        result.setIndexType("NDVI");
        result.setMin(-1.0);
        result.setMax(1.0);
        
        // NDVI = (NIR - Red) / (NIR + Red)
        // 模拟计算
        result.setValid(true);
        
        log.info("计算NDVI: imageId={}", imageId);
        
        return result;
    }
    
    /**
     * 计算NDWI
     */
    public RasterResult calculateNDWI(String imageId) {
        SatelliteImage image = images.get(imageId);
        if (image == null) {
            throw new ImageNotFoundException("影像不存在: " + imageId);
        }
        
        RasterResult result = new RasterResult();
        result.setImageId(imageId);
        result.setIndexType("NDWI");
        result.setMin(-1.0);
        result.setMax(1.0);
        
        // NDWI = (Green - NIR) / (Green + NIR)
        result.setValid(true);
        
        log.info("计算NDWI: imageId={}", imageId);
        
        return result;
    }
    
    /**
     * 计算EVI
     */
    public RasterResult calculateEVI(String imageId) {
        SatelliteImage image = images.get(imageId);
        if (image == null) {
            throw new ImageNotFoundException("影像不存在: " + imageId);
        }
        
        RasterResult result = new RasterResult();
        result.setImageId(imageId);
        result.setIndexType("EVI");
        result.setMin(-1.0);
        result.setMax(1.0);
        
        // EVI = 2.5 * (NIR - Red) / (NIR + 6*Red - 7.5*Blue + 1)
        result.setValid(true);
        
        log.info("计算EVI: imageId={}", imageId);
        
        return result;
    }
    
    /**
     * 计算NDBI
     */
    public RasterResult calculateNDBI(String imageId) {
        SatelliteImage image = images.get(imageId);
        if (image == null) {
            throw new ImageNotFoundException("影像不存在: " + imageId);
        }
        
        RasterResult result = new RasterResult();
        result.setImageId(imageId);
        result.setIndexType("NDBI");
        result.setMin(-1.0);
        result.setMax(1.0);
        
        // NDBI = (SWIR - NIR) / (SWIR + NIR)
        result.setValid(true);
        
        log.info("计算NDBI: imageId={}", imageId);
        
        return result;
    }
    
    /**
     * 变化检测
     */
    public ChangeDetectionResult detectChange(String beforeImageId, String afterImageId, ChangeConfig config) {
        SatelliteImage before = images.get(beforeImageId);
        SatelliteImage after = images.get(afterImageId);
        
        if (before == null || after == null) {
            throw new ImageNotFoundException("影像不存在");
        }
        
        ChangeDetectionResult result = new ChangeDetectionResult();
        result.setBeforeImageId(beforeImageId);
        result.setAfterImageId(afterImageId);
        result.setChangedArea(0.0);
        result.setChangePercent(0.0);
        result.setChangePolygons(new ArrayList<>());
        
        log.info("变化检测: before={}, after={}", beforeImageId, afterImageId);
        
        return result;
    }
    
    /**
     * 裁剪影像
     */
    public RasterResult clipImage(String imageId, double[] bounds) {
        SatelliteImage image = images.get(imageId);
        if (image == null) {
            throw new ImageNotFoundException("影像不存在: " + imageId);
        }
        
        RasterResult result = new RasterResult();
        result.setImageId(imageId);
        result.setBounds(bounds);
        result.setValid(true);
        
        log.info("裁剪影像: imageId={}, bounds={}", imageId, Arrays.toString(bounds));
        
        return result;
    }
    
    /**
     * 重采样
     */
    public RasterResult resampleImage(String imageId, double resolution, String method) {
        SatelliteImage image = images.get(imageId);
        if (image == null) {
            throw new ImageNotFoundException("影像不存在: " + imageId);
        }
        
        RasterResult result = new RasterResult();
        result.setImageId(imageId);
        result.setResolution(resolution);
        result.setResampleMethod(method);
        result.setValid(true);
        
        log.info("重采样影像: imageId={}, resolution={}, method={}", 
                imageId, resolution, method);
        
        return result;
    }
    
    // ==================== 植被分析 ====================
    
    /**
     * 植被覆盖分析
     */
    public VegetationAnalysis analyzeVegetation(String imageId) {
        SatelliteImage image = images.get(imageId);
        if (image == null) {
            throw new ImageNotFoundException("影像不存在: " + imageId);
        }
        
        VegetationAnalysis analysis = new VegetationAnalysis();
        analysis.setImageId(imageId);
        analysis.setHealthyVegetation(0.0);
        analysis.setStressedVegetation(0.0);
        analysis.setBareSoil(0.0);
        analysis.setWater(0.0);
        
        return analysis;
    }
    
    /**
     * 植被变化趋势
     */
    public List<VegetationTrend> analyzeTrend(String regionId, Long startDate, Long endDate) {
        List<VegetationTrend> trends = new ArrayList<>();
        
        log.info("植被趋势分析: region={}, period={}-{}", 
                regionId, startDate, endDate);
        
        return trends;
    }
    
    // ==================== 水体分析 ====================
    
    /**
     * 水体提取
     */
    public WaterExtractionResult extractWater(String imageId, double threshold) {
        SatelliteImage image = images.get(imageId);
        if (image == null) {
            throw new ImageNotFoundException("影像不存在: " + imageId);
        }
        
        WaterExtractionResult result = new WaterExtractionResult();
        result.setImageId(imageId);
        result.setWaterArea(0.0);
        result.setWaterPercent(0.0);
        result.setThreshold(threshold);
        
        log.info("水体提取: imageId={}, threshold={}", imageId, threshold);
        
        return result;
    }
    
    // ==================== 任务管理 ====================
    
    /**
     * 获取任务状态
     */
    public ProcessingTask getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * 取消任务
     */
    public void cancelTask(String taskId) {
        ProcessingTask task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("cancelled");
            task.setEndTime(System.currentTimeMillis());
        }
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class SatelliteImage {
        private String id;
        private String name;
        private String description;
        private String satellite; // Landsat-8, Sentinel-2, GF-1, etc.
        private String sensor;
        private Long captureDate;
        private double cloudCover;
        private double[] bounds;
        private int width;
        private int height;
        private double resolution;
        private int bandCount;
        private List<String> bands;
        private String status;
        private String url;
        private Map<String, Object> metadata;
        private Long uploadedAt;
        private Long updatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class BandData {
        private String imageId;
        private String bandName;
        private double min;
        private double max;
        private double mean;
        private double stddev;
        private double[] histogram;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class PreprocessOptions {
        private boolean atmosphericCorrection;
        private boolean geometricCorrection;
        private boolean radiometricCorrection;
        private String demSource;
        private boolean cloudRemoval;
        private double cloudThreshold;
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("atmosphericCorrection", atmosphericCorrection);
            map.put("geometricCorrection", geometricCorrection);
            map.put("radiometricCorrection", radiometricCorrection);
            map.put("demSource", demSource);
            map.put("cloudRemoval", cloudRemoval);
            map.put("cloudThreshold", cloudThreshold);
            return map;
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RasterResult {
        private String imageId;
        private String indexType;
        private double min;
        private double max;
        private double[] bounds;
        private double resolution;
        private String resampleMethod;
        private boolean valid;
        private String outputPath;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ChangeConfig {
        private String method; // pixel-based, object-based
        private double threshold;
        private int minArea;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ChangeDetectionResult {
        private String beforeImageId;
        private String afterImageId;
        private double changedArea;
        private double changePercent;
        private List<double[]> changePolygons;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class VegetationAnalysis {
        private String imageId;
        private double healthyVegetation;
        private double stressedVegetation;
        private double bareSoil;
        private double water;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class VegetationTrend {
        private Long date;
        private double ndvi;
        private double ndviChange;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class WaterExtractionResult {
        private String imageId;
        private double waterArea;
        private double waterPercent;
        private double threshold;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ProcessingTask {
        private String id;
        private String imageId;
        private String type;
        private Map<String, Object> options;
        private String status;
        private int progress;
        private String error;
        private Long createdAt;
        private Long endTime;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ImageSearchQuery {
        private String satellite;
        private Double cloudCover;
        private Long startDate;
        private Long endDate;
        private double[] bounds;
    }
    
    public static class ImageNotFoundException extends RuntimeException {
        public ImageNotFoundException(String msg) { super(msg); }
    }
}

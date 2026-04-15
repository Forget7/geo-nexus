package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 流式分析服务 - 实时数据流处理
 */
@Slf4j
@Service
public class StreamingAnalyticsService {
    
    private final CacheService cacheService;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    
    // 数据流
    private final Map<String, DataStream> streams = new ConcurrentHashMap<>();
    
    // 窗口
    private final Map<String, StreamWindow> windows = new ConcurrentHashMap<>();
    
    // 实时指标
    private final Map<String, MetricValue> metrics = new ConcurrentHashMap<>();
    
    // 事件缓冲区
    private final Map<String, List<StreamEvent>> eventBuffers = new ConcurrentHashMap<>();
    
    public StreamingAnalyticsService(CacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    // ==================== 数据流管理 ====================
    
    /**
     * 创建数据流
     */
    public DataStream createStream(DataStream stream) {
        stream.setId(UUID.randomUUID().toString());
        stream.setCreatedAt(System.currentTimeMillis());
        stream.setStatus("active");
        
        streams.put(stream.getId(), stream);
        eventBuffers.put(stream.getId(), new CopyOnWriteArrayList<>());
        
        // 启动流处理
        startStreamProcessing(stream);
        
        log.info("创建数据流: id={}, name={}, type={}", 
                stream.getId(), stream.getName(), stream.getStreamType());
        
        return stream;
    }
    
    /**
     * 获取数据流
     */
    public DataStream getStream(String streamId) {
        return streams.get(streamId);
    }
    
    /**
     * 删除数据流
     */
    public void deleteStream(String streamId) {
        streams.remove(streamId);
        eventBuffers.remove(streamId);
        windows.remove(streamId);
        
        log.info("删除数据流: id={}", streamId);
    }
    
    // ==================== 事件处理 ====================
    
    /**
     * 发送事件
     */
    public void sendEvent(String streamId, StreamEvent event) {
        DataStream stream = streams.get(streamId);
        if (stream == null) {
            throw new StreamNotFoundException("数据流不存在: " + streamId);
        }
        
        event.setId(UUID.randomUUID().toString());
        event.setStreamId(streamId);
        event.setTimestamp(System.currentTimeMillis());
        
        // 添加到缓冲区
        List<StreamEvent> buffer = eventBuffers.get(streamId);
        if (buffer != null) {
            buffer.add(event);
            
            // 更新指标
            updateMetrics(streamId, event);
        }
        
        // 触发窗口计算
        processWindow(streamId, event);
        
        log.debug("接收事件: streamId={}, type={}", streamId, event.getEventType());
    }
    
    /**
     * 批量发送事件
     */
    public void sendEvents(String streamId, List<StreamEvent> events) {
        for (StreamEvent event : events) {
            sendEvent(streamId, event);
        }
    }
    
    /**
     * 获取实时事件
     */
    public List<StreamEvent> getRecentEvents(String streamId, int limit) {
        List<StreamEvent> buffer = eventBuffers.get(streamId);
        if (buffer == null) return Collections.emptyList();
        
        int size = buffer.size();
        int from = Math.max(0, size - limit);
        
        return new ArrayList<>(buffer.subList(from, size));
    }
    
    // ==================== 窗口处理 ====================
    
    /**
     * 创建窗口
     */
    public StreamWindow createWindow(StreamWindow window) {
        window.setId(UUID.randomUUID().toString());
        window.setCreatedAt(System.currentTimeMillis());
        window.setStatus("active");
        
        windows.put(window.getId(), window);
        
        log.info("创建流窗口: id={}, type={}, size={}", 
                window.getId(), window.getWindowType(), window.getSize());
        
        return window;
    }
    
    /**
     * 获取窗口结果
     */
    public WindowResult getWindowResult(String windowId) {
        StreamWindow window = windows.get(windowId);
        if (window == null) {
            throw new WindowNotFoundException("窗口不存在: " + windowId);
        }
        
        List<StreamEvent> buffer = eventBuffers.get(window.getStreamId());
        if (buffer == null) {
            return new WindowResult();
        }
        
        // 获取时间范围内的事件
        long now = System.currentTimeMillis();
        long windowStart = now - window.getSize();
        
        List<StreamEvent> windowEvents = buffer.stream()
                .filter(e -> e.getTimestamp() >= windowStart)
                .toList();
        
        // 计算窗口结果
        return calculateWindowResult(window, windowEvents);
    }
    
    private WindowResult calculateWindowResult(StreamWindow window, List<StreamEvent> events) {
        WindowResult result = new WindowResult();
        result.setWindowId(window.getId());
        result.setStartTime(events.isEmpty() ? System.currentTimeMillis() : events.get(0).getTimestamp());
        result.setEndTime(events.isEmpty() ? System.currentTimeMillis() : events.get(events.size() - 1).getTimestamp());
        result.setEventCount(events.size());
        
        switch (window.getWindowType()) {
            case "tumbling":
                result.setType("tumbling");
                result.setSum(calculateSum(events, window.getAggregationField()));
                result.setAvg(calculateAvg(events, window.getAggregationField()));
                result.setMin(calculateMin(events, window.getAggregationField()));
                result.setMax(calculateMax(events, window.getAggregationField()));
                result.setCount(events.size());
                break;
                
            case "sliding":
                result.setType("sliding");
                result.setSum(calculateSum(events, window.getAggregationField()));
                result.setAvg(calculateAvg(events, window.getAggregationField()));
                break;
                
            case "session":
                result.setType("session");
                result.setEventCount(events.size());
                result.setSessionDuration(calculateSessionDuration(events));
                break;
                
            default:
                result.setType(window.getWindowType());
        }
        
        return result;
    }
    
    private double calculateSum(List<StreamEvent> events, String field) {
        return events.stream()
                .filter(e -> e.getData().containsKey(field))
                .mapToDouble(e -> ((Number) e.getData().get(field)).doubleValue())
                .sum();
    }
    
    private double calculateAvg(List<StreamEvent> events, String field) {
        return events.stream()
                .filter(e -> e.getData().containsKey(field))
                .mapToDouble(e -> ((Number) e.getData().get(field)).doubleValue())
                .average()
                .orElse(0);
    }
    
    private double calculateMin(List<StreamEvent> events, String field) {
        return events.stream()
                .filter(e -> e.getData().containsKey(field))
                .mapToDouble(e -> ((Number) e.getData().get(field)).doubleValue())
                .min()
                .orElse(0);
    }
    
    private double calculateMax(List<StreamEvent> events, String field) {
        return events.stream()
                .filter(e -> e.getData().containsKey(field))
                .mapToDouble(e -> ((Number) e.getData().get(field)).doubleValue())
                .max()
                .orElse(0);
    }
    
    private long calculateSessionDuration(List<StreamEvent> events) {
        if (events.isEmpty()) return 0;
        return events.get(events.size() - 1).getTimestamp() - events.get(0).getTimestamp();
    }
    
    // ==================== 指标计算 ====================
    
    private void updateMetrics(String streamId, StreamEvent event) {
        // 更新事件计数
        String countKey = streamId + ":count";
        MetricValue count = metrics.computeIfAbsent(countKey, k -> new MetricValue());
        count.setValue(count.getValue() + 1);
        count.setTimestamp(System.currentTimeMillis());
        
        // 更新速度指标
        String rateKey = streamId + ":rate";
        MetricValue rate = metrics.computeIfAbsent(rateKey, k -> new MetricValue());
        rate.setValue(calculateRate(streamId));
        rate.setTimestamp(System.currentTimeMillis());
    }
    
    private double calculateRate(String streamId) {
        List<StreamEvent> buffer = eventBuffers.get(streamId);
        if (buffer == null || buffer.size() < 2) return 0;
        
        StreamEvent first = buffer.get(0);
        StreamEvent last = buffer.get(buffer.size() - 1);
        
        long timeDiff = last.getTimestamp() - first.getTimestamp();
        if (timeDiff == 0) return 0;
        
        return (double) buffer.size() / timeDiff * 1000; // events per second
    }
    
    /**
     * 获取实时指标
     */
    public Map<String, MetricValue> getMetrics(String streamId) {
        Map<String, MetricValue> result = new HashMap<>();
        
        for (Map.Entry<String, MetricValue> entry : metrics.entrySet()) {
            if (entry.getKey().startsWith(streamId + ":")) {
                result.put(entry.getKey().substring(streamId.length() + 1), entry.getValue());
            }
        }
        
        return result;
    }
    
    // ==================== 告警 ====================
    
    /**
     * 创建告警规则
     */
    public void createAlertRule(String streamId, AlertRule rule) {
        rule.setId(UUID.randomUUID().toString());
        rule.setStreamId(streamId);
        rule.setCreatedAt(System.currentTimeMillis());
        
        // 启动告警监控
        executor.scheduleAtFixedRate(() -> checkAlerts(streamId, rule), 
                0, rule.getCheckInterval(), TimeUnit.SECONDS);
        
        log.info("创建告警规则: streamId={}, condition={}", streamId, rule.getCondition());
    }
    
    private void checkAlerts(String streamId, AlertRule rule) {
        MetricValue metric = metrics.get(streamId + ":" + rule.getMetric());
        if (metric == null) return;
        
        boolean triggered = false;
        
        switch (rule.getCondition()) {
            case "gt":
                triggered = metric.getValue() > rule.getThreshold();
                break;
            case "lt":
                triggered = metric.getValue() < rule.getThreshold();
                break;
            case "eq":
                triggered = Math.abs(metric.getValue() - rule.getThreshold()) < 0.0001;
                break;
        }
        
        if (triggered) {
            log.info("触发告警: streamId={}, rule={}, value={}", 
                    streamId, rule.getName(), metric.getValue());
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private void startStreamProcessing(DataStream stream) {
        // 启动缓冲清理
        executor.scheduleAtFixedRate(() -> {
            cleanBuffer(stream.getId(), stream.getBufferSize());
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    private void cleanBuffer(String streamId, int maxSize) {
        List<StreamEvent> buffer = eventBuffers.get(streamId);
        if (buffer != null && buffer.size() > maxSize) {
            int removeCount = buffer.size() - maxSize;
            for (int i = 0; i < removeCount; i++) {
                buffer.remove(0);
            }
            log.debug("清理事件缓冲: streamId={}, removed={}", streamId, removeCount);
        }
    }
    
    private void processWindow(String streamId, StreamEvent event) {
        for (StreamWindow window : windows.values()) {
            if (window.getStreamId().equals(streamId) && "active".equals(window.getStatus())) {
                // 窗口事件处理
                log.debug("处理窗口: windowId={}", window.getId());
            }
        }
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class DataStream {
        private String id;
        private String name;
        private String streamType; // vehicle, sensor, user, custom
        private String source;
        private int bufferSize;
        private String status;
        private Long createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class StreamEvent {
        private String id;
        private String streamId;
        private String eventType;
        private double[] location;
        private Map<String, Object> data;
        private Long timestamp;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class StreamWindow {
        private String id;
        private String streamId;
        private String windowType; // tumbling, sliding, session
        private long size; // milliseconds
        private long slide; // milliseconds
        private String aggregationField;
        private String status;
        private Long createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class WindowResult {
        private String windowId;
        private String type;
        private Long startTime;
        private Long endTime;
        private int eventCount;
        private double sum;
        private double avg;
        private double min;
        private double max;
        private long sessionDuration;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MetricValue {
        private double value;
        private Long timestamp;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AlertRule {
        private String id;
        private String streamId;
        private String name;
        private String metric;
        private String condition; // gt, lt, eq
        private double threshold;
        private int checkInterval; // seconds
        private Long createdAt;
    }
    
    public static class StreamNotFoundException extends RuntimeException {
        public StreamNotFoundException(String msg) { super(msg); }
    }
    
    public static class WindowNotFoundException extends RuntimeException {
        public WindowNotFoundException(String msg) { super(msg); }
    }
}

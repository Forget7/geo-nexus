package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.service.StreamingAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流式分析API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "流式分析", description = "实时数据分析与统计")
public class AnalyticsController {

    private final StreamingAnalyticsService analyticsService;

    @GetMapping("/streams")
    @Operation(summary = "列出数据流", description = "获取所有活跃的数据流信息")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStreams() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping("/streams")
    @Operation(summary = "创建数据流")
    public ResponseEntity<ApiResponse<StreamingAnalyticsService.DataStream>> createStream(
            @RequestBody StreamingAnalyticsService.DataStream stream) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.createStream(stream)));
    }

    @GetMapping("/streams/{streamId}")
    @Operation(summary = "获取数据流")
    public ResponseEntity<ApiResponse<StreamingAnalyticsService.DataStream>> getStream(@PathVariable String streamId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getStream(streamId)));
    }

    @DeleteMapping("/streams/{streamId}")
    @Operation(summary = "删除数据流")
    public ResponseEntity<ApiResponse<Void>> deleteStream(@PathVariable String streamId) {
        analyticsService.deleteStream(streamId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/events")
    @Operation(summary = "提交事件")
    public ResponseEntity<ApiResponse<Void>> sendEvent(@RequestBody Map<String, Object> eventBody) {
        String streamId = (String) eventBody.getOrDefault("streamId", "default");
        StreamingAnalyticsService.StreamEvent event = StreamingAnalyticsService.StreamEvent.builder()
                .eventType((String) eventBody.getOrDefault("type", "custom"))
                .data(eventBody)
                .build();
        analyticsService.sendEvent(streamId, event);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/events")
    @Operation(summary = "查询事件")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEvents(
            @RequestParam(required = false) String streamId,
            @RequestParam(defaultValue = "50") int limit) {
        List<StreamingAnalyticsService.StreamEvent> events;
        if (streamId != null && !streamId.isEmpty()) {
            events = analyticsService.getRecentEvents(streamId, limit);
        } else {
            events = List.of();
        }
        List<Map<String, Object>> result = events.stream()
                .map(e -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", e.getId());
                    m.put("streamId", e.getStreamId());
                    m.put("type", e.getEventType());
                    m.put("timestamp", e.getTimestamp());
                    m.put("data", e.getData());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/events/batch")
    @Operation(summary = "批量提交事件")
    public ResponseEntity<ApiResponse<Void>> sendEvents(
            @RequestParam String streamId,
            @RequestBody List<Map<String, Object>> eventList) {
        List<StreamingAnalyticsService.StreamEvent> events = eventList.stream()
                .map(m -> StreamingAnalyticsService.StreamEvent.builder()
                        .eventType((String) m.getOrDefault("type", "custom"))
                        .data(m)
                        .build())
                .collect(Collectors.toList());
        analyticsService.sendEvents(streamId, events);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/metrics")
    @Operation(summary = "获取实时指标")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMetrics(@RequestParam String streamId) {
        Map<String, StreamingAnalyticsService.MetricValue> raw = analyticsService.getMetrics(streamId);
        Map<String, Object> converted = raw.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Map.of("value", e.getValue().getValue(), "timestamp", e.getValue().getTimestamp())
                ));
        return ResponseEntity.ok(ApiResponse.success(converted));
    }

    @GetMapping("/windows/{windowId}")
    @Operation(summary = "获取窗口结果")
    public ResponseEntity<ApiResponse<StreamingAnalyticsService.WindowResult>> getWindowResult(@PathVariable String windowId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getWindowResult(windowId)));
    }

    @PostMapping("/windows")
    @Operation(summary = "创建窗口")
    public ResponseEntity<ApiResponse<StreamingAnalyticsService.StreamWindow>> createWindow(
            @RequestBody StreamingAnalyticsService.StreamWindow window) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.createWindow(window)));
    }
}

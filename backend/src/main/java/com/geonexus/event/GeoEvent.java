package com.geonexus.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 地理事件 - 通用地理事件模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoEvent {
    /** 事件唯一 ID */
    private String eventId;

    /** 事件类型 */
    private String eventType;

    /** 设备/传感器 ID */
    private String sourceId;

    /** 事件时间 */
    private Instant timestamp;

    /** 经度 */
    private Double longitude;

    /** 纬度 */
    private Double latitude;

    /** 高度（可选） */
    private Double altitude;

    /** 事件数据 */
    private Map<String, Object> properties;

    /** 坐标系（默认 EPSG:4326） */
    private String crs;
}

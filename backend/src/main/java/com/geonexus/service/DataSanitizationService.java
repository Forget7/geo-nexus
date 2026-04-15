package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据脱敏服务
 *
 * 支持的脱敏规则：
 * - 手机号：138****5678
 * - 邮箱：a***@domain.com
 * - 坐标精度降级：保留3位小数（约111米精度）
 * - 门牌号：xx号
 */
@Slf4j
@Service
public class DataSanitizationService {

    // 手机号正则（1开头的11位数字）
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<before>1[3-9]\\d{2})(\\d{4})(\\d{4})");
    // 邮箱正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(^[^@]+)(@.+$)");
    // 坐标精度（3位小数）
    private static final int COORD_PRECISION = 3;
    // 门牌号正则
    private static final Pattern ADDRESS_NUMBER_PATTERN = Pattern.compile("(\\d+)号");

    /**
     * 按规则脱敏GIS数据
     *
     * @param gisData 原始GIS数据（GeoJSON格式的Map）
     * @param rules   启用的脱敏规则，如 ["phone", "email", "coordinate", "address"]
     * @return 脱敏后的GIS数据
     */
    public Map<String, Object> sanitize(Map<String, Object> gisData, Map<String, Boolean> rules) {
        Map<String, Object> sanitized = new HashMap<>(gisData);

        boolean sanitizePhone = rules.getOrDefault("phone", true);
        boolean sanitizeEmail = rules.getOrDefault("email", true);
        boolean sanitizeCoord = rules.getOrDefault("coordinate", true);
        boolean sanitizeAddress = rules.getOrDefault("address", true);

        // 处理FeatureCollection
        if ("FeatureCollection".equals(gisData.get("type"))) {
            @SuppressWarnings("unchecked")
            var features = (java.util.List<Map<String, Object>>) sanitized.get("features");
            if (features != null) {
                for (Map<String, Object> feature : features) {
                    sanitizeFeature(feature, sanitizePhone, sanitizeEmail, sanitizeCoord, sanitizeAddress);
                }
            }
        }
        // 处理单个Feature
        else if ("Feature".equals(gisData.get("type"))) {
            sanitizeFeature(sanitized, sanitizePhone, sanitizeEmail, sanitizeCoord, sanitizeAddress);
        }

        // 特殊处理properties中的字段
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) sanitized.get("properties");
        if (props != null) {
            sanitizeProperties(props, sanitizePhone, sanitizeEmail, sanitizeCoord, sanitizeAddress);
        }

        sanitized.put("_sanitized", true);
        sanitized.put("_sanitizedAt", System.currentTimeMillis());

        return sanitized;
    }

    private void sanitizeFeature(Map<String, Object> feature,
                                  boolean sanitizePhone, boolean sanitizeEmail,
                                  boolean sanitizeCoord, boolean sanitizeAddress) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) feature.get("properties");
        if (props != null) {
            sanitizeProperties(props, sanitizePhone, sanitizeEmail, sanitizeCoord, sanitizeAddress);
        }

        // 处理geometry中的坐标
        @SuppressWarnings("unchecked")
        Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
        if (geometry != null && sanitizeCoord) {
            sanitizeGeometry(geometry);
        }
    }

    private void sanitizeProperties(Map<String, Object> props,
                                     boolean sanitizePhone, boolean sanitizeEmail,
                                     boolean sanitizeCoord, boolean sanitizeAddress) {
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Object value = entry.getValue();

            if (value == null) continue;

            if (sanitizePhone && isPhoneField(key) && value instanceof String phone) {
                props.put(entry.getKey(), maskPhone(phone));
            } else if (sanitizeEmail && isEmailField(key) && value instanceof String email) {
                props.put(entry.getKey(), maskEmail(email));
            } else if (sanitizeAddress && isAddressField(key) && value instanceof String address) {
                props.put(entry.getKey(), maskAddressNumber(address));
            } else if (sanitizeCoord && isCoordField(key) && value instanceof Number num) {
                props.put(entry.getKey(), roundCoord(num.doubleValue()));
            }
        }
    }

    private void sanitizeGeometry(Map<String, Object> geometry) {
        String type = (String) geometry.get("type");
        if (type == null) return;

        if ("Point".equals(type)) {
            @SuppressWarnings("unchecked")
            var coords = (java.util.List<Number>) geometry.get("coordinates");
            if (coords != null && coords.size() >= 2) {
                coords.set(0, roundCoord(coords.get(0).doubleValue()));
                coords.set(1, roundCoord(coords.get(1).doubleValue()));
            }
        } else if ("LineString".equals(type) || "Polygon".equals(type)) {
            @SuppressWarnings("unchecked")
            var rings = (java.util.List<?> ) geometry.get("coordinates");
            if (rings != null) {
                for (Object ring : rings) {
                    sanitizeCoordList(ring);
                }
            }
        } else if ("MultiPoint".equals(type) || "MultiLineString".equals(type)) {
            @SuppressWarnings("unchecked")
            var lines = (java.util.List<?> ) geometry.get("coordinates");
            if (lines != null) {
                for (Object line : lines) {
                    sanitizeCoordList(line);
                }
            }
        } else if ("MultiPolygon".equals(type)) {
            @SuppressWarnings("unchecked")
            var polys = (java.util.List<?> ) geometry.get("coordinates");
            if (polys != null) {
                for (Object poly : polys) {
                    sanitizeCoordList(poly);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void sanitizeCoordList(Object obj) {
        if (obj instanceof java.util.List<?> list) {
            for (Object item : list) {
                if (item instanceof java.util.List<?> coords && coords.size() >= 2) {
                    if (coords.get(0) instanceof Number n0 && coords.get(1) instanceof Number n1) {
                        ((java.util.List<Number>) coords).set(0, roundCoord(n0.doubleValue()));
                        ((java.util.List<Number>) coords).set(1, roundCoord(n1.doubleValue()));
                    }
                } else {
                    sanitizeCoordList(item);
                }
            }
        }
    }

    // ── 脱敏规则实现 ─────────────────────────────────

    /** 手机号：138****5678 */
    public String maskPhone(String phone) {
        if (phone == null) return null;
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$3");
    }

    /** 邮箱：a***@domain.com */
    public String maskEmail(String email) {
        if (email == null) return null;
        return EMAIL_PATTERN.matcher(email).replaceAll(match -> {
            String local = match.group(1);
            String domain = match.group(2);
            if (local.length() <= 1) {
                return local + "***" + domain;
            }
            return local.charAt(0) + "***" + domain;
        });
    }

    /** 坐标精度降级：保留3位小数（约111米精度） */
    public BigDecimal roundCoord(double value) {
        return BigDecimal.valueOf(value)
                .setScale(COORD_PRECISION, RoundingMode.HALF_UP);
    }

    /** 门牌号：xx号 */
    public String maskAddressNumber(String address) {
        if (address == null) return null;
        return ADDRESS_NUMBER_PATTERN.matcher(address).replaceAll("xx号");
    }

    // ── 字段识别 ─────────────────────────────────────

    private boolean isPhoneField(String key) {
        return key.contains("phone") || key.contains("mobile") || key.contains("tel");
    }

    private boolean isEmailField(String key) {
        return key.contains("email") || key.contains("mail");
    }

    private boolean isAddressField(String key) {
        return key.contains("address") && (key.contains("num") || key.contains("door") || key.contains("号"));
    }

    private boolean isCoordField(String key) {
        return key.equals("lon") || key.equals("lng") || key.equals("longitude") ||
               key.equals("lat") || key.equals("latitude") ||
               key.equals("x") || key.equals("y");
    }
}

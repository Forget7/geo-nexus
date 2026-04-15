package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务 - Redis缓存封装
 */
@Slf4j
@Service
public class CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 缓存key前缀
    public static final String PREFIX_MAP = "geonexus:map:";
    public static final String PREFIX_SESSION = "geonexus:session:";
    public static final String PREFIX_TOOL = "geonexus:tool:";
    public static final String PREFIX_GEOJSON = "geonexus:geojson:";
    public static final String PREFIX_RATE = "geonexus:rate:";
    
    // 默认过期时间
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    public static final Duration MAP_TTL = Duration.ofHours(24);
    public static final Duration SESSION_TTL = Duration.ofDays(7);
    
    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_TTL);
    }
    
    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("缓存设置成功: {}, TTL: {}", key, ttl);
        } catch (Exception e) {
            log.error("缓存设置失败: {}", key, e);
        }
    }
    
    /**
     * 设置缓存（秒）
     */
    public void setEx(String key, Object value, long seconds) {
        set(key, value, Duration.ofSeconds(seconds));
    }
    
    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("缓存命中: {}", key);
                return (T) value;
            }
            log.debug("缓存未命中: {}", key);
            return null;
        } catch (Exception e) {
            log.error("缓存获取失败: {}", key, e);
            return null;
        }
    }
    
    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("缓存删除: {}, result: {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("缓存删除失败: {}", key, e);
            return false;
        }
    }
    
    /**
     * 检查key是否存在
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("缓存检查失败: {}", key, e);
            return false;
        }
    }
    
    /**
     * 设置过期时间
     */
    public boolean expire(String key, Duration ttl) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, ttl.toSeconds(), TimeUnit.SECONDS));
        } catch (Exception e) {
            log.error("设置过期时间失败: {}", key, e);
            return false;
        }
    }
    
    /**
     * 获取过期时间
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            log.error("获取过期时间失败: {}", key, e);
            return -1;
        }
    }
    
    // ==================== 业务方法 ====================
    
    /**
     * 缓存地图
     */
    public void cacheMap(String mapId, Object mapData) {
        set(PREFIX_MAP + mapId, mapData, MAP_TTL);
    }
    
    /**
     * 获取缓存地图
     */
    public Object getCachedMap(String mapId) {
        return get(PREFIX_MAP + mapId);
    }
    
    /**
     * 删除缓存地图
     */
    public boolean deleteCachedMap(String mapId) {
        return delete(PREFIX_MAP + mapId);
    }
    
    /**
     * 缓存会话
     */
    public void cacheSession(String sessionId, Object sessionData) {
        set(PREFIX_SESSION + sessionId, sessionData, SESSION_TTL);
    }
    
    /**
     * 获取缓存会话
     */
    public Object getCachedSession(String sessionId) {
        return get(PREFIX_SESSION + sessionId);
    }
    
    /**
     * 删除缓存会话
     */
    public boolean deleteCachedSession(String sessionId) {
        return delete(PREFIX_SESSION + sessionId);
    }
    
    /**
     * 缓存GeoJSON
     */
    public void cacheGeoJSON(String hash, Object geojson) {
        set(PREFIX_GEOJSON + hash, geojson, MAP_TTL);
    }
    
    /**
     * 获取缓存GeoJSON
     */
    public Object getCachedGeoJSON(String hash) {
        return get(PREFIX_GEOJSON + hash);
    }
    
    /**
     * 限流检查
     */
    public boolean isRateLimited(String clientId, int maxRequests, Duration window) {
        String key = PREFIX_RATE + clientId;
        
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) return false;
            
            if (count == 1) {
                // 第一次请求，设置过期时间
                redisTemplate.expire(key, window.toSeconds(), TimeUnit.SECONDS);
            }
            
            return count > maxRequests;
        } catch (Exception e) {
            log.error("限流检查失败: {}", clientId, e);
            return false; // 失败时允许通过
        }
    }
    
    /**
     * 获取限流剩余次数
     */
    public int getRateLimitRemaining(String clientId, int maxRequests) {
        String key = PREFIX_RATE + clientId;
        
        try {
            Object count = redisTemplate.opsForValue().get(key);
            if (count == null) return maxRequests;
            
            int current = ((Number) count).intValue();
            return Math.max(0, maxRequests - current);
        } catch (Exception e) {
            return maxRequests;
        }
    }
    
    /**
     * 清空所有缓存
     */
    public void flushAll() {
        try {
            redisTemplate.execute((connection) -> {
                connection.flushDb();
                return null;
            });
            log.info("缓存已清空");
        } catch (Exception e) {
            log.error("清空缓存失败", e);
        }
    }
}

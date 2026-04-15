-- GeoNexus V2 数据迁移 - 完整初始化
-- 执行: psql -d geonexus -f V2__seed_data.sql

-- ========== 启用扩展 ==========
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- 模糊搜索

-- ========== 创建表 ==========

-- 会话表
CREATE TABLE IF NOT EXISTS chat_sessions (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    title VARCHAR(255),
    user_id VARCHAR(64) NOT NULL DEFAULT 'default',
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    context JSONB DEFAULT '{}',
    metadata JSONB DEFAULT '{}',
    message_count INTEGER DEFAULT 0,
    starred BOOLEAN DEFAULT false,
    archived BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_message_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_chat_sessions_user_status ON chat_sessions(user_id, status);
CREATE INDEX idx_chat_sessions_updated ON chat_sessions(updated_at DESC);

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    session_id VARCHAR(36) NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant', 'system', 'tool')),
    content TEXT NOT NULL,
    model VARCHAR(50),
    tokens_used INTEGER,
    has_attachments BOOLEAN DEFAULT false,
    attachments JSONB DEFAULT '[]',
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT fk_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
);

CREATE INDEX idx_messages_session_created ON messages(session_id, created_at);
CREATE INDEX idx_messages_role ON messages(role);

-- GIS数据表
CREATE TABLE IF NOT EXISTS gis_data (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    data_type VARCHAR(20) NOT NULL CHECK (data_type IN ('vector', 'raster', 'tabular')),
    format VARCHAR(20) NOT NULL CHECK (format IN ('GeoJSON', 'Shapefile', 'KML', 'GML', 'GPX', 'CSV', 'GeoTIFF', 'MBTiles')),
    size_bytes BIGINT,
    crs VARCHAR(50),
    geojson JSONB,
    properties_schema JSONB,
    metadata JSONB DEFAULT '{}',
    tags TEXT[] DEFAULT '{}',
    user_id VARCHAR(64) NOT NULL DEFAULT 'default',
    is_public BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_gis_data_user_type ON gis_data(user_id, data_type);
CREATE INDEX idx_gis_data_tags ON gis_data USING GIN(tags);
CREATE INDEX idx_gis_data_name_trgm ON gis_data USING GIN(name gin_trgm_ops);

-- 地图文档表
CREATE TABLE IF NOT EXISTS map_documents (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    map_type VARCHAR(20) NOT NULL DEFAULT '2d',
    config JSONB NOT NULL DEFAULT '{}',
    layers JSONB DEFAULT '[]',
    style JSONB DEFAULT '{}',
    user_id VARCHAR(64) NOT NULL DEFAULT 'default',
    is_public BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_map_documents_user ON map_documents(user_id);

-- 异步任务表
CREATE TABLE IF NOT EXISTS async_tasks (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    name VARCHAR(255) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'running', 'completed', 'failed', 'cancelled', 'timeout')),
    priority INTEGER DEFAULT 5,
    progress INTEGER DEFAULT 0,
    progress_message VARCHAR(255),
    input_data JSONB DEFAULT '{}',
    output_data JSONB,
    error_message TEXT,
    user_id VARCHAR(64) NOT NULL DEFAULT 'default',
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_async_tasks_status ON async_tasks(status);
CREATE INDEX idx_async_tasks_user_status ON async_tasks(user_id, status);

-- ========== 触发器 ==========

-- updated_at自动更新
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_chat_sessions_updated_at
    BEFORE UPDATE ON chat_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_gis_data_updated_at
    BEFORE UPDATE ON gis_data
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_map_documents_updated_at
    BEFORE UPDATE ON map_documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- message_count自动更新
CREATE OR REPLACE FUNCTION update_message_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE chat_sessions 
        SET message_count = message_count + 1,
            last_message_at = NEW.created_at
        WHERE id = NEW.session_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE chat_sessions 
        SET message_count = message_count - 1
        WHERE id = OLD.session_id;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_session_message_count
    AFTER INSERT OR DELETE ON messages
    FOR EACH ROW EXECUTE FUNCTION update_message_count();

-- ========== 种子数据 ==========

-- 示例GIS数据
INSERT INTO gis_data (id, name, description, data_type, format, size_bytes, crs, geojson, tags, user_id) VALUES
(
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    '北京市主要地标',
    '北京市主要旅游景点和地标建筑位置',
    'vector',
    'GeoJSON',
    15420,
    'EPSG:4326',
    '{
        "type": "FeatureCollection",
        "features": [
            {
                "type": "Feature",
                "geometry": {"type": "Point", "coordinates": [116.3972, 39.9165]},
                "properties": {"name": "天安门", "category": "广场", "visitors": 200000}
            },
            {
                "type": "Feature",
                "geometry": {"type": "Point", "coordinates": [116.4122, 39.9485]},
                "properties": {"name": "故宫", "category": "宫殿", "visitors": 80000}
            },
            {
                "type": "Feature",
                "geometry": {"type": "Point", "coordinates": [116.4074, 39.9042]},
                "properties": {"name": "天坛", "category": "寺庙", "visitors": 50000}
            },
            {
                "type": "Feature",
                "geometry": {"type": "Point", "coordinates": [116.2377, 39.9406]},
                "properties": {"name": "颐和园", "category": "园林", "visitors": 40000}
            }
        ]
    }'::jsonb,
    ARRAY['北京', '地标', '旅游'],
    'default'
);

INSERT INTO gis_data (id, name, description, data_type, format, size_bytes, crs, geojson, tags, user_id) VALUES
(
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    '上海市主要地标',
    '上海市主要旅游景点和地标建筑位置',
    'vector',
    'GeoJSON',
    12350,
    'EPSG:4326',
    '{
        "type": "FeatureCollection",
        "features": [
            {
                "type": "Feature",
                "geometry": {"type": "Point", "coordinates": [121.4737, 31.2304]},
                "properties": {"name": "外滩", "category": "景观", "visitors": 300000}
            },
            {
                "type": "Feature",
                "geometry": {"type": "Point", "coordinates": [121.4990, 31.2412]},
                "properties": {"name": "东方明珠", "category": "建筑", "visitors": 100000}
            },
            {
                "type": "Feature",
                "geometry": {"type": "Point", "coordinates": [121.4018, 31.1812]},
                "properties": {"name": "豫园", "category": "园林", "visitors": 60000}
            }
        ]
    }'::jsonb,
    ARRAY['上海', '地标', '旅游'],
    'default'
);

-- 示例地图配置
INSERT INTO map_documents (id, name, description, map_type, config, layers, user_id) VALUES
(
    'c3d4e5f6-a7b8-9012-cdef-123456789012',
    '京津冀地区地图',
    '展示北京、天津、河北地区的主要城市和交通网络',
    '2d',
    '{
        "center": [116.4, 39.9],
        "zoom": 8,
        "tileType": "osm",
        "minZoom": 5,
        "maxZoom": 18,
        "bounds": [[115.0, 38.0], [118.0, 42.0]]
    }'::jsonb,
    '[
        {
            "id": "cities",
            "name": "主要城市",
            "type": "geojson",
            "visible": true,
            "opacity": 1.0,
            "style": {"color": "#2563EB", "radius": 8}
        },
        {
            "id": "roads",
            "name": "高速公路",
            "type": "geojson",
            "visible": true,
            "opacity": 0.8,
            "style": {"color": "#F59E0B", "weight": 3}
        }
    ]'::jsonb,
    'default'
);

-- 示例地图配置
INSERT INTO map_documents (id, name, description, map_type, config, user_id) VALUES
(
    'd4e5f6a7-b8c9-0123-defa-234567890123',
    '全国3D地图',
    '使用Cesium展示的全国地级行政区3D视图',
    '3d',
    '{
        "center": [105.0, 35.0],
        "zoom": 5,
        "terrain": true,
        "imagery": "BingMaps",
        "sceneMode": "3D"
    }'::jsonb,
    'default'
);

-- ========== 分析和统计函数 ==========

-- 获取用户会话统计
CREATE OR REPLACE FUNCTION get_user_session_stats(p_user_id VARCHAR)
RETURNS TABLE (
    total_sessions BIGINT,
    active_sessions BIGINT,
    total_messages BIGINT,
    avg_messages_per_session NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*)::BIGINT,
        COUNT(*) FILTER (WHERE status = 'active')::BIGINT,
        COALESCE(SUM(message_count)::BIGINT, 0),
        COALESCE(AVG(message_count)::NUMERIC, 0)
    FROM chat_sessions
    WHERE user_id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- 获取GIS数据统计
CREATE OR REPLACE FUNCTION get_gis_data_stats()
RETURNS TABLE (
    total_count BIGINT,
    by_type JSONB,
    by_format JSONB,
    total_size_bytes BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*)::BIGINT,
        JSONB_OBJECT_AGG(data_type, count) FILTER (WHERE data_type IS NOT NULL),
        JSONB_OBJECT_AGG(format, count) FILTER (WHERE format IS NOT NULL),
        COALESCE(SUM(size_bytes)::BIGINT, 0)
    FROM (
        SELECT data_type, format, COUNT(*) as count, SUM(size_bytes) as size_bytes
        FROM gis_data
        GROUP BY data_type, format
    ) agg;
END;
$$ LANGUAGE plpgsql;

-- 空间查询：查找范围内的数据
CREATE OR REPLACE FUNCTION find_gis_data_in_bounds(
    p_min_lng FLOAT,
    p_min_lat FLOAT,
    p_max_lng FLOAT,
    p_max_lat FLOAT,
    p_user_id VARCHAR DEFAULT 'default'
)
RETURNS TABLE (
    id VARCHAR,
    name VARCHAR,
    bounds JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        g.id,
        g.name,
        JSONB_BUILD_OBJECT(
            'minLng', ST_XMin(g.geojson::geometry::box2d),
            'minLat', ST_YMin(g.geojson::geometry::box2d),
            'maxLng', ST_XMax(g.geojson::geometry::box2d),
            'maxLat', ST_YMax(g.geojson::geometry::box2d)
        ) as bounds
    FROM gis_data g
    WHERE g.user_id = p_user_id
      AND g.geojson::geometry && ST_MakeEnvelope(p_min_lng, p_min_lat, p_max_lng, p_max_lat, 4326);
END;
$$ LANGUAGE plpgsql;

-- ========== 权限和安全 ==========

-- 行级安全策略 (RLS) - PostgreSQL 15+
ALTER TABLE chat_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE gis_data ENABLE ROW LEVEL SECURITY;
ALTER TABLE map_documents ENABLE ROW LEVEL SECURITY;

-- 创建策略：用户只能访问自己的数据
CREATE POLICY session_user_policy ON chat_sessions
    FOR ALL USING (user_id = current_setting('app.current_user', true));

-- 注意: 需要在连接时设置 app.current_user，或者使用以下简化的策略

-- 简化策略：基于user_id字段
CREATE POLICY chat_sessions_user_policy ON chat_sessions
    FOR ALL USING (user_id = 'default');

CREATE POLICY messages_user_policy ON messages
    FOR ALL USING (
        session_id IN (
            SELECT id FROM chat_sessions WHERE user_id = 'default'
        )
    );

CREATE POLICY gis_data_user_policy ON gis_data
    FOR ALL USING (user_id = 'default');

CREATE POLICY map_documents_user_policy ON map_documents
    FOR ALL USING (user_id = 'default');

-- ========== 维护视图 ==========

-- 创建会话详情视图
CREATE OR REPLACE VIEW v_chat_sessions_detail AS
SELECT 
    s.id,
    s.title,
    s.user_id,
    s.status,
    s.message_count,
    s.starred,
    s.archived,
    s.created_at,
    s.updated_at,
    s.last_message_at,
    COALESCE(
        (SELECT content FROM messages WHERE session_id = s.id ORDER BY created_at DESC LIMIT 1),
        ''
    ) as last_message,
    EXTRACT(EPOCH FROM (COALESCE(s.last_message_at, s.created_at) - s.created_at)) / 3600 as hours_active
FROM chat_sessions s;

-- 创建GIS数据详情视图
CREATE OR REPLACE VIEW v_gis_data_detail AS
SELECT 
    g.id,
    g.name,
    g.description,
    g.data_type,
    g.format,
    g.size_bytes,
    g.crs,
    g.tags,
    g.is_public,
    g.created_at,
    g.updated_at,
    CASE 
        WHEN g.geojson IS NOT NULL THEN 
            JSONB_BUILD_OBJECT(
                'type', 'FeatureCollection',
                'featureCount', (g.geojson->'features')::int
            )
        ELSE NULL
    END as geometry_info,
    ST_Extent(g.geojson::geometry) as extent
FROM gis_data g;

-- ========== 完成 ==========
DO $$
BEGIN
    RAISE NOTICE 'GeoAgent V2 migration completed successfully';
    RAISE NOTICE 'Created tables: chat_sessions, messages, gis_data, map_documents, async_tasks';
    RAISE NOTICE 'Created indexes: 10+ indexes for performance';
    RAISE NOTICE 'Created triggers: updated_at, message_count';
    RAISE NOTICE 'Created functions: get_user_session_stats, get_gis_data_stats, find_gis_data_in_bounds';
    RAISE NOTICE 'Created views: v_chat_sessions_detail, v_gis_data_detail';
    RAISE NOTICE 'Seeded sample GIS data: 2 datasets';
    RAISE NOTICE 'Seeded sample maps: 2 configurations';
END $$;

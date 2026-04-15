-- GeoNexus 数据库索引优化脚本
-- 执行方式: psql -d geonexus -f indexes.sql

-- ========== 会话表索引 ==========
CREATE INDEX IF NOT EXISTS idx_chat_session_user_id ON chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_session_created_at ON chat_sessions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_session_updated_at ON chat_sessions(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_session_status ON chat_sessions(status);

-- ========== 消息表索引 ==========
CREATE INDEX IF NOT EXISTS idx_message_session_id ON messages(session_id);
CREATE INDEX IF NOT EXISTS idx_message_session_created ON messages(session_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_message_role ON messages(role);
CREATE INDEX IF NOT EXISTS idx_message_has_attachments ON messages(has_attachments) WHERE has_attachments = true;

-- ========== GIS数据表索引 ==========
CREATE INDEX IF NOT EXISTS idx_gis_data_user_id ON gis_data(user_id);
CREATE INDEX IF NOT EXISTS idx_gis_data_type ON gis_data(data_type);
CREATE INDEX IF NOT EXISTS idx_gis_data_created_at ON gis_data(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_gis_data_name ON gis_data(name varchar_pattern_ops);
CREATE INDEX IF NOT EXISTS idx_gis_data_tags ON gis_data USING GIN(tags);
CREATE INDEX IF NOT EXISTS idx_gis_data_spatial ON gis_data(geojson) 
    WHERE geojson IS NOT NULL;

-- ========== 地图文档表索引 ==========
CREATE INDEX IF NOT EXISTS idx_map_doc_user_id ON map_documents(user_id);
CREATE INDEX IF NOT EXISTS idx_map_doc_type ON map_documents(map_type);
CREATE INDEX IF NOT EXISTS idx_map_doc_created_at ON map_documents(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_map_doc_public ON map_documents(is_public) WHERE is_public = true;

-- ========== 异步任务表索引 ==========
CREATE INDEX IF NOT EXISTS idx_task_user_id ON async_tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_task_status ON async_tasks(status);
CREATE INDEX IF NOT EXISTS idx_task_type ON async_tasks(task_type);
CREATE INDEX IF NOT EXISTS idx_task_created_at ON async_tasks(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_completed_at ON async_tasks(completed_at DESC) 
    WHERE completed_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_task_progress ON async_tasks(progress) 
    WHERE status = 'PROCESSING';

-- ========== PostGIS空间索引 ==========
-- 为包含几何数据的表创建空间索引
-- 注意: 需要先确保PostGIS扩展已启用

-- 在gis_data表上创建空间索引(如果包含几何列)
-- CREATE INDEX IF NOT EXISTS idx_gis_data_geom ON gis_data USING GIST(geom);

-- ========== 复合索引 ==========
CREATE INDEX IF NOT EXISTS idx_chat_session_user_status ON chat_sessions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_message_session_role ON messages(session_id, role);
CREATE INDEX IF NOT EXISTS idx_gis_data_user_type ON gis_data(user_id, data_type);

-- ========== 统计信息更新 ==========
ANALYZE chat_sessions;
ANALYZE messages;
ANALYZE gis_data;
ANALYZE map_documents;
ANALYZE async_tasks;

-- ========== 查看索引大小 ==========
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
    idx_scan AS index_scans
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY pg_relation_size(indexrelid) DESC;

-- V6__add_permissions_audit.sql
-- 细粒度资源权限表 + 审计日志表
--
-- 执行时机：应用升级前手动执行，或由 JPA auto-ddl 自动创建

-- ==================== 资源权限表 ====================
CREATE TABLE IF NOT EXISTS resource_permissions (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    resource_id VARCHAR(50) NOT NULL,
    permission VARCHAR(20) NOT NULL,  -- 'READ', 'WRITE', 'ADMIN', 'DELETE'
    row_filter JSONB,                 -- 行级过滤条件，如 {"district": "朝阳区"}
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_permission CHECK (permission IN ('READ', 'WRITE', 'ADMIN', 'DELETE'))
);

CREATE INDEX IF NOT EXISTS idx_resource_perms_user ON resource_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_resource_perms_resource ON resource_permissions(resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_perms_user_resource ON resource_permissions(user_id, resource_id);

COMMENT ON TABLE resource_permissions IS 'GIS资源细粒度权限控制表';
COMMENT ON COLUMN resource_permissions.row_filter IS '行级数据过滤条件，JSON格式';

-- ==================== 审计日志表 ====================
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50),
    action VARCHAR(50) NOT NULL,      -- CREATE, UPDATE, DELETE, READ, EXPORT, LOGIN, LOGOUT
    resource_type VARCHAR(50),         -- GIS_DATA, MAP_DOCUMENT, USER, SESSION
    resource_id VARCHAR(50),
    details JSONB,                   -- 详细操作信息
    ip_address INET,
    user_agent TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_user_time ON audit_logs(user_id, timestamp DESC);

COMMENT ON TABLE audit_logs IS '系统操作审计日志表';
COMMENT ON COLUMN audit_logs.action IS '操作类型：CREATE/UPDATE/DELETE/READ/EXPORT/LOGIN/LOGOUT';
COMMENT ON COLUMN audit_logs.resource_type IS '资源类型：GIS_DATA/MAP_DOCUMENT/USER/SESSION等';

-- ==================== 统计 ====================
ANALYZE resource_permissions;
ANALYZE audit_logs;

DO $$
BEGIN
    RAISE NOTICE 'V6 permissions and audit tables created';
END $$;

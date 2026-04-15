-- V4__add_audit_fields.sql
-- 为所有表添加审计字段（created_by, updated_by）、乐观锁（version）、
-- 软删除（deleted_at, deleted_by）和多租户（tenant_id）
--
-- 执行时机：应用升级前手动执行
-- 注意：此脚本增加字段，不影响现有数据

-- ==================== chat_sessions ====================
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(50);
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50) NOT NULL DEFAULT 'default';
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NOT NULL DEFAULT 'system';
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NOT NULL DEFAULT 'system';

COMMENT ON COLUMN chat_sessions.version IS '乐观锁版本号';
COMMENT ON COLUMN chat_sessions.deleted_at IS '软删除时间（null=未删除）';
COMMENT ON COLUMN chat_sessions.deleted_by IS '软删除操作人';
COMMENT ON COLUMN chat_sessions.tenant_id IS '租户ID（用于数据隔离）';
COMMENT ON COLUMN chat_sessions.created_by IS '创建人';
COMMENT ON COLUMN chat_sessions.updated_by IS '最后更新人';

-- ==================== messages ====================
ALTER TABLE messages ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(50);
ALTER TABLE messages ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50) NOT NULL DEFAULT 'default';
ALTER TABLE messages ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NOT NULL DEFAULT 'system';
ALTER TABLE messages ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NOT NULL DEFAULT 'system';

COMMENT ON COLUMN messages.version IS '乐观锁版本号';
COMMENT ON COLUMN messages.deleted_at IS '软删除时间（null=未删除）';
COMMENT ON COLUMN messages.deleted_by IS '软删除操作人';
COMMENT ON COLUMN messages.tenant_id IS '租户ID（用于数据隔离）';
COMMENT ON COLUMN messages.created_by IS '创建人';
COMMENT ON COLUMN messages.updated_by IS '最后更新人';

-- ==================== gis_data ====================
ALTER TABLE gis_data ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE gis_data ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE gis_data ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(50);
ALTER TABLE gis_data ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50) NOT NULL DEFAULT 'default';
ALTER TABLE gis_data ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NOT NULL DEFAULT 'system';
ALTER TABLE gis_data ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NOT NULL DEFAULT 'system';

COMMENT ON COLUMN gis_data.version IS '乐观锁版本号';
COMMENT ON COLUMN gis_data.deleted_at IS '软删除时间（null=未删除）';
COMMENT ON COLUMN gis_data.deleted_by IS '软删除操作人';
COMMENT ON COLUMN gis_data.tenant_id IS '租户ID（用于数据隔离）';
COMMENT ON COLUMN gis_data.created_by IS '创建人';
COMMENT ON COLUMN gis_data.updated_by IS '最后更新人';

-- ==================== map_documents ====================
ALTER TABLE map_documents ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE map_documents ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE map_documents ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(50);
ALTER TABLE map_documents ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50) NOT NULL DEFAULT 'default';
ALTER TABLE map_documents ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NOT NULL DEFAULT 'system';
ALTER TABLE map_documents ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NOT NULL DEFAULT 'system';

COMMENT ON COLUMN map_documents.version IS '乐观锁版本号';
COMMENT ON COLUMN map_documents.deleted_at IS '软删除时间（null=未删除）';
COMMENT ON COLUMN map_documents.deleted_by IS '软删除操作人';
COMMENT ON COLUMN map_documents.tenant_id IS '租户ID（用于数据隔离）';
COMMENT ON COLUMN map_documents.created_by IS '创建人';
COMMENT ON COLUMN map_documents.updated_by IS '最后更新人';

-- ==================== async_tasks ====================
ALTER TABLE async_tasks ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE async_tasks ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE async_tasks ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(50);
ALTER TABLE async_tasks ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50) NOT NULL DEFAULT 'default';
ALTER TABLE async_tasks ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NOT NULL DEFAULT 'system';
ALTER TABLE async_tasks ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50) NOT NULL DEFAULT 'system';

COMMENT ON COLUMN async_tasks.version IS '乐观锁版本号';
COMMENT ON COLUMN async_tasks.deleted_at IS '软删除时间（null=未删除）';
COMMENT ON COLUMN async_tasks.deleted_by IS '软删除操作人';
COMMENT ON COLUMN async_tasks.tenant_id IS '租户ID（用于数据隔离）';
COMMENT ON COLUMN async_tasks.created_by IS '创建人';
COMMENT ON COLUMN async_tasks.updated_by IS '最后更新人';

-- ==================== 索引 ====================
CREATE INDEX IF NOT EXISTS idx_deleted_at ON chat_sessions(deleted_at);
CREATE INDEX IF NOT EXISTS idx_deleted_at ON messages(deleted_at);
CREATE INDEX IF NOT EXISTS idx_deleted_at ON gis_data(deleted_at);
CREATE INDEX IF NOT EXISTS idx_deleted_at ON map_documents(deleted_at);
CREATE INDEX IF NOT EXISTS idx_deleted_at ON async_tasks(deleted_at);

CREATE INDEX IF NOT EXISTS idx_tenant_id ON chat_sessions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_id ON messages(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_id ON gis_data(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_id ON map_documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_id ON async_tasks(tenant_id);

CREATE INDEX IF NOT EXISTS idx_created_by ON chat_sessions(created_by);
CREATE INDEX IF NOT EXISTS idx_created_by ON messages(created_by);
CREATE INDEX IF NOT EXISTS idx_created_by ON gis_data(created_by);
CREATE INDEX IF NOT EXISTS idx_created_by ON map_documents(created_by);
CREATE INDEX IF NOT EXISTS idx_created_by ON async_tasks(created_by);

-- ==================== 统计 ====================
ANALYZE chat_sessions;
ANALYZE messages;
ANALYZE gis_data;
ANALYZE map_documents;
ANALYZE async_tasks;

DO $$
BEGIN
    RAISE NOTICE 'V4 audit fields migration completed';
    RAISE NOTICE 'Added to all tables: version, deleted_at, deleted_by, tenant_id, created_by, updated_by';
    RAISE NOTICE 'Created indexes on deleted_at, tenant_id, created_by';
END $$;

-- V3__rename_to_geonexus.sql
-- 迁移：将数据库/用户名从 geoagent 改为 geonexus
-- 执行时机：应用启动前手动执行，或蓝绿部署时执行
-- 注意：此脚本为不可逆变更，执行前请备份数据

-- 1. 确保新数据库不存在（如果是从 geoagent 重命名过来的）
-- CREATE DATABASE geonexus;

-- 2. 重命名用户（如果使用的是 OS 级别的用户）
-- ALTER USER geoagent RENAME TO geonexus;

-- 3. 更新 application 表或配置表中的系统级配置（如果配置里硬编码了 geoagent）
-- UPDATE system_config SET value = 'geonexus' WHERE key = 'app.name';

-- 4. 重建序列（如果使用了 SEQUENCE）
-- ALTER SEQUENCE IF EXISTS hibernate_sequence RENAME TO geonexus_hibernate_sequence;

-- 5. 备注：Redis key 迁移（如有需要）
-- 如果 Redis 中有 geoagent: 前缀的 key，需要执行：
-- KEYS geoagent:* → 遍历并 RENAME 为 geonexus:

-- 6. 验证
-- SELECT current_database();
-- SELECT current_user;

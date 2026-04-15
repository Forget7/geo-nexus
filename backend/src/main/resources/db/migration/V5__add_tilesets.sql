-- V5: 添加 3D Tiles 数据集表
CREATE TABLE tilesets (
    id              VARCHAR(36) PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMP,
    version         BIGINT DEFAULT 0,
    tenant_id       VARCHAR(36),

    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    url             VARCHAR(1024) NOT NULL,
    type            VARCHAR(20) NOT NULL,  -- CESIUM_ION or URL
    ion_asset_id    INTEGER,
    position_lon    DOUBLE PRECISION,
    position_lat    DOUBLE PRECISION,
    position_height DOUBLE PRECISION DEFAULT 0,
    max_screen_space_error INTEGER DEFAULT 16,
    visible         BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_tilesets_name ON tilesets(name);
CREATE INDEX idx_tilesets_type ON tilesets(type);

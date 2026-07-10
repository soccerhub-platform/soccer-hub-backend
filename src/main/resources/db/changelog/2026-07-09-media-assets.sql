-- ============================================
-- MEDIA ASSET METADATA
-- ============================================

CREATE TABLE IF NOT EXISTS media_asset
(
    id                  UUID PRIMARY KEY,
    owner_type          VARCHAR(30)   NOT NULL,
    owner_id            UUID          NOT NULL,
    kind                VARCHAR(30)   NOT NULL,
    file_name           VARCHAR(255)  NOT NULL,
    mime_type           VARCHAR(255)  NOT NULL,
    size_bytes          BIGINT        NOT NULL,
    width               INTEGER,
    height              INTEGER,
    original_storage_key VARCHAR(1024) NOT NULL,
    thumb_storage_key   VARCHAR(1024),
    medium_storage_key  VARCHAR(1024),
    deleted_by          UUID,
    deleted_at          TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by          VARCHAR,
    modified_by         VARCHAR,

    CONSTRAINT chk_media_asset_file_name_not_blank
        CHECK (TRIM(file_name) <> ''),
    CONSTRAINT chk_media_asset_mime_type_not_blank
        CHECK (TRIM(mime_type) <> ''),
    CONSTRAINT chk_media_asset_size_bytes_non_negative
        CHECK (size_bytes >= 0),
    CONSTRAINT chk_media_asset_width_positive
        CHECK (width IS NULL OR width > 0),
    CONSTRAINT chk_media_asset_height_positive
        CHECK (height IS NULL OR height > 0),
    CONSTRAINT chk_media_asset_original_storage_key_not_blank
        CHECK (TRIM(original_storage_key) <> ''),
    CONSTRAINT uq_media_asset_original_storage_key
        UNIQUE (original_storage_key),
    CONSTRAINT uq_media_asset_thumb_storage_key
        UNIQUE (thumb_storage_key),
    CONSTRAINT uq_media_asset_medium_storage_key
        UNIQUE (medium_storage_key)
);

CREATE INDEX IF NOT EXISTS idx_media_asset_owner
    ON media_asset (owner_type, owner_id, kind, deleted_at);

CREATE UNIQUE INDEX IF NOT EXISTS uq_media_asset_active_avatar
    ON media_asset (owner_type, owner_id, kind)
    WHERE deleted_at IS NULL
      AND kind = 'AVATAR';

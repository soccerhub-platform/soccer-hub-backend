-- ============================================
-- COACH PROFILE EXTENSIONS
-- ============================================

ALTER TABLE coach_profiles
    ADD COLUMN IF NOT EXISTS specialization VARCHAR(255),
    ADD COLUMN IF NOT EXISTS bio VARCHAR(1000);

-- ============================================
-- COACH AVAILABILITY
-- ============================================

CREATE TABLE IF NOT EXISTS coach_availability
(
    coach_id     UUID PRIMARY KEY,
    days         VARCHAR(64) NOT NULL,
    time_from    TIME NOT NULL,
    time_to      TIME NOT NULL,
    timezone     VARCHAR(64) NOT NULL DEFAULT 'Asia/Almaty',
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW(),
    created_by   VARCHAR,
    modified_by  VARCHAR,

    CONSTRAINT fk_coach_availability_coach FOREIGN KEY (coach_id)
        REFERENCES coach_profiles(id) ON DELETE CASCADE
);

-- ============================================
-- COACH NOTIFICATION SETTINGS
-- ============================================

CREATE TABLE IF NOT EXISTS coach_notification_settings
(
    coach_id          UUID PRIMARY KEY,
    today_sessions    BOOLEAN NOT NULL DEFAULT TRUE,
    overdue_reports   BOOLEAN NOT NULL DEFAULT TRUE,
    schedule_changes  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW(),
    created_by        VARCHAR,
    modified_by       VARCHAR,

    CONSTRAINT fk_coach_notification_settings_coach FOREIGN KEY (coach_id)
        REFERENCES coach_profiles(id) ON DELETE CASCADE
);

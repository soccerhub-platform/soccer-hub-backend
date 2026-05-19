-- ============================================
-- COACH TRAINING SESSIONS
-- ============================================

CREATE TABLE IF NOT EXISTS training_sessions
(
    id                 UUID PRIMARY KEY,
    group_id           UUID         NOT NULL,
    coach_id           UUID         NOT NULL,
    schedule_id        UUID,
    location_id        UUID,
    session_date       DATE         NOT NULL,
    scheduled_start_at TIMESTAMP    NOT NULL,
    scheduled_end_at   TIMESTAMP    NOT NULL,
    actual_start_at    TIMESTAMP,
    actual_end_at      TIMESTAMP,
    status             VARCHAR(32)  NOT NULL,
    cancel_reason      TEXT,
    topic              VARCHAR(255),
    coach_comment      TEXT,
    incidents          TEXT,
    homework           TEXT,
    report_done        BOOLEAN      NOT NULL DEFAULT FALSE,
    started_by         UUID,
    completed_by       UUID,
    cancelled_by       UUID,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         VARCHAR,
    modified_by        VARCHAR,
    version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_training_sessions_group FOREIGN KEY (group_id)
        REFERENCES groups (id) ON DELETE CASCADE,

    CONSTRAINT fk_training_sessions_coach FOREIGN KEY (coach_id)
        REFERENCES coach_profiles (id) ON DELETE CASCADE,

    CONSTRAINT fk_training_sessions_schedule FOREIGN KEY (schedule_id)
        REFERENCES group_schedules (id) ON DELETE SET NULL,

    CONSTRAINT fk_training_sessions_location FOREIGN KEY (location_id)
        REFERENCES location (id),

    CONSTRAINT fk_training_sessions_started_by FOREIGN KEY (started_by)
        REFERENCES app_user (id),

    CONSTRAINT fk_training_sessions_completed_by FOREIGN KEY (completed_by)
        REFERENCES app_user (id),

    CONSTRAINT fk_training_sessions_cancelled_by FOREIGN KEY (cancelled_by)
        REFERENCES app_user (id),

    CONSTRAINT uq_training_sessions_schedule_date UNIQUE (schedule_id, session_date)
);

CREATE INDEX IF NOT EXISTS idx_training_sessions_coach_date ON training_sessions (coach_id, session_date);
CREATE INDEX IF NOT EXISTS idx_training_sessions_group_date ON training_sessions (group_id, session_date);
CREATE INDEX IF NOT EXISTS idx_training_sessions_status ON training_sessions (status);
CREATE INDEX IF NOT EXISTS idx_training_sessions_schedule ON training_sessions (schedule_id);

-- ============================================
-- TRAINING SESSION ATTENDANCE
-- ============================================

CREATE TABLE IF NOT EXISTS training_session_attendance
(
    id          UUID PRIMARY KEY,
    session_id  UUID         NOT NULL,
    player_id   UUID         NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    comment     TEXT,
    marked_by   UUID,
    marked_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_training_session_attendance_session FOREIGN KEY (session_id)
        REFERENCES training_sessions (id) ON DELETE CASCADE,

    CONSTRAINT fk_training_session_attendance_player FOREIGN KEY (player_id)
        REFERENCES players (id) ON DELETE CASCADE,

    CONSTRAINT fk_training_session_attendance_marked_by FOREIGN KEY (marked_by)
        REFERENCES app_user (id),

    CONSTRAINT uq_training_session_attendance_session_player UNIQUE (session_id, player_id)
);

CREATE INDEX IF NOT EXISTS idx_training_session_attendance_session ON training_session_attendance (session_id);
CREATE INDEX IF NOT EXISTS idx_training_session_attendance_player ON training_session_attendance (player_id);
CREATE INDEX IF NOT EXISTS idx_training_session_attendance_status ON training_session_attendance (status);

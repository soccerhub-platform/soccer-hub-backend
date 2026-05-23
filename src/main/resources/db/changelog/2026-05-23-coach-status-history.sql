CREATE TABLE IF NOT EXISTS coach_status_history
(
    id          UUID PRIMARY KEY,
    coach_id    UUID NOT NULL,
    status      VARCHAR(50) NOT NULL,
    changed_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    changed_by  UUID,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    created_by  VARCHAR,
    modified_by VARCHAR,

    CONSTRAINT fk_coach_status_history_coach FOREIGN KEY (coach_id)
        REFERENCES coach_profiles(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_coach_status_history_coach_changed_at
    ON coach_status_history(coach_id, changed_at DESC);

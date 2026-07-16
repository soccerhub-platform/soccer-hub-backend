--liquibase formatted sql

--changeset codex:2026-07-16-group-coach-history
ALTER TABLE group_coaches
    ADD COLUMN IF NOT EXISTS removal_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS replacement_coach_id UUID;

CREATE INDEX IF NOT EXISTS idx_group_coaches_group_assigned_from
    ON group_coaches(group_id, assigned_from DESC);

--rollback DROP INDEX IF EXISTS idx_group_coaches_group_assigned_from;
--rollback ALTER TABLE group_coaches DROP COLUMN IF EXISTS replacement_coach_id;
--rollback ALTER TABLE group_coaches DROP COLUMN IF EXISTS removal_reason;

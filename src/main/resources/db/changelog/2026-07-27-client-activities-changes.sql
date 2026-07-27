-- liquibase formatted sql

--changeset codex:2026-07-27-client-activity-source
ALTER TABLE client_activities
    ADD COLUMN source_type VARCHAR(50),
    ADD COLUMN source_id UUID;

CREATE UNIQUE INDEX ux_client_activity_source
    ON client_activities(client_id, activity_type, source_type, source_id)
    WHERE source_id IS NOT NULL;
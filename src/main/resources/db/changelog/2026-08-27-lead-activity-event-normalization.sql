--liquibase formatted sql

-- changeset codex:2026-08-27-lead-activity-event-normalization
-- Normalize legacy UI action values accidentally persisted as lead events.
UPDATE lead_activities SET event = 'START_CONTRACT' WHERE event = 'CREATE_CONTRACT';
UPDATE lead_activities SET event = 'REJECT' WHERE event = 'LOST';

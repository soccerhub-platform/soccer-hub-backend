--liquibase formatted sql

--changeset codex:2026-08-17-trial-coach-recommendation
ALTER TABLE trial_bookings
    ADD COLUMN coach_recommendation VARCHAR(40),
    ADD COLUMN coach_recommended_group_id UUID,
    ADD COLUMN coach_recommendation_comment TEXT,
    ADD COLUMN coach_recommendation_at TIMESTAMP,
    ADD COLUMN coach_recommendation_by UUID;
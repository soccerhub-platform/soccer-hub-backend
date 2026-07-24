--liquibase formatted sql

-- changeset codex:2026-07-22-lead-lifecycle-v2
-- Normalize historical sales statuses into the Lead v2 funnel.
UPDATE leads SET status = 'IN_PROGRESS' WHERE status IN ('CONTACTED', 'QUALIFIED');
UPDATE leads SET status = 'DECISION_PENDING' WHERE status IN ('TRIAL_DONE', 'WAITING_PAYMENT');
UPDATE leads SET status = 'CONVERTED' WHERE status = 'WON';

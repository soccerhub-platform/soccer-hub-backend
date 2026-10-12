--liquibase formatted sql
--changeset codex:2026-10-09-branch-history
CREATE TABLE branch_history (
 id UUID PRIMARY KEY,branch_id UUID NOT NULL,event VARCHAR(32) NOT NULL,
 changed_by VARCHAR(100) NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 before_snapshot JSONB,after_snapshot JSONB NOT NULL
);
CREATE INDEX idx_branch_history_branch ON branch_history(branch_id,created_at DESC);

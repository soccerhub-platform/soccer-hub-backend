--liquibase formatted sql
--changeset codex:2026-10-09-employee-permissions
CREATE TABLE employee_access_policy (
 user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
 changed_by UUID NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE employee_permission (
 user_id UUID NOT NULL REFERENCES employee_access_policy(user_id) ON DELETE CASCADE,
 permission VARCHAR(64) NOT NULL,
 PRIMARY KEY(user_id,permission)
);
CREATE TABLE employee_access_history (
 id UUID PRIMARY KEY, user_id UUID NOT NULL, changed_by UUID NOT NULL,
 permissions TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_employee_access_history_user ON employee_access_history(user_id,created_at DESC);

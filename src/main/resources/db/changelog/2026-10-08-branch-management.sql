--liquibase formatted sql

--changeset codex:2026-10-08-branch-management
ALTER TABLE branches ADD COLUMN phone VARCHAR(20);
ALTER TABLE branches ADD COLUMN email VARCHAR(254);
ALTER TABLE branches ADD COLUMN manager_admin_id UUID;
ALTER TABLE branches ADD COLUMN working_hours JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE branches ADD COLUMN archived_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE branches ADD CONSTRAINT fk_branch_manager FOREIGN KEY (manager_admin_id)
    REFERENCES admin_profiles(id) ON DELETE SET NULL;
ALTER TABLE branches ADD CONSTRAINT ck_branch_working_hours_array
    CHECK (jsonb_typeof(working_hours) = 'array' AND jsonb_array_length(working_hours) IN (0, 7));
UPDATE branches SET archived_at = NOW() WHERE active = FALSE;
CREATE INDEX idx_branches_club_active ON branches(club_id, active);
CREATE INDEX idx_branches_manager ON branches(manager_admin_id) WHERE manager_admin_id IS NOT NULL;

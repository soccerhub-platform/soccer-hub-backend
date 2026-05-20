-- ============================================
-- REMOVE SECTION DEPENDENCY (MVP)
-- ============================================

-- TRIALS -> GROUP
ALTER TABLE trials
    ADD COLUMN IF NOT EXISTS group_id UUID;

UPDATE trials t
SET group_id = s.group_id
FROM sections s
WHERE t.group_id IS NULL
  AND t.section_id = s.id;

DO $$
DECLARE
    unresolved_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO unresolved_count
    FROM trials
    WHERE group_id IS NULL;

    IF unresolved_count > 0 THEN
        RAISE EXCEPTION 'Cannot migrate trials to group_id. Unresolved rows: %', unresolved_count;
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_trials_group'
    ) THEN
        ALTER TABLE trials
            ADD CONSTRAINT fk_trials_group
                FOREIGN KEY (group_id)
                    REFERENCES groups (id) ON DELETE CASCADE;
    END IF;
END$$;

ALTER TABLE trials
    ALTER COLUMN group_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_trials_group ON trials (group_id);

ALTER TABLE trials DROP CONSTRAINT IF EXISTS fk_trials_section;
DROP INDEX IF EXISTS idx_trials_section;
ALTER TABLE trials DROP COLUMN IF EXISTS section_id;

-- ATTENDANCE -> GROUP
ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS group_id UUID;

UPDATE attendance a
SET group_id = s.group_id
FROM sections s
WHERE a.group_id IS NULL
  AND a.section_id = s.id;

DO $$
DECLARE
    unresolved_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO unresolved_count
    FROM attendance
    WHERE group_id IS NULL;

    IF unresolved_count > 0 THEN
        RAISE EXCEPTION 'Cannot migrate attendance to group_id. Unresolved rows: %', unresolved_count;
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_attendance_group'
    ) THEN
        ALTER TABLE attendance
            ADD CONSTRAINT fk_attendance_group
                FOREIGN KEY (group_id)
                    REFERENCES groups (id);
    END IF;
END$$;

ALTER TABLE attendance
    ALTER COLUMN group_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_attendance_group ON attendance (group_id);

ALTER TABLE attendance DROP CONSTRAINT IF EXISTS fk_attendance_section;
DROP INDEX IF EXISTS idx_attendance_section;
ALTER TABLE attendance DROP COLUMN IF EXISTS section_id;

-- CONTRACTS cleanup: section_id no longer used
ALTER TABLE contracts DROP CONSTRAINT IF EXISTS fk_contracts_section;
DROP INDEX IF EXISTS idx_contracts_section;
ALTER TABLE contracts DROP COLUMN IF EXISTS section_id;

-- DROP SECTION TABLE
DROP INDEX IF EXISTS idx_sections_group;
DROP TABLE IF EXISTS sections;

-- ============================================
-- CONTRACTS: MOVE MVP ENROLLMENT LINK TO GROUP
-- ============================================

ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS group_id UUID;

ALTER TABLE contracts
    ALTER COLUMN section_id DROP NOT NULL;

UPDATE contracts c
SET group_id = s.group_id
FROM sections s
WHERE c.group_id IS NULL
  AND c.section_id = s.id;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_contracts_group'
    ) THEN
        ALTER TABLE contracts
            ADD CONSTRAINT fk_contracts_group
                FOREIGN KEY (group_id)
                    REFERENCES groups (id);
    END IF;
END$$;

DO $$
DECLARE
    unresolved_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO unresolved_count
    FROM contracts
    WHERE group_id IS NULL;

    IF unresolved_count > 0 THEN
        RAISE EXCEPTION 'Cannot set contracts.group_id NOT NULL. Unresolved rows: %', unresolved_count;
    END IF;
END$$;

ALTER TABLE contracts
    ALTER COLUMN group_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_contracts_group ON contracts (group_id);
CREATE INDEX IF NOT EXISTS idx_contracts_group_dates ON contracts (group_id, start_date, end_date);

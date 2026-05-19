-- ============================================
-- LEAD ACTIVITIES: ACTOR ADMIN
-- ============================================

ALTER TABLE lead_activities
    ADD COLUMN IF NOT EXISTS actor_admin_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_lead_activities_actor_admin'
    ) THEN
        ALTER TABLE lead_activities
            ADD CONSTRAINT fk_lead_activities_actor_admin
                FOREIGN KEY (actor_admin_id)
                    REFERENCES admin_profiles (id);
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_lead_activities_actor_admin ON lead_activities(actor_admin_id);

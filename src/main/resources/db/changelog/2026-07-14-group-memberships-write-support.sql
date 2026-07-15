-- ============================================
-- GROUP MEMBERSHIPS: WRITE SUPPORT
-- ============================================

ALTER TABLE public.group_memberships
    ADD COLUMN IF NOT EXISTS join_reason varchar(64);

ALTER TABLE public.group_memberships
    ADD COLUMN IF NOT EXISTS leave_reason varchar(64);

ALTER TABLE public.group_memberships
    ADD COLUMN IF NOT EXISTS comment text;

CREATE UNIQUE INDEX IF NOT EXISTS uq_group_memberships_active_group_player
    ON public.group_memberships (group_id, player_id)
    WHERE status = 'ACTIVE';

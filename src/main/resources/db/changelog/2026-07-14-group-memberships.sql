-- ============================================
-- GROUP MEMBERSHIPS: SEPARATE GROUP PARTICIPATION
-- ============================================

CREATE TABLE IF NOT EXISTS public.group_memberships
(
    id                 uuid                    NOT NULL
        PRIMARY KEY,
    group_id           uuid                    NOT NULL
        CONSTRAINT fk_group_memberships_group
            REFERENCES public.groups
            ON DELETE CASCADE,
    player_id          uuid                    NOT NULL
        CONSTRAINT fk_group_memberships_player
            REFERENCES public.players
            ON DELETE CASCADE,
    status             varchar(32)             NOT NULL,
    joined_at          date                    NOT NULL,
    left_at            date,
    source_contract_id uuid
        CONSTRAINT fk_group_memberships_contract
            REFERENCES public.contracts,
    created_at         timestamp DEFAULT now() NOT NULL,
    updated_at         timestamp DEFAULT now() NOT NULL,
    created_by         varchar,
    modified_by        varchar,
    version            bigint    DEFAULT 0     NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_group_memberships_source_contract
    ON public.group_memberships (source_contract_id)
    WHERE source_contract_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_group_memberships_group
    ON public.group_memberships (group_id);

CREATE INDEX IF NOT EXISTS idx_group_memberships_player
    ON public.group_memberships (player_id);

CREATE INDEX IF NOT EXISTS idx_group_memberships_group_dates
    ON public.group_memberships (group_id, joined_at, left_at);

INSERT INTO public.group_memberships (
    id,
    group_id,
    player_id,
    status,
    joined_at,
    left_at,
    source_contract_id,
    created_at,
    updated_at,
    created_by,
    modified_by,
    version
)
SELECT
    gen_random_uuid(),
    c.group_id,
    c.player_id,
    CASE
        WHEN c.status = 'CANCELLED' THEN 'REMOVED'
        WHEN c.start_date > current_date THEN 'UPCOMING'
        WHEN c.end_date IS NOT NULL AND c.end_date < current_date THEN 'COMPLETED'
        ELSE 'ACTIVE'
    END,
    c.start_date,
    CASE
        WHEN c.status = 'CANCELLED' THEN
            CASE
                WHEN c.end_date IS NOT NULL AND c.end_date < current_date THEN c.end_date
                WHEN c.start_date > current_date THEN c.start_date
                ELSE current_date
            END
        ELSE c.end_date
    END,
    c.id,
    COALESCE(c.created_at, now()),
    COALESCE(c.updated_at, now()),
    c.created_by,
    c.modified_by,
    0
FROM public.contracts c
WHERE NOT EXISTS (
    SELECT 1
    FROM public.group_memberships gm
    WHERE gm.source_contract_id = c.id
);

-- liquibase formatted sql

--changeset codex:2026-07-21-client-student-relations
CREATE TABLE IF NOT EXISTS public.client_student_relations
(
    id                      uuid                    NOT NULL PRIMARY KEY,
    client_id               uuid                    NOT NULL
        CONSTRAINT fk_client_student_relations_client
            REFERENCES public.client_profiles,
    player_id               uuid                    NOT NULL
        CONSTRAINT fk_client_student_relations_player
            REFERENCES public.players,
    relationship_type       varchar(32)             NOT NULL,
    is_primary_contact      boolean   DEFAULT false NOT NULL,
    is_primary_payer        boolean   DEFAULT false NOT NULL,
    is_legal_representative boolean   DEFAULT false NOT NULL,
    receives_notifications  boolean   DEFAULT true  NOT NULL,
    started_at              date                    NOT NULL,
    ended_at                date,
    created_at              timestamp DEFAULT now() NOT NULL,
    updated_at              timestamp DEFAULT now() NOT NULL,
    created_by              varchar,
    modified_by             varchar,
    version                 bigint    DEFAULT 0     NOT NULL,
    CONSTRAINT chk_client_student_relation_dates
        CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_client_student_relations_active_pair
    ON public.client_student_relations (client_id, player_id)
    WHERE ended_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_client_student_relations_primary_contact
    ON public.client_student_relations (player_id)
    WHERE ended_at IS NULL AND is_primary_contact;

CREATE UNIQUE INDEX IF NOT EXISTS uq_client_student_relations_primary_payer
    ON public.client_student_relations (player_id)
    WHERE ended_at IS NULL AND is_primary_payer;

CREATE UNIQUE INDEX IF NOT EXISTS uq_client_student_relations_self
    ON public.client_student_relations (player_id)
    WHERE ended_at IS NULL AND relationship_type = 'SELF';

CREATE INDEX IF NOT EXISTS idx_client_student_relations_client
    ON public.client_student_relations (client_id, started_at, ended_at);

CREATE INDEX IF NOT EXISTS idx_client_student_relations_player
    ON public.client_student_relations (player_id, started_at, ended_at);

INSERT INTO public.client_student_relations (
    id,
    client_id,
    player_id,
    relationship_type,
    is_primary_contact,
    is_primary_payer,
    is_legal_representative,
    receives_notifications,
    started_at,
    ended_at,
    created_at,
    updated_at,
    created_by,
    modified_by,
    version
)
SELECT
    gen_random_uuid(),
    p.parent_id,
    p.id,
    'LEGACY_PARENT',
    true,
    true,
    true,
    true,
    COALESCE(p.created_at::date, current_date),
    NULL,
    COALESCE(p.created_at, now()),
    COALESCE(p.updated_at, now()),
    p.created_by,
    p.modified_by,
    0
FROM public.players p
WHERE p.parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM public.client_student_relations relation
    WHERE relation.client_id = p.parent_id
      AND relation.player_id = p.id
      AND relation.ended_at IS NULL
);

ALTER TABLE public.contracts
    ADD COLUMN IF NOT EXISTS client_id uuid;

UPDATE public.contracts contract
SET client_id = player.parent_id
FROM public.players player
WHERE contract.player_id = player.id
  AND contract.client_id IS NULL;

ALTER TABLE public.contracts
    ADD CONSTRAINT fk_contracts_client
        FOREIGN KEY (client_id) REFERENCES public.client_profiles;

CREATE INDEX IF NOT EXISTS idx_contracts_client
    ON public.contracts (client_id);

-- liquibase formatted sql

-- changeset codex:2026-07-24-contract-workspace-v2
-- validCheckSum: 9:ea01a4f8201f004dbeda7df7ea827fc9
-- preconditions onFail:HALT onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM public.contracts WHERE client_id IS NULL
ALTER TABLE public.contracts
    ALTER COLUMN group_id DROP NOT NULL;

ALTER TABLE public.contracts
    ALTER COLUMN client_id SET NOT NULL;

COMMENT ON COLUMN public.contracts.group_id IS
    'Deprecated compatibility field. Group participation is owned by group_memberships.';

COMMENT ON COLUMN public.contracts.coach_id IS
    'Deprecated compatibility field. Coach assignment is owned by group/session assignments.';

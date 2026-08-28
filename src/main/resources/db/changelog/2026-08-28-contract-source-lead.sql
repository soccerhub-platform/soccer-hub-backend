--liquibase formatted sql

-- changeset codex:2026-08-28-contract-source-lead
ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS source_lead_id UUID;

CREATE INDEX IF NOT EXISTS idx_contracts_source_lead
    ON contracts (source_lead_id)
    WHERE source_lead_id IS NOT NULL;

WITH lead_contract_match AS (
    SELECT DISTINCT ON (contract.id)
           contract.id AS contract_id,
           lead.id AS lead_id
    FROM contracts contract
    JOIN leads lead
      ON lead.client_id = contract.client_id
    JOIN lead_participants participant
      ON participant.lead_id = lead.id
     AND participant.player_id = contract.player_id
    WHERE contract.source_lead_id IS NULL
    ORDER BY contract.id, lead.updated_at DESC NULLS LAST, lead.created_at DESC NULLS LAST
)
UPDATE contracts contract
SET source_lead_id = lead_contract_match.lead_id
FROM lead_contract_match
WHERE contract.id = lead_contract_match.contract_id;

UPDATE leads lead
SET status = 'PAYMENT_PENDING',
    updated_at = NOW()
FROM contracts contract
WHERE contract.source_lead_id = lead.id
  AND lead.status = 'CONTRACT_PENDING'
  AND contract.status IN ('ACTIVE', 'UPCOMING');

UPDATE leads lead
SET status = 'CONVERTED',
    updated_at = NOW()
FROM contracts contract
WHERE contract.source_lead_id = lead.id
  AND lead.status IN ('CONTRACT_PENDING', 'PAYMENT_PENDING')
  AND EXISTS (
      SELECT 1
      FROM payments payment
      WHERE payment.contract_id = contract.id
        AND payment.status = 'PAID'
  );

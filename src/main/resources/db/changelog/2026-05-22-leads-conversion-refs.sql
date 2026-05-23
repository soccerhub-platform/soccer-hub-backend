alter table if exists leads
    add column if not exists player_id uuid,
    add column if not exists contract_id uuid;

with latest_conversion as (
    select distinct on (la.lead_id)
           la.lead_id,
           la.details
    from lead_activities la
    where la.activity_type = 'LEAD_CONVERTED'
      and la.details is not null
      and la.details like '{%'
    order by la.lead_id, la.created_at desc
)
update leads l
set player_id = coalesce(l.player_id, nullif((lc.details::jsonb ->> 'playerId'), '')::uuid),
    contract_id = coalesce(l.contract_id, nullif((lc.details::jsonb ->> 'contractId'), '')::uuid)
from latest_conversion lc
where l.id = lc.lead_id;

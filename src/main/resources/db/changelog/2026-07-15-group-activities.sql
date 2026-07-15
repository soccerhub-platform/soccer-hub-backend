create table if not exists public.group_activities
(
    id             uuid                    not null
        primary key,
    group_id       uuid                    not null
        constraint fk_group_activities_group
            references public.groups
            on delete cascade,
    activity_type  varchar(64)             not null,
    actor_user_id  uuid
        constraint fk_group_activities_actor_user
            references public.app_user,
    payload        jsonb     default '{}'::jsonb not null,
    correlation_id uuid,
    occurred_at    timestamp default now() not null,
    created_at     timestamp default now() not null
);

create index if not exists idx_group_activities_group_occurred
    on public.group_activities (group_id, occurred_at desc);

create index if not exists idx_group_activities_type
    on public.group_activities (activity_type);

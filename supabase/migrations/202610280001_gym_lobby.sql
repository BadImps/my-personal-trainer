-- Social fields are intentionally limited to the public Gym Lobby profile.
-- Email, photos, body data, workouts, nutrition, routes, and schedules stay on-device.
create table if not exists public.gym_members (
    user_id uuid primary key references auth.users(id) on delete cascade,
    display_name text not null check (char_length(display_name) between 3 and 40),
    level integer not null default 1 check (level > 0),
    momentum_points integer not null default 0 check (momentum_points >= 0),
    apex_tier text not null default 'Foundation',
    achievements text[] not null default '{}',
    updated_at timestamptz not null default now()
);

create table if not exists public.gym_private_lobbies (
    owner_id uuid primary key references auth.users(id) on delete cascade,
    created_at timestamptz not null default now()
);

create table if not exists public.gym_private_lobby_members (
    owner_id uuid not null references public.gym_private_lobbies(owner_id) on delete cascade,
    member_id uuid not null references auth.users(id) on delete cascade,
    joined_at timestamptz not null default now(),
    primary key (owner_id, member_id)
);

create table if not exists public.gym_requests (
    id uuid primary key default gen_random_uuid(),
    kind text not null check (kind in ('buddy', 'invite', 'join')),
    sender_id uuid not null references auth.users(id) on delete cascade,
    recipient_id uuid not null references auth.users(id) on delete cascade,
    lobby_owner_id uuid references auth.users(id) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'accepted', 'declined')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (sender_id <> recipient_id)
);

create unique index if not exists gym_requests_unique_action
    on public.gym_requests (sender_id, recipient_id, kind, coalesce(lobby_owner_id, '00000000-0000-0000-0000-000000000000'::uuid));

alter table public.gym_members enable row level security;
alter table public.gym_private_lobbies enable row level security;
alter table public.gym_private_lobby_members enable row level security;
alter table public.gym_requests enable row level security;

revoke all on public.gym_members, public.gym_private_lobbies,
    public.gym_private_lobby_members, public.gym_requests from anon, authenticated;

create or replace function public.gym_lobby_publish(
    member_name text,
    member_level integer,
    member_points integer,
    member_apex text,
    member_achievements text[]
) returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    member_id uuid := auth.uid();
begin
    if member_id is null then raise exception 'Authentication required'; end if;
    insert into public.gym_members(user_id, display_name, level, momentum_points, apex_tier, achievements, updated_at)
    values (member_id,
            case when char_length(trim(coalesce(member_name, ''))) >= 3
                then left(trim(member_name), 40) else 'Gym Member' end,
            greatest(1, coalesce(member_level, 1)), greatest(0, coalesce(member_points, 0)),
            left(coalesce(nullif(trim(member_apex), ''), 'Foundation'), 30),
            array(
                select left(trim(achievement), 60)
                from unnest(coalesce(member_achievements, '{}')) achievement
                where trim(achievement) <> ''
                limit 50
            ), now())
    on conflict (user_id) do update set
        display_name = excluded.display_name,
        level = excluded.level,
        momentum_points = excluded.momentum_points,
        apex_tier = excluded.apex_tier,
        achievements = excluded.achievements,
        updated_at = now();
    insert into public.gym_private_lobbies(owner_id) values (member_id) on conflict do nothing;
    insert into public.gym_private_lobby_members(owner_id, member_id) values (member_id, member_id) on conflict do nothing;
end;
$$;

create or replace function public.gym_lobby_snapshot() returns jsonb
language sql
security definer
set search_path = public, pg_temp
stable
as $$
select case when auth.uid() is null then jsonb_build_object('error', 'Authentication required') else jsonb_build_object(
    'members', coalesce((
        select jsonb_agg(jsonb_build_object(
            'id', m.user_id,
            'name', m.display_name,
            'level', m.level,
            'points', m.momentum_points,
            'apex', m.apex_tier,
            'achievements', to_jsonb(m.achievements),
            'buddy', exists(
                select 1 from public.gym_requests r
                where r.kind = 'buddy' and r.status = 'accepted'
                  and ((r.sender_id = auth.uid() and r.recipient_id = m.user_id)
                    or (r.recipient_id = auth.uid() and r.sender_id = m.user_id))
            )
        ) order by lower(m.display_name))
        from public.gym_members m where m.user_id <> auth.uid()
    ), '[]'::jsonb),
    'requests', coalesce((
        select jsonb_agg(jsonb_build_object(
            'id', r.id,
            'kind', r.kind,
            'senderId', r.sender_id,
            'senderName', coalesce(sender.display_name, 'Gym Member'),
            'lobbyOwnerId', r.lobby_owner_id,
            'lobbyOwnerName', coalesce(owner.display_name, sender.display_name, 'Gym Member')
        ) order by r.created_at)
        from public.gym_requests r
        left join public.gym_members sender on sender.user_id = r.sender_id
        left join public.gym_members owner on owner.user_id = r.lobby_owner_id
        where r.recipient_id = auth.uid() and r.status = 'pending'
    ), '[]'::jsonb),
    'lobbies', coalesce((
        select jsonb_agg(jsonb_build_object(
            'ownerId', lm.owner_id,
            'ownerName', coalesce(owner.display_name, 'Gym Member')
        ) order by lower(coalesce(owner.display_name, 'Gym Member')))
        from public.gym_private_lobby_members lm
        left join public.gym_members owner on owner.user_id = lm.owner_id
        where lm.member_id = auth.uid()
    ), '[]'::jsonb)
) end;
$$;

create or replace function public.gym_lobby_send(target_id uuid, action_kind text) returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    sender uuid := auth.uid();
    owner uuid;
begin
    if sender is null then raise exception 'Authentication required'; end if;
    if target_id is null or target_id = sender then raise exception 'Choose another Gym Member'; end if;
    if not exists (select 1 from public.gym_members where user_id = target_id) then raise exception 'Gym Member not found'; end if;
    if action_kind not in ('buddy', 'invite', 'join') then raise exception 'Unsupported lobby action'; end if;
    owner := case when action_kind = 'invite' then sender when action_kind = 'join' then target_id else null end;
    insert into public.gym_requests(kind, sender_id, recipient_id, lobby_owner_id)
    values (action_kind, sender, target_id, owner)
    on conflict (sender_id, recipient_id, kind, (coalesce(lobby_owner_id, '00000000-0000-0000-0000-000000000000'::uuid)))
    do update set status = 'pending', updated_at = now();
end;
$$;

create or replace function public.gym_lobby_respond(request_id uuid, accept_request boolean) returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    request_row public.gym_requests%rowtype;
    joining_member uuid;
begin
    select * into request_row from public.gym_requests
    where id = request_id and recipient_id = auth.uid() and status = 'pending' for update;
    if not found then raise exception 'Request is no longer available'; end if;
    update public.gym_requests set status = case when accept_request then 'accepted' else 'declined' end,
        updated_at = now() where id = request_id;
    if accept_request and request_row.kind in ('invite', 'join') then
        joining_member := case when request_row.kind = 'invite' then request_row.recipient_id else request_row.sender_id end;
        insert into public.gym_private_lobby_members(owner_id, member_id)
        values (request_row.lobby_owner_id, joining_member) on conflict do nothing;
    end if;
end;
$$;

create or replace function public.gym_lobby_leaderboard(lobby_owner uuid) returns jsonb
language sql
security definer
set search_path = public, pg_temp
stable
as $$
select case
    when auth.uid() is null then jsonb_build_object('error', 'Authentication required')
    when not exists (
        select 1 from public.gym_private_lobby_members
        where owner_id = lobby_owner and member_id = auth.uid()
    ) then jsonb_build_object('error', 'Private lobby access required')
    else jsonb_build_object(
        'ownerId', lobby_owner,
        'ownerName', coalesce((select display_name from public.gym_members where user_id = lobby_owner), 'Gym Member'),
        'members', coalesce((
            select jsonb_agg(jsonb_build_object(
                'id', ranked.user_id,
                'name', ranked.display_name,
                'level', ranked.level,
                'points', ranked.momentum_points,
                'apex', ranked.apex_tier,
                'achievements', to_jsonb(ranked.achievements),
                'rank', ranked.position
            ) order by ranked.position)
            from (
                select m.*, row_number() over (
                    order by m.momentum_points desc, m.level desc, lower(m.display_name), m.user_id
                ) as position
                from public.gym_private_lobby_members lm
                join public.gym_members m on m.user_id = lm.member_id
                where lm.owner_id = lobby_owner
            ) ranked
        ), '[]'::jsonb)
    )
end;
$$;

revoke all on function public.gym_lobby_publish(text, integer, integer, text, text[]) from public;
revoke all on function public.gym_lobby_snapshot() from public;
revoke all on function public.gym_lobby_send(uuid, text) from public;
revoke all on function public.gym_lobby_respond(uuid, boolean) from public;
revoke all on function public.gym_lobby_leaderboard(uuid) from public;
grant execute on function public.gym_lobby_publish(text, integer, integer, text, text[]) to authenticated;
grant execute on function public.gym_lobby_snapshot() to authenticated;
grant execute on function public.gym_lobby_send(uuid, text) to authenticated;
grant execute on function public.gym_lobby_respond(uuid, boolean) to authenticated;
grant execute on function public.gym_lobby_leaderboard(uuid) to authenticated;

comment on table public.gym_members is
    'Public Gym Lobby fields only. Never store email, photos, body data, workouts, nutrition, routes, or schedules here.';

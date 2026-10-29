-- Private Gym Lobby messages. No health, workout, body, email, or photo data is stored here.
create table if not exists public.gym_lobby_messages (
    id uuid primary key default gen_random_uuid(),
    sender_id uuid not null references auth.users(id) on delete cascade,
    recipient_id uuid not null references auth.users(id) on delete cascade,
    body text not null check (char_length(trim(body)) between 1 and 500),
    created_at timestamptz not null default now(),
    read_at timestamptz,
    check (sender_id <> recipient_id)
);

create index if not exists gym_lobby_messages_recipient_created
    on public.gym_lobby_messages (recipient_id, created_at desc);

alter table public.gym_lobby_messages enable row level security;
revoke all on public.gym_lobby_messages from anon, authenticated;

create or replace function public.gym_lobby_send_message(target_id uuid, message_body text) returns void
language plpgsql security definer set search_path = public, pg_temp
as $$
declare sender uuid := auth.uid();
begin
    if sender is null then raise exception 'Authentication required'; end if;
    if target_id is null or target_id = sender then raise exception 'Choose another Gym Member'; end if;
    if not exists (select 1 from public.gym_members where user_id = target_id) then raise exception 'Gym Member not found'; end if;
    if not exists (
        select 1 from public.gym_requests r where r.kind = 'buddy' and r.status = 'accepted'
          and ((r.sender_id = sender and r.recipient_id = target_id)
            or (r.recipient_id = sender and r.sender_id = target_id))
    ) and not exists (
        select 1 from public.gym_private_lobby_members mine
        join public.gym_private_lobby_members theirs on theirs.owner_id = mine.owner_id
        where mine.member_id = sender and theirs.member_id = target_id
    ) then
        raise exception 'Messages are available to Gym Buddies or members of the same private lobby';
    end if;
    insert into public.gym_lobby_messages(sender_id, recipient_id, body)
    values (sender, target_id, left(trim(message_body), 500));
end;
$$;

create or replace function public.gym_lobby_mark_messages_read() returns void
language sql security definer set search_path = public, pg_temp
as $$
    update public.gym_lobby_messages set read_at = now()
    where recipient_id = auth.uid() and read_at is null;
$$;

create or replace function public.gym_lobby_inbox() returns jsonb
language sql security definer set search_path = public, pg_temp stable
as $$
select case when auth.uid() is null then jsonb_build_object('error', 'Authentication required') else jsonb_build_object(
    'requests', coalesce((
        select jsonb_agg(jsonb_build_object(
            'id', r.id, 'kind', r.kind, 'senderId', r.sender_id,
            'senderName', coalesce(sender.display_name, 'Gym Member'),
            'lobbyOwnerId', r.lobby_owner_id,
            'lobbyOwnerName', coalesce(owner.display_name, sender.display_name, 'Gym Member')
        ) order by r.created_at)
        from public.gym_requests r
        left join public.gym_members sender on sender.user_id = r.sender_id
        left join public.gym_members owner on owner.user_id = r.lobby_owner_id
        where r.recipient_id = auth.uid() and r.status = 'pending'
    ), '[]'::jsonb),
    'messages', coalesce((
        select jsonb_agg(jsonb_build_object(
            'id', m.id, 'senderId', m.sender_id,
            'senderName', coalesce(sender.display_name, 'Gym Member'),
            'body', m.body, 'createdAt', m.created_at, 'unread', m.read_at is null
        ) order by m.created_at desc)
        from (select * from public.gym_lobby_messages
              where recipient_id = auth.uid() or sender_id = auth.uid()
              order by created_at desc limit 50) m
        left join public.gym_members sender on sender.user_id = m.sender_id
    ), '[]'::jsonb)
) end;
$$;

revoke all on function public.gym_lobby_send_message(uuid, text) from public;
revoke all on function public.gym_lobby_mark_messages_read() from public;
revoke all on function public.gym_lobby_inbox() from public;
grant execute on function public.gym_lobby_send_message(uuid, text) to authenticated;
grant execute on function public.gym_lobby_mark_messages_read() to authenticated;
grant execute on function public.gym_lobby_inbox() to authenticated;

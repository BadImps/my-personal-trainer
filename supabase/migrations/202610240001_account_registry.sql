-- My Personal Trainer stores identity only in Supabase.
-- Health, body, nutrition, route, schedule, coach, and workout data stay on-device.
create table if not exists public.account_registry (
    user_id uuid primary key references auth.users(id) on delete cascade,
    created_at timestamptz not null default now()
);

alter table public.account_registry enable row level security;

create policy "members can read only their account record"
    on public.account_registry for select
    to authenticated
    using ((select auth.uid()) = user_id);

revoke all on table public.account_registry from anon;
revoke all on table public.account_registry from authenticated;
grant select on table public.account_registry to authenticated;

create or replace function public.register_new_auth_user()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    insert into public.account_registry(user_id) values (new.id)
    on conflict (user_id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute procedure public.register_new_auth_user();

comment on table public.account_registry is
    'Authentication registry only. Never store body, workout, nutrition, route, schedule, photo, or coach data here.';

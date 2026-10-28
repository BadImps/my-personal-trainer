-- One opaque, per-account application vault.  The Android client uses the
-- publishable key only; Row Level Security binds every row to auth.uid().
create table if not exists public.member_vaults (
    user_id uuid primary key references auth.users(id) on delete cascade,
    schema_version integer not null default 1 check (schema_version >= 1),
    payload jsonb not null default '{}'::jsonb check (jsonb_typeof(payload) = 'object'),
    client_updated_at timestamptz,
    updated_at timestamptz not null default now()
);

alter table public.member_vaults enable row level security;
alter table public.member_vaults force row level security;

revoke all on table public.member_vaults from anon;
revoke all on table public.member_vaults from authenticated;
grant select, insert, update, delete on table public.member_vaults to authenticated;

create policy "members read own private vault" on public.member_vaults
for select to authenticated using ((select auth.uid()) = user_id);
create policy "members create own private vault" on public.member_vaults
for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "members update own private vault" on public.member_vaults
for update to authenticated using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);
create policy "members delete own private vault" on public.member_vaults
for delete to authenticated using ((select auth.uid()) = user_id);

create or replace function public.touch_member_vault_updated_at()
returns trigger language plpgsql security invoker set search_path = '' as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists member_vaults_touch_updated_at on public.member_vaults;
create trigger member_vaults_touch_updated_at
before update on public.member_vaults
for each row execute procedure public.touch_member_vault_updated_at();

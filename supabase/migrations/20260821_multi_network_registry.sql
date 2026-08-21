begin;

create table if not exists public.monitored_networks (
  id uuid primary key default gen_random_uuid(),
  household_id text not null references public.households(id) on delete cascade,
  network_name text not null,
  ssid text not null,
  bssid text,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint monitored_networks_name_check check (length(trim(network_name)) between 1 and 120),
  constraint monitored_networks_ssid_check check (length(trim(ssid)) between 1 and 255),
  constraint monitored_networks_bssid_check check (
    bssid is null or bssid = '' or bssid ~* '^([0-9a-f]{2}:){5}[0-9a-f]{2}$'
  )
);

create index if not exists monitored_networks_household_idx
  on public.monitored_networks (household_id, is_active);

create unique index if not exists monitored_networks_household_bssid_uidx
  on public.monitored_networks (household_id, lower(trim(bssid)))
  where bssid is not null and trim(bssid) <> '';

create unique index if not exists monitored_networks_household_ssid_uidx
  on public.monitored_networks (household_id, lower(trim(ssid)), lower(trim(coalesce(bssid, ''))));

alter table public.devices
  add column if not exists target_network_id uuid;

alter table public.quota_policies
  add column if not exists target_network_id uuid;

do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conname = 'devices_target_network_id_fkey'
      and conrelid = 'public.devices'::regclass
  ) then
    alter table public.devices
      add constraint devices_target_network_id_fkey
      foreign key (target_network_id)
      references public.monitored_networks(id)
      on delete set null;
  end if;

  if not exists (
    select 1 from pg_constraint
    where conname = 'quota_policies_target_network_id_fkey'
      and conrelid = 'public.quota_policies'::regclass
  ) then
    alter table public.quota_policies
      add constraint quota_policies_target_network_id_fkey
      foreign key (target_network_id)
      references public.monitored_networks(id)
      on delete set null;
  end if;
end $$;

-- Backfill one managed network per household from the existing global Target Wi-Fi setting.
insert into public.monitored_networks (household_id, network_name, ssid, bssid)
select
  g.household_id,
  'الشبكة الحالية',
  trim(g.target_ssid),
  nullif(lower(trim(g.target_bssid)), '')
from public.gateway_system_settings g
where length(trim(g.target_ssid)) > 0
  and not exists (
    select 1
    from public.monitored_networks m
    where m.household_id = g.household_id
      and lower(trim(m.ssid)) = lower(trim(g.target_ssid))
      and lower(trim(coalesce(m.bssid, ''))) = lower(trim(coalesce(g.target_bssid, '')))
  );

-- Existing policies inherit the only seeded household network; new policies will be explicit.
update public.quota_policies q
set target_network_id = m.id
from public.monitored_networks m
where q.target_network_id is null
  and q.household_id = m.household_id
  and not exists (
    select 1
    from public.monitored_networks other
    where other.household_id = m.household_id
      and other.id <> m.id
  );

update public.devices d
set target_network_id = q.target_network_id
from public.quota_policies q
where d.device_key = q.device_key
  and q.target_network_id is not null
  and d.target_network_id is distinct from q.target_network_id;

alter table public.monitored_networks enable row level security;

drop policy if exists monitored_networks_select_member on public.monitored_networks;
create policy monitored_networks_select_member
on public.monitored_networks
for select
to authenticated
using (
  is_household_member(household_id) or is_household_owner(household_id)
);

drop policy if exists monitored_networks_insert_owner on public.monitored_networks;
create policy monitored_networks_insert_owner
on public.monitored_networks
for insert
to authenticated
with check (is_household_owner(household_id));

drop policy if exists monitored_networks_update_owner on public.monitored_networks;
create policy monitored_networks_update_owner
on public.monitored_networks
for update
to authenticated
using (is_household_owner(household_id))
with check (is_household_owner(household_id));

drop policy if exists monitored_networks_delete_owner on public.monitored_networks;
create policy monitored_networks_delete_owner
on public.monitored_networks
for delete
to authenticated
using (is_household_owner(household_id));

commit;

-- ODS Pulse v1 schema. Applied to the ODS-Link-Core project (kfbtpcwakpyptwqqhabu).
-- Default-deny RLS: the pulse edge function uses the service role (bypasses RLS); the app
-- never queries these tables directly.

create table if not exists public.pulse_targets (
  id text primary key,
  label text not null,
  url text not null,
  enabled boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.pulse_checks (
  id bigint generated always as identity primary key,
  target_id text not null references public.pulse_targets(id) on delete cascade,
  url text not null,
  status_code int not null default 0,
  ok boolean not null default false,
  ttfb_ms int not null default 0,
  error text,
  checked_at timestamptz not null default now()
);

create index if not exists pulse_checks_target_time_idx
  on public.pulse_checks (target_id, checked_at desc);

alter table public.pulse_targets enable row level security;
alter table public.pulse_checks enable row level security;

insert into public.pulse_targets (id, label, url) values
  ('website', 'outlined-design.com', 'https://www.outlined-design.com'),
  ('recon',   'ods-recon',           'https://ods-recon.com'),
  ('link',    'ods-link',            'https://ods-link-core.vercel.app'),
  ('aether',  'ods-aether',          'https://ods-aether.vercel.app'),
  ('assist',  'ods-assist',          'https://ods-assist.vercel.app')
on conflict (id) do update set label = excluded.label, url = excluded.url, enabled = true;

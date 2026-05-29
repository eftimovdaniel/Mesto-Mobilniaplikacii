-- Изврши во Supabase → SQL Editor (еднаш по проект).

create table public.companies (
  id bigint generated always as identity primary key,
  name text not null,
  address text not null,
  latitude numeric(10, 7) not null,
  longitude numeric(10, 7) not null,
  email text not null,
  phone text not null,
  website text not null,
  image_url text,
  categories jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now()
);

-- Ако базата веќе постои без image_url:
-- alter table public.companies add column if not exists image_url text;

alter table public.companies enable row level security;

create policy "read_all_companies"
  on public.companies for select
  using (true);

create policy "insert_all_companies"
  on public.companies for insert
  with check (true);

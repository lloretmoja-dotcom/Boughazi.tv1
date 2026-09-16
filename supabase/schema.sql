-- Boughazi TV — esquema Supabase para la app nativa Android TV / móvil

create table if not exists channels (
  id uuid primary key default gen_random_uuid(),
  channel_number int not null unique,
  name text not null,
  category text not null,
  logo_url text,
  stream_url text not null,
  is_premium boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null,
  is_subscribed boolean not null default false,
  subscription_provider text check (subscription_provider in ('stripe', 'paypal')),
  subscription_expires_at timestamptz,
  created_at timestamptz not null default now()
);

-- Se crea automáticamente un perfil al registrarse por Gmail.
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, email)
  values (new.id, new.email);
  return new;
end;
$$ language plpgsql security definer;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- Row Level Security
alter table channels enable row level security;
alter table profiles enable row level security;

-- Los canales son de lectura pública (el filtrado de premium se hace en la app,
-- verificando is_subscribed del perfil antes de reproducir).
create policy "channels: lectura publica"
  on channels for select
  using (true);

-- Cada usuario solo puede ver y editar su propio perfil.
create policy "profiles: lectura propia"
  on profiles for select
  using (auth.uid() = id);

create policy "profiles: actualizacion propia"
  on profiles for update
  using (auth.uid() = id);

-- Los 10 canales premium se marcan así, ejemplo:
-- update channels set is_premium = true where channel_number in (1,2,3,4,5,6,7,8,9,10);

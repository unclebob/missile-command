create table if not exists players (
  id text primary key,
  public_code text not null unique,
  token_hash text not null,
  display_name text not null,
  created_at text not null,
  updated_at text not null
);

create table if not exists scores (
  id text primary key,
  run_id text not null unique,
  player_id text not null references players(id),
  initials text not null,
  score integer not null,
  wave integer not null,
  duration_ms integer,
  game_version text,
  host text not null,
  created_at text not null
);

create index if not exists scores_top_idx on scores(score desc, created_at asc);
create index if not exists scores_created_idx on scores(created_at desc);
create index if not exists scores_player_top_idx on scores(player_id, score desc, created_at asc);

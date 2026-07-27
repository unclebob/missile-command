alter table scores add column ip_hash text;

create index if not exists scores_ip_created_idx on scores(ip_hash, created_at desc);
create index if not exists scores_player_created_idx on scores(player_id, created_at desc);

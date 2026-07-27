# Missile Command Leaderboard Worker

Cloudflare Workers + D1 service for the global Missile Command leaderboard.

The game talks to this Worker, never directly to D1.

## Local Setup

```sh
cd cloudflare/leaderboard-worker
npm install
npm test
npm run d1:migrate:local
npm run dev
```

Local Worker URL:

```text
http://localhost:8787
```

Production Worker URL:

```text
https://missile-command-leaderboard.unclebob-missile-command.workers.dev
```

## API

```text
GET  /health
GET  /leaderboard-info
GET  /leaderboard?limit=10
POST /players
POST /scores
```

Create an anonymous player:

```sh
curl -s http://localhost:8787/players \
  -H 'content-type: application/json' \
  -d '{"display_name":"Uncle Bob"}'
```

Submit a score with the returned `player_id` and `player_token`:

```sh
curl -s http://localhost:8787/scores \
  -H 'content-type: application/json' \
  -d '{
    "player_id":"...",
    "player_token":"...",
    "run_id":"550e8400-e29b-41d4-a716-446655440000",
    "initials":"BOB",
    "score":12345,
    "wave":9,
    "duration_ms":312000,
    "game_version":"1.0.0",
    "host":"desktop"
  }'
```

Accepted score submissions are rate-limited before insertion:

```text
1 per minute per player id
1 per minute per source IP
20 per day per player id
20 per day per source IP
```

Rate-limited submissions return HTTP 429 with `accepted:false`,
`error:"rate_limited"`, the limited `subject`, the limited `window`, and
`retry_after_seconds`.

Read the leaderboard:

```sh
curl -s http://localhost:8787/leaderboard?limit=10
```

## Remote Setup

Authenticate first:

```sh
npx wrangler login
npx wrangler whoami
```

If deploy reports that no `workers.dev` subdomain is registered, open the
Cloudflare onboarding page it prints and choose an account-wide subdomain. This
is required before the Worker can be published to a `*.workers.dev` URL.

Create the D1 database:

```sh
npm run d1:create
```

Copy the generated `database_id` into `wrangler.toml`, replacing:

```text
replace-with-cloudflare-d1-database-id
```

Apply migrations remotely:

```sh
npm run d1:migrate:remote
```

Deploy:

```sh
npm run deploy
```

## Leaderboard Identity

Set these in `wrangler.toml` or Cloudflare environment variables:

```toml
[vars]
LEADERBOARD_ID = "missile-command-official"
LEADERBOARD_DISPLAY_NAME = "Official Missile Command"
LEADERBOARD_OPERATOR = "Uncle Bob"
LEADERBOARD_POLICY = "best-score-per-player"
ALLOWED_ORIGIN = "*"
```

The game displays the service-provided `LEADERBOARD_DISPLAY_NAME` on the
network/global high-score page.

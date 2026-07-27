# Cloudflare Global High Scores Plan

**Task:** `cloudflare-global-high-scores`  
**Behavior change:** add optional global high-score synchronization  
**Hosts:** desktop JVM and ClojureScript browser  

## Problem

High scores are currently local to the host. The desktop host persists an EDN
file and the browser host persists local browser state. Players on different
machines cannot see the same leaderboard.

The global leaderboard must never block play, initials entry, title navigation,
or the local high-score table. A slow or unavailable service must degrade to the
current local behavior.

## Goal

Use Cloudflare Workers + D1 as a lightweight global leaderboard service.

Both hosts should:

- fetch the global leaderboard when the high-score table is displayed
- submit completed qualifying scores only after a global leaderboard read has
  succeeded at least once in the current host profile
- keep local high scores as the immediate fallback
- time out quickly and continue without user-visible failure

## Non-Goals

- Do not require player accounts.
- Do not make the game dependent on the network.
- Do not trust browser clients enough for strong anti-cheat guarantees.
- Do not remove local high-score persistence.

## Service Shape

Create a Cloudflare Worker with D1 binding.

Endpoints:

```text
GET  /leaderboard-info
GET  /leaderboard?limit=10
POST /players
POST /scores
GET  /health
```

`GET /leaderboard-info` response:

```json
{
  "leaderboard_id": "missile-command-official",
  "display_name": "Official Missile Command",
  "operator": "Uncle Bob",
  "policy": "best-score-per-player"
}
```

`GET /leaderboard` response:

```json
{
  "leaderboard": {
    "leaderboard_id": "missile-command-official",
    "display_name": "Official Missile Command",
    "operator": "Uncle Bob",
    "policy": "best-score-per-player"
  },
  "scores": [
    {
      "rank": 1,
      "initials": "ACE",
      "score": 12345,
      "wave": 9,
      "created_at": "2026-07-27T12:00:00Z"
    }
  ]
}
```

`POST /scores` request:

```json
{
  "player_id": "uuid",
  "player_token": "secret",
  "run_id": "uuid",
  "initials": "ACE",
  "score": 12345,
  "wave": 9,
  "duration_ms": 312000,
  "game_version": "1.0.0",
  "host": "browser"
}
```

`POST /scores` response:

```json
{
  "accepted": true
}
```

## D1 Schema

```sql
create table players (
  id text primary key,
  public_code text not null unique,
  token_hash text not null,
  display_name text not null,
  created_at text not null,
  updated_at text not null
);

create table scores (
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

create index scores_top_idx on scores(score desc, created_at asc);
create index scores_created_idx on scores(created_at desc);
create index scores_player_top_idx on scores(player_id, score desc, created_at asc);
```

The Worker stores `player_token` as a hash only. The clear token is returned
once when the player is created and is stored locally by the host.

Server validation:

- player submissions must include a valid `player_id` and token
- initials must be normalized to the same rules as the game
- score must be positive
- wave must be positive
- `host` must be `browser` or `desktop`
- duplicate `run_id` submissions are ignored or returned as already accepted
- reject obvious impossible values with conservative limits
- rate-limit by IP where Cloudflare makes that practical

Leaderboard ranking defaults to one visible row per `player_id`: the best score
for that player. Raw duplicate scores can remain in the `scores` table for
audit/history.

## Game Model

Keep local high scores in the pure core as the source of game behavior.

Add a host-owned global leaderboard state alongside local scores:

```clojure
{:global-high-scores {:status :idle | :loading | :ready | :failed
                      :leaderboard {:id "missile-command-official"
                                    :display-name "Official Missile Command"
                                    :url "https://..."}
                      :scores [...]
                      :last-updated-ms n
                      :error nil}}
```

This state is display metadata. It must not affect:

- whether a score qualifies locally
- whether initials entry appears
- the final score
- title/options/pause/gameplay transitions

## Host Integration

### Shared Contract

Define a small cross-host interface:

```clojure
fetch-global-leaderboard!
submit-global-score!
```

Each function is asynchronous from the game loop perspective.

Required behavior:

- never block draw or update loops
- use a short timeout
- ignore malformed responses
- leave local scores visible on failure
- cache the last successful global leaderboard per host
- do not submit scores until a global leaderboard read has succeeded

Suggested defaults:

- leaderboard fetch timeout: 1000 ms
- score submission timeout: 1500 ms
- no automatic retries during active play
- one retry later when the high-score screen is opened, if a submission failed

### Browser Host

Use `js/fetch` with `AbortController`.

On high-score table open:

1. render local table immediately
2. mark global status `:loading`
3. start `GET /leaderboard`
4. if it succeeds, render global table
5. if it fails or times out, keep local table and set status `:failed`

On initials submit:

1. update local table immediately
2. return to the normal flow immediately
3. if global leaderboard read has succeeded, start `POST /scores` in the
   background
4. if no global read has succeeded, skip network submission and keep the score
   local only
5. record success/failure/skipped in host metadata only

### Desktop Host

Use Java `HttpClient` or another small JVM HTTP client from host code.

The desktop host must follow the same timing rules:

- local update first
- network in background
- no blocking on the Quil animation thread
- quick timeout
- failure is silent except for optional QA telemetry/logging

## Display Behavior

High-score screen should support both local and global scores without making
network state disruptive.

Use two rotating pages. Show one page at a time for 5 seconds, then switch to
the other page and repeat while the high-score screen remains open.

```text
HIGH SCORES - GLOBAL
Official Missile Command

1 ACE 12345
...
```

```text
HIGH SCORES - LOCAL

1 BOB 9000
...
```

If global scores are loading:

```text
GLOBAL
Official Missile Command
loading...
```

If unavailable:

```text
GLOBAL
Official Missile Command
unavailable
```

The local table must still be visible.

If the global page is unavailable, keep the 5-second rotation but show a compact
status page:

```text
HIGH SCORES - GLOBAL
Official Missile Command

Unavailable
```

The local page remains the reliable fallback.

The global page must identify the leaderboard being displayed because players
may configure different community leaderboards. Show at least one of:

- configured display name from `/leaderboard-info` or `/leaderboard`
- configured URL host when no display name has been fetched
- short fallback label such as `Custom leaderboard`

Examples:

```text
HIGH SCORES - GLOBAL
Official Missile Command
```

```text
HIGH SCORES - GLOBAL
scores.example.net
```

Add a manual page toggle if convenient:

- `GLOBAL`
- `LOCAL`

Recommended key: `Tab`.

The first page may be `LOCAL` so the player sees scores immediately. Once a
global read succeeds, the next global page shows the global table.

## Configuration

Add documented host options:

```text
--leaderboard-url URL
--leaderboard-name NAME
--no-global-scores
```

Environment fallback for desktop:

```text
MC_LEADERBOARD_URL
```

Browser configuration options:

- compile-time constant in ClojureScript config, or
- `resources/public/config.json`, fetched at startup, or
- a JavaScript global in `index.html`

Configuration must allow users/builds to point at a different compatible
leaderboard service. The high-score screen must display which leaderboard is
configured, and then replace that fallback label with the service-provided
display name after the first successful metadata or leaderboard read.

Default development URL:

```text
http://localhost:8787
```

Production URL:

```text
https://<worker-name>.<account>.workers.dev
```

## QA and Telemetry

Add QA telemetry fields:

```text
global_scores_status=idle|loading|ready|failed
global_leaderboard_name=<display-safe-name>
global_leaderboard_url=<configured-url>
global_scores_count=10
global_scores_error=timeout|http|parse|none
global_score_submit_status=idle|pending|accepted|failed|skipped_no_read
```

Add QA event support only where needed:

```text
open-high-scores
initials ABC
```

Use a local fake leaderboard server or Worker dev server for end-to-end QA.

Required QA cases:

1. Service available: opening high scores eventually shows global scores.
2. Service slow: local table appears immediately and gameplay/title navigation
   is not blocked.
3. Service down: local table remains usable and no crash occurs.
4. Submit success: initials entry updates local table immediately and global
   submission is recorded accepted after a successful global read.
5. Submit before first successful read: initials entry updates local table and
   no network submission is attempted.
6. Submit timeout/failure after successful read: local table remains updated
   and the flow continues.
7. High-score screen rotates `LOCAL` and `GLOBAL` pages every 5 seconds.
8. Global page identifies the configured leaderboard by service display name or
   URL fallback.
9. Browser and desktop both use the same API contract.

## Implementation Stages

### Stage 1 - Contract and Feature Spec

- Add Gherkin for global high-score fetch and submit behavior.
- Specify failure/timeout behavior explicitly.
- Specify rotating local/global high-score pages.
- Specify that global submission is skipped until a global read succeeds.
- Add QA procedure and script plan.

Done when:

- local fallback behavior is specified
- browser and desktop expectations match
- no scenario requires network success for normal navigation
- no scenario submits globally before the first successful global read

### Stage 2 - Worker and D1

- Add `cloudflare/leaderboard-worker/` or equivalent service directory.
- Add Worker code.
- Add D1 migration.
- Add local dev instructions using Wrangler.
- Add unit tests for validation, sorting, duplicate `run_id`, and malformed
  payloads.

Done when:

- `GET /leaderboard`, `POST /scores`, and `GET /health` work locally
- `/leaderboard-info` exposes display name and policy metadata
- validation rejects obvious bad submissions
- duplicate run IDs are idempotent

### Stage 3 - Shared Host Contract

- Add shared data shape for global scores.
- Add host-facing functions or events for fetch/submit status.
- Keep pure core independent from HTTP and Cloudflare details.

Done when:

- core game logic does not require network code
- host metadata can represent loading, ready, and failed global states

### Stage 4 - Browser Host

- Implement `fetch` based leaderboard fetch and score submission.
- Add timeout with `AbortController`.
- Add browser config for endpoint URL and disable flag.
- Render local scores immediately.

Done when:

- browser high-score screen works with service available
- browser high-score screen remains usable when service is down or slow

### Stage 5 - Desktop Host

- Implement background HTTP fetch and submit.
- Use short request timeouts.
- Ensure no network call runs on the Quil draw/update path.
- Add CLI/env configuration.

Done when:

- desktop high-score screen works with service available
- desktop high-score screen remains usable when service is down or slow

### Stage 6 - QA and Documentation

- Add fake-service QA or local Worker QA path.
- Add README setup/deploy instructions.
- Add Cloudflare secret/config documentation.
- Add troubleshooting notes for offline mode.

Done when:

- QA can verify success, down, and slow-service paths
- users know how to run without global scores
- users know how to configure a production Worker URL

## Risks

- Cheating is possible from browser clients.
- Global scores can become spammy without rate limits or moderation.
- CORS must be configured correctly for the browser host.
- Desktop async state must be thread-safe with the Quil host state atom.
- Network status display must not create layout clutter.

## Future Hardening

- Add signed server-issued run tokens.
- Add replay transcript submission and server-side replay.
- Add moderation/admin delete endpoint.
- Add per-version leaderboards.
- Add daily/weekly leaderboards.

## Done When

- Global high scores are visible in both hosts when the service is available.
- Local high scores remain visible immediately.
- Service outage, slow response, malformed response, or submission failure never
  blocks play or navigation.
- Score submission never delays initials entry completion.
- The Cloudflare service can be run locally and deployed reproducibly.

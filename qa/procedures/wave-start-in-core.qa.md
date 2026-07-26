# QA: Wave start owned by core

**Task:** `wave-start-in-core`  
**Suite:** continuous play / waves-and-rearm / sequential attacks  
**Gherkin:** `features/waves-and-rearm.feature`, `features/wave-banner.feature`  
**Plan:** `docs/architecture/plans/pr-01-wave-start-in-core.md`

Verify that **core `tick`** starts attack 1 when the sky is empty, no attack is active, and the wave is incomplete. Hosts call only `handle` / `tick` for continuous play — no host-side wave-start policy.

## Rules

| Item | Detail |
|------|--------|
| Attack 1 start | Core `ensure-wave-attack-started` inside playing `tick` |
| Host policy | No `ensure-wave-enemies` / host `activate-wave-schedule` loops |
| After banner | Resume playing → next wave attack 1 starts via tick |
| Attacks 2..N | Already advanced inside tick (unchanged) |

## Preconditions

- PR 1 implementation: `wave-schedule/ensure-attack-started` + core tick integration.
- Hosts gutted of ensure-wave-enemies.

## UI Event Boundary

- Telemetry: `screen`, `wave`, `wave_attack`, `ballistic_missiles`.
- Events: `start`, waits, `quit` only — hosts must not inject schedule activation beyond handle/tick.

## Procedure

### A. Automated — unit + acceptance + arch-check + property tests.

### B. Static — JVM and browser hosts contain no `ensure-wave-enemies` and do not call `activate-wave-schedule`.

### C. Host start — `start` then wait → `playing`, `wave_attack=1`, positive ballistics (tick-started attack 1).

### D. Continuous after banner — stage final attack clear path → WAVE banner → after banner `playing` with `wave=2` and `wave_attack=1`.

## Pass criteria

- Unit, accept, arch, and property green.
- Host B–D hold.

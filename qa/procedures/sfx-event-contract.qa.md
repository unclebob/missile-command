# QA: SFX event contract

**Task:** `sfx-event-contract`  
**Suite:** sound-events  
**Gherkin:** `features/sound-events.feature`  
**Plan:** `docs/architecture/plans/pr-02-sfx-event-contract.md`

Verify hosts learn which SFX to play each frame via the **documented cursor contract**: cumulative `:sfx-events`, `take-new` from previous log length, optional `truncate-to` / `drain`. No dual mental model (`:events` is not the SFX log).

## Rules

| Item | Detail |
|------|--------|
| Source of truth | `:sfx-events` on state |
| Host play path | `(take-new state prev-count)` then advance cursor |
| Helpers | `take-new`, `truncate-to`, `drain` in `sfx.cljc` (re-exported on core) |
| Tests | Full cumulative log still available for acceptance |
| Audible behavior | Unchanged (same events, same mute rules) |

## Preconditions

- PR 2 implementation: helpers + host wiring + ns docstring.
- Existing sound-events catalog remains valid.

## UI Event Boundary

- Telemetry: `sfx_count=`, `sfx_last=`, `qa-sfx type=… played=…`.
- Host QA must show play attempts only for **new** events since the previous frame (cursor).

## Procedure

### A. Automated — unit + acceptance (sound-events) + arch-check + property tests.

### B. Static — `sfx.cljc` documents the contract; JVM and browser hosts call `sfx-take-new` / `core/sfx-take-new` (not raw log slicing alone).

### C. Host fire — start + fire → `qa-sfx type=sfx/launch played=true` when unmuted (take-new path live).

### D. Mute still suppresses play only — muted fire → `played=false` with core log still showing launch.

## Pass criteria

- Unit, accept, arch, property green.
- Host B–D hold.

# PR 1 — Wave start owned by core

**Task:** `wave-start-in-core`  
**Priority:** P0  
**Depends on:** PR 0 (docs)  
**Behavior change:** none for players (same continuous-play outcomes); hosts stop owning attack-1 policy

## Goal

Hosts call only `handle` / `tick` for continuous play. Core starts attack 1 when the sky is empty, no attack is in progress, and the wave is not complete. Attacks 2..N already advance via `maybe-advance-wave-attack` inside `tick`.

## Current problem

| Location | Policy |
|----------|--------|
| `browser/main.cljs` `ensure-wave-enemies` | If playing, empty sky, no `:wave-attack`, not complete → `activate-wave-schedule` |
| `jvm/sketch.clj` `ensure-wave-enemies` | Same (plus QA paths) |
| `core/tick` | Advances attack 2..N only |

Risk: hosts diverge; QA must know host conventions.

## Design

### Pure helper (prefer `wave-schedule`)

```clojure
(defn needs-attack-start?
  "True when playing continuous fire should begin (or re-begin) attack 1."
  [state]
  (and (sky-clear? state)
       (nil? (current-attack state))
       (not (:wave-complete? state))))

(defn ensure-attack-started
  "If needs-attack-start?, invoke begin-attack-fn (usually begin attack 1).
  begin-attack-fn: (fn [state] → state) that starts attack 1."
  [state begin-attack-fn]
  (if (needs-attack-start? state)
    (begin-attack-fn state)
    state))
```

Note: do **not** call this while screen is not playing. Core `tick` already branches on `playing?`.

### Core integration

In `tick` **playing** path, after combat ticks and after `maybe-advance-wave-attack` / `maybe-complete-wave` (order matters):

**Recommended order:**

1. advance clock  
2. defensive missiles, fireballs, destroyables  
3. enemy missiles, flyers  
4. `maybe-advance-wave-attack` (2..N when current cleared)  
5. `maybe-complete-wave` (last attack cleared → banner)  
6. **`ensure-attack-started`** (sky clear, nil attack, not complete → attack 1)  
7. `evaluate-game-over`

Why ensure after complete: completing a wave sets `:wave-attack nil` and leaves banner (not playing). When play resumes after banner, next `tick` while playing should start attack 1. If ensure runs while still playing with complete false and empty sky after last enemy of attack 3—`maybe-complete-wave` should fire first and enter banner before ensure starts a new attack on the same wave. Confirm `wave-ready-to-complete?` vs `needs-attack-start?` mutual exclusion:

- After last attack clear: `wave-ready-to-complete?` true → complete sets complete flag / banner / nil attack  
- After complete while still playing (if any): `wave-complete?` true → needs-attack-start? false  
- After banner `start-next-wave`: complete false, attack nil, sky clear → ensure starts attack 1  

If complete path leaves screen as wave-banner, ensure is not run until playing again. Good.

### Host changes

**Delete or gut** `ensure-wave-enemies` in:

- `src/missile_command/browser/main.cljs`
- `src/missile_command/jvm/sketch.clj`

Remove all call sites:

- after start-game  
- after banner finishes  
- after click-to-start  
- draw/update loops that currently call ensure  

**Keep** explicit `activate-wave-schedule` / `begin-wave-attack` for:

- unit tests  
- QA scenario EDN (`:wave-attack` already uses `begin-wave-attack`)  
- optional force-spawn if any step still needs it  

Hosts must not re-implement sky-empty policy.

### Tests to add/adjust

| Test | Intent |
|------|--------|
| Unit: playing + empty sky + nil attack + not complete → after tick, wave-attack = 1 and enemies present | ensure works inside tick |
| Unit: wave-attack already 1, sky has enemies → tick does not double-spawn | guard |
| Unit: wave-complete? true → tick does not activate | guard |
| Unit: after attack 1 cleared, wave-attack becomes 2 (existing) | no regression |
| Host: no private ensure required for continuous play | code review / delete dead fns |
| QA scripts: `waves-and-rearm`, `sequential-attacks-banner`, `play-wave-schedule` | still pass |

### Files to touch

| File | Change |
|------|--------|
| `wave_schedule.cljc` | `needs-attack-start?`, `ensure-attack-started` |
| `core.cljc` | call ensure on playing tick path; optional public re-export for tests |
| `browser/main.cljs` | remove ensure |
| `jvm/sketch.clj` | remove ensure |
| `spec/.../core_spec.clj` or `wave_schedule_spec.clj` | new examples |
| `wave_schedule_spec.clj` | pure helper tests |

### Out of scope

- Seedable RNG  
- Changing 3×3 schedule  
- SFX event model  

### Verification checklist

- [ ] `bb script/arch_check.bb`
- [ ] Unit: `core_spec`, `wave_schedule_spec`, `waves_spec`
- [ ] Acceptance: waves-and-rearm, wave-banner, sequential-attacks-banner related
- [ ] Manual/QA: start game → clear wave 1 salvos → banner → wave 2 enemies appear without host ensure
- [ ] JVM + browser both continuous-play

### Coder notes

- Prefer pure functions in `wave-schedule` + thin core glue.
- Grep for `ensure-wave-enemies` and `activate-wave-schedule` after edit; hosts should not call activate on every frame.
- Watch double-spawn: ensure only when attack is nil.

### Done when

Playing continuous mode works with hosts only calling `handle`/`tick` (plus start/aim/fire). No host file contains wave-start policy. Specs green.

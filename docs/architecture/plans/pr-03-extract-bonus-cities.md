# PR 3 — Extract `bonus-cities` module

**Task:** `extract-bonus-cities`  
**Priority:** P1  
**Depends on:** PR 1 (soft)  
**Behavior change:** none (award mid-wave, place only at wave end — already rules)

## Goal

Move bonus city award/place logic out of `core.cljc` into `missile-command.bonus-cities`.

## Rules to preserve

| Moment | Behavior |
|--------|----------|
| Score crosses threshold | +1 reserve, earned event, SFX; **no** place |
| Wave resolution | `apply-bonus-cities-from-reserve` fills destroyed slots up to 6 |
| Game over eval | living=0 and reserve=0 → THE END; **no** place mid-eval |
| Place | lowest destroyed city id first; set banner flag when any placed |

## Move from core

| Function | Notes |
|----------|--------|
| `sync-bonus-cities-from-score` | award only |
| `apply-bonus-cities-from-reserve` | place + `:bonus-city-for-banner?` |
| `lowest-destroyed-city-id` | private to module |
| Threshold/reserve long helpers if only used here | |

## API sketch

```clojure
(ns missile-command.bonus-cities
  (:require [missile-command.scoring :as scoring]
            [missile-command.sfx :as sfx]
            ...))

(defn award-from-score [state score threshold already-awarded] ...)
(defn sync-from-score [state] ...) ;; uses state keys
(defn apply-from-reserve [state living-cities-fn update-city-fn city-count] ...)
```

Prefer **state in/out** with minimal hooks. If cycle risk with cities/core, inject `living-cities` / `update-city` like wave-lifecycle does today.

## Call sites

| Caller | After |
|--------|--------|
| `add-score` / `set-score` | `bonus-cities/sync-from-score` |
| `wave-lifecycle/complete-wave` | call module place fn (drop inject if possible) |
| `core` | re-export `apply-bonus-cities-from-reserve`, `set-bonus-city-*` for acceptance |

## Tests

- Acceptance: `bonus-cities.feature`, `the-end.feature` (reserve prevents THE END without place)
- Unit: existing core_spec bonus describe → require new ns or re-exports
- Property: wave-end place only (existing)

## Files

| File | Action |
|------|--------|
| `src/missile_command/bonus_cities.cljc` | **create** |
| `core.cljc` | delete moved impl; re-export |
| `wave_lifecycle.cljc` | call bonus-cities if deps allow |
| specs | update requires if needed |

## Verification

- [ ] Arch check (no host deps)  
- [ ] bonus-cities + the-end acceptance  
- [ ] wave banner still shows Bonus City when place at end  

## Done when

No award/place algorithm body left in core; re-exports keep steps working; behavior unchanged.

# PR 8 — Seedable sky RNG

**Task:** `seedable-sky-rng`  
**Priority:** P2  
**Depends on:** PR 1; builds on random sky origins  
**Behavior change:** optional deterministic salvos when seed set; default remains random

## Goal

QA and property tests can fix sky entry positions via seed without changing default arcade feel.

## Design

### State

```clojure
;; optional
:rng {:seed long :state ...}  ; or :rng-fn (fn [] double in [0,1))
```

### API

```clojure
;; waves.cljc
(defn random-sky-origin-x
  ([width] (random-sky-origin-x width rand))
  ([width rand-fn] ...))

;; core / wave-schedule: when spawning
(let [rand-fn (or (rng/next-fn state) rand)
      x (waves/random-sky-origin-x width rand-fn)
      state (rng/advance state)]  ; if seed-based
  ...)
```

Prefer a tiny `missile-command.rng` pure module (linear congruential or splitmix) for CLJC portability.

### Scenario / QA

```edn
{:rng-seed 42
 :wave-attack 1}
```

Apply in `jvm` scenario loader → `core/with-rng-seed` or `assoc` seed.

### Tests

- Same seed → identical origin xs for `set-wave-enemies-active` n=3  
- Different seeds → different xs (almost always)  
- No seed → still in [0, width)  

## Files

| File | Change |
|------|--------|
| `rng.cljc` | **create** |
| `waves.cljc` | already has injectable rand-fn |
| `wave_schedule.cljc` | pass rng from state |
| `core.cljc` | thread rng on spawn paths |
| `jvm/input` scenario | `:rng-seed` |
| specs | seed determinism |

## Out of scope

- Full game replay  
- Randomizing targets (only sky x)  

## Verification

- [ ] Unit: fixed seed reproducibility  
- [ ] QA sequential script still passes with random default  
- [ ] Optional QA scenario with seed for golden telemetry  

## Done when

Default play is random; seeded path is documented and tested.

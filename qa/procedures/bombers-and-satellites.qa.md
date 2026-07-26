# QA: Bombers and satellites

**Task:** `bombers-and-satellites`  
**Suite:** bombers-and-satellites  
**Gherkin:** `features/bombers-and-satellites.feature`

Verify **bomber** and **satellite** flyers: traverse the sky on a path; drop enemy missiles during a pass; fireballs destroy flyers; kill awards **100 × multiplier** (same for bomber and satellite); destroying a flyer stops further drops; dropped missiles behave as normal enemies; later waves schedule flyers.

## Rules summary

| Behavior | Detail |
|----------|--------|
| Motion | Flyer moves from start toward end along a sky path (constant altitude in examples) |
| Drops | At documented path progress, spawn N enemy missiles from flyer position toward cities |
| Destroy | Fireball intersection destroys flyer (no smart-bomb evasion for flyers) |
| Score | **100 × mult** per bomber or satellite destroyed |
| After kill | No further drops from that flyer; missiles already dropped continue |
| Schedule | Early/mid waves: 0; later waves introduce bombers then satellites |

Out of scope: new weapons (only fireballs destroy flyers).

## Preconditions

- US-07/US-08 enemies and waves; US-09 for score asserts.
- `bb play --qa` with scenario / events / speed.

## UI Event Boundary

- Scenario + events + telemetry only.
- Telemetry: flyer kind (`bomber` / `satellite`), position, path progress; dropped `enemy_missiles`; destroy fate; `score=` / `multiplier=`.

## Scenario staging

**Bomber cross:**
```edn
{:flyers [{:kind :bomber :from [0 80] :to [800 80] :speed 100
           :drops [{:at-progress 0.4 :target [:city 0]}]}]}
```

**Satellite cross (right→left):**
```edn
{:flyers [{:kind :satellite :from [800 50] :to [0 50] :speed 120
           :drops [{:at-progress 0.35 :target [:city 2]}]}]}
```

**Early destroy (before drop progress):**
```edn
{:flyers [{:kind :bomber :from [0 80] :to [800 80] :speed 100
           :drops [{:at-progress 0.6 :target [:city 0]}
                   {:at-progress 0.8 :target [:city 1]}]}]}
```
```text
aim 100 80
key x
wait 1.5
quit
```

**Score (wave open):**
```edn
{:wave 1
 :flyers [{:kind :bomber :from [0 80] :to [800 80] :speed 100}]
 :enemies [{:target [:city 0]}]}
```

## Procedure

### A. Automated — unit + acceptance (`bombers-and-satellites`) + arch-check.

### B. Motion — bomber/satellite advances along path; altitude stable.

### C. Drops — after drop progress, enemy missiles present; origins match flyer position at drop.

### D. Destroy flyer — fireball on path; flyer gone.

### E. Score — destroy with another threat still active → **100 × mult** (bomber and satellite).

### F. Stop drops — kill before drop progress → 0 dropped missiles.

### G. Dropped impact — unintercepted drop destroys target city.

### H. Schedule — waves 1/5 → 0/0; wave 8 → bomber; wave 10 → bomber + satellite (per table).

## Pass criteria

- Acceptance passes.
- Motion, drop, kill, score 100×mult, stop-drop, and schedule match Gherkin.

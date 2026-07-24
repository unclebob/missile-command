# QA: Smart bombs

**Task:** `smart-bombs`  
**Suite:** smart-bombs  
**Gherkin:** `features/smart-bombs.feature`

Verify smart bombs: move toward cities/batteries; **well-centered** fireballs destroy them; **edge-of-blast** fireballs are **evaded once** (deterministic); after evasion they remain a threat; a later centered blast can still kill; unintercepted smart bombs destroy targets; kill awards **125 × multiplier**; later waves schedule them.

## Evasion model (locked by examples)

| Region (distance *d* from fireball center) | Result |
|--------------------------------------------|--------|
| `d ≤ center_limit` (well centered) | Destroyed |
| `edge_inner < d ≤ radius` (edge band) | Evade once (steer clear); not destroyed by that blast |
| `d > radius` | No contact |

- Evasion is **once per fireball threat**; a new well-centered fireball can destroy the bomb afterward.
- Constants appear as Gherkin parameters (`radius`, `center_limit`, `edge_inner`).

## Scoring

| Event | Base × multiplier |
|-------|-------------------|
| Destroy smart bomb | **125** × mult |

Keep another threat alive when checking kill score so wave-end bonuses do not mix in.

## Wave schedule (intent)

| Waves | Smart bombs |
|-------|-------------|
| Early / mid (e.g. 1–6) | 0 |
| Later (e.g. 7+) | ≥1, rising with wave |

Out of scope: flyers (US-13).

## Preconditions

- US-06 fireballs, US-08 waves; US-09 scoring for points check.
- `bb play --qa` with scenario/events/speed.

## UI Event Boundary

- Scenario + events + telemetry only.
- Telemetry: identify smart bombs (`enemy_kind=smart` or equivalent), position, target, evade/destroy fate; `score=`, `multiplier=`.
- Look-and-feel: **request approval** for smart-bomb motion/evade readability.

## Scenario staging

**Smart bomb only:**
```edn
{:enemies [{:kind :smart :target [:city 1]}]}
```

**Centered kill (after route through blast center):**
```edn
{:enemies [{:kind :smart :target [:city 1]}]}
```
```text
aim 400 250
key x
wait 2
quit
```

**Edge evade then second shot:** stage smart bomb; first detonation at edge; second detonation centered on new path.

**Score (wave open):**
```edn
{:wave 1
 :enemies [{:kind :smart :target [:city 1]}
           {:target [:city 0]}]}
```

## Procedure

### A. Automated — unit + acceptance (`smart-bombs`) + arch-check.

### B. Flight — assert smart bomb progresses toward city.

### C. Centered destroy — fireball path within `center_limit`; bomb destroyed; city living.

### D. Edge evade — path only in edge band; bomb evades; still in flight; city living.

### E. Post-evade — still progressing; second centered blast destroys it.

### F. Impact — unintercepted → target city dead.

### G. Score — centered kill with second enemy still up → score **125 × mult** only.

### H. Schedule — waves 1/4 → 0 smart; 7+ → ≥1 per table.

### I. Look-and-feel — **request user approval**.

## Pass criteria

- Acceptance passes; centered vs edge outcomes match parameters.
- Score 125×mult; impact and schedule rules hold.
- User approves readability if drawn.

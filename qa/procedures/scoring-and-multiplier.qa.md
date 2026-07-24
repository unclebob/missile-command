# QA: Scoring and multiplier

**Task:** `scoring-and-multiplier`  
**Suite:** scoring-and-multiplier  
**Gherkin:** `features/scoring-and-multiplier.feature`

Verify score starts at 0, multiplier starts at 1× and follows the wave schedule ( +1× every two waves, max 6× ), destroying an enemy missile awards **25 × multiplier**, wave-end awards **5 × mult per unused defensive missile** and **100 × mult per living city**, and score never decreases.

## Design values (base, before multiplier)

| Event | Points |
|-------|--------|
| Destroy enemy missile | 25 |
| Unused defensive missile at wave end | 5 |
| Surviving city at wave end | 100 |

Multiplier: `min(6, 1 + floor((wave - 1) / 2))`  
(waves 1–2 → 1×, 3–4 → 2×, …, 11+ → 6×).

Out of scope for this suite: bomber/satellite/smart-bomb point types (later stories), bonus cities (US-10), full HUD polish (US-17).

## Preconditions

- US-08 present (waves, rearm, enemy spawn).
- Documented QA launch (README):
  - `bb play --qa` — telemetry on
  - `--qa-scenario <file.edn>` — ammo, enemies, wave
  - `--qa-events <file>` — aim/fire/wait
  - `--qa-speed <n>` — faster sim for waits

## UI Event Boundary

- Automated: documented unit/acceptance only.
- Running app: **scenario + events + telemetry** (no private core API).
- Score/multiplier must appear in **QA telemetry** (and may already show on HUD; full HUD layout is US-17).
- Look-and-feel: optional human check that score/multiplier are readable if drawn.

## Scenario staging

**Wave 1, depleted ammo, one enemy (wave-end bonus after impact):**
```edn
{:wave 1
 :batteries {:left {:ammo 5} :center {:ammo 5} :right {:ammo 5}}
 :enemies [{:target [:city 0]}]}
```

**Wave 3 (2× multiplier), full ammo, one enemy:**
```edn
{:wave 3
 :batteries {:left {:ammo 10} :center {:ammo 10} :right {:ammo 10}}
 :enemies [{:target [:city 0]}]}
```

**Intercept kill (no wave-end yet — keep a second threat or quit before clear):**
```edn
{:wave 1
 :enemies [{:target [:city 1]} {:target [:city 2]}]}
```
```text
aim 400 250
key x
wait 2
quit
```

## Telemetry

Expect `score=` and `multiplier=` (names may match README conventions once documented). Also `wave=`, ammo, `cities_alive=` / living city counts for bonus checks.

| Check | Expectation |
|-------|-------------|
| New game | `score=0`, `multiplier=1`, `wave=1` |
| Kill (25×mult) | score increases by 25, 50, or 75 at mult 1/2/3 while wave incomplete |
| Wave end | score += `(sum non-destroyed battery ammo)*5*mult + living_cities*100*mult` |
| Impact after kill | score does not drop |

Wave-end uses ammo **before rearm** and living cities **after** impacts for that wave.

## Procedure

### A. Automated

1. Unit tests — success.
2. Acceptance including `scoring-and-multiplier` — success.
3. Arch-check if documented — success.

### B. Initial state

4. `bb play --qa` (or empty scenario). Assert telemetry `score=0`, `multiplier=1`, `wave=1`.

### C. Kill scoring

5. Scenario with ≥2 enemies; create fireball on one path via events; assert one enemy destroyed and score += **25 × mult** while another enemy remains (wave not complete).
6. Repeat or stage wave 3 and confirm kill awards **50**.

### D. Wave-end bonuses

7. Stage known ammo and one enemy; let enemy impact (no defensive kill).
8. Assert wave complete (or next wave) and score equals  
   `(3 * ammo * 5 + living_cities * 100) * mult`  
   with living cities reduced if a city was hit (e.g. ammo 10, mult 1, 5 cities → **650**).
9. Stage wave 3, ammo 10, one impact → expect **1300** at 2×.

### E. Score never decreases

10. After a kill points award, aim/move without scoring; score unchanged.
11. Allow remaining threats to resolve; score stays ≥ kill total (grows only by lawful bonuses).

### F. Multiplier schedule / cap

12. Scenario or host setup at waves 1, 2, 3, 11, 13 — assert mult 1, 1, 2, 6, 6.

### G. Look-and-feel (optional)

13. If score/multiplier are drawn, confirm readable; **request user approval** if visible.

## Pass criteria

- Acceptance for `scoring-and-multiplier` passes.
- Telemetry (or documented UI) shows score and multiplier consistent with design table.
- Kill and wave-end awards match parameterized examples.
- Score never decreases across non-scoring actions and impacts.

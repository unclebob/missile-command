# QA: Bonus cities

**Task:** `bonus-cities`  
**Suite:** bonus-cities  
**Gherkin:** `features/bonus-cities.feature`

Verify bonus cities: every **threshold** points (default **10000**) awards one city to **reserve**; reserve restores destroyed cities when earned and after wave resolution; living cities never exceed **six**; extras stay in reserve; a **bonus-city earned** event is recorded for later SFX.

Depends on **US-09** scoring (score can increase). Out of scope: THE END (US-14), full HUD (US-17).

## Rules summary

| Rule | Detail |
|------|--------|
| Award | `floor(score / threshold)` total awards over the run; each new threshold crossed → +1 reserve + 1 earned event |
| Default threshold | 10000 (configurable) |
| Place from reserve | While living cities &lt; 6 and reserve &gt; 0, restore one destroyed city (lowest index first), decrement reserve |
| When to place | Immediately when a bonus is **earned** if living &lt; 6; and after **wave resolution** for remaining reserve |
| Cap | Living cities never exceed 6 |

## Preconditions

- US-09 score/multiplier available.
- Documented QA launch:
  - `bb play --qa`
  - `--qa-scenario <file.edn>` — score, destroyed cities, reserve, wave, enemies
  - `--qa-events` / `--qa-speed` as needed

## UI Event Boundary

- Automated: unit + acceptance only.
- Running app: scenario + events + telemetry only (no private core API).
- Telemetry must expose at least: `score=`, `bonus_cities=` / reserve, living city count, and a bonus-city earned signal when awarded (event log or telemetry flag).
- Look-and-feel: optional if cities reappear visibly; **request approval** if host draws restored cities.

## Scenario staging

**Near threshold, all cities up (reserve only):**
```edn
{:score 10000}
```

**One city down, then threshold (restore on earn):**
```edn
{:score 10000
 :cities {:destroyed [0]}}
```
(or score applied after destroy via events / staged order documented by host)

**Two cities down, stacked awards:**
```edn
{:score 30000
 :cities {:destroyed [0 1]}}
```
Expect living 6 and reserve 1 after earn+place.

**Wave resolution apply remaining reserve:**
```edn
{:bonus-cities 2
 :cities {:destroyed [0 1]}
 :enemies [{:target [:battery :left]}]}
```
Complete wave; assert living cities increase from reserve without exceeding 6.

## Telemetry / events

| Field / signal | Use |
|----------------|-----|
| `score=` | Threshold crossings |
| Reserve / `bonus_cities=` | Cities held, not yet placed |
| `cities_alive=` | Living count ≤ 6 |
| Bonus earned | Count of threshold awards (SFX later) |

## Procedure

### A. Automated

1. Unit tests — success.
2. Acceptance including `bonus-cities` — success.
3. Arch-check if documented — success.

### B. Threshold awards

4. New game: reserve 0, living 6, score 0.
5. Raise score to 9999 (scenario or staged): reserve still 0.
6. Raise to 10000: reserve 1 (all cities living) **or** living still 6 and reserve 1; one earned event.
7. Raise to 30000 in one step: three awards total (events = 3; reserve 3 if no restores needed).

### C. Restore on earn

8. Destroy one city (impact or scenario); living 5.
9. Cross one threshold: that city (or a destroyed slot) living again; reserve 0; living 6.

### D. Cap at six

10. Destroy two cities; award three thresholds worth: living 6, reserve 1 (two placed, one held).

### E. Wave resolution

11. Stage reserve &gt; 0 with destroyed cities; complete a wave (battery target preferred so city count only changes via restore).
12. Assert reserve applied until living 6 or reserve empty.

### F. Look-and-feel

13. If restored cities appear on screen, confirm readable; **request user approval**.

## Pass criteria

- Acceptance for `bonus-cities` passes.
- Threshold, reserve, restore, and six-city cap match design.
- Bonus-city earned events recorded once per threshold crossed.
- Living cities never exceed six.

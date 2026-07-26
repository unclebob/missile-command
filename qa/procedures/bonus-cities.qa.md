# QA: Bonus cities

**Task:** `bonus-cities`  
**Suite:** bonus-cities  
**Gherkin:** `features/bonus-cities.feature`

Verify bonus cities: every **threshold** points (default **10000**) awards one city to **reserve** only; reserve **does not** restore destroyed cities mid-wave; restore from reserve happens **only after wave resolution** (end of wave); living cities never exceed **six** when placing; extras stay in reserve; a **bonus-city earned** event is recorded for later SFX.

Depends on **US-09** scoring (score can increase). Out of scope: THE END (US-14), full HUD (US-17).

## Rules summary

| Rule | Detail |
|------|--------|
| Award | `floor(score / threshold)` total awards over the run; each new threshold crossed → +1 reserve + 1 earned event |
| Default threshold | 10000 (configurable) |
| When to award | Immediately when score crosses a threshold (reserve only) |
| When to place | **Only** after **wave resolution** (end of wave), while living cities &lt; 6 and reserve &gt; 0 |
| Place from reserve | Restore one destroyed city (lowest index first), decrement reserve; repeat until full or reserve empty |
| Cap | Living cities never exceed 6 |
| Mid-wave | Earned reserve does **not** restore cities until the wave ends |

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

## Scenario staging

**Near threshold, all cities up (reserve only):**
```edn
{:score 10000}
```

**One city down, then threshold (no restore until wave end):**
```edn
{:score 10000
 :cities {:destroyed [0]}}
```
Expect living still 5, reserve 1, city 0 still destroyed.

**Two cities down, stacked awards (still no mid-wave place):**
```edn
{:score 30000
 :cities {:destroyed [0 1]}}
```
Expect living 4 and reserve 3 until wave resolution.

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
6. Raise to 10000: reserve 1 (all cities living); one earned event.
7. Raise to 30000 in one step: three awards total (events = 3; reserve 3 if no restores needed).

### C. No mid-wave restore on earn

8. Destroy one city (impact or scenario); living 5.
9. Cross one threshold: living still 5; reserve 1; destroyed city still down.

### D. Stacked awards stay in reserve

10. Destroy two cities; award three thresholds worth mid-wave: living 4, reserve 3 (nothing placed yet).

### E. Wave resolution

11. Stage reserve &gt; 0 with destroyed cities; complete a wave (battery target preferred so city count only changes via restore).
12. Assert reserve applied until living 6 or reserve empty.
13. With three awards and two destroyed: after wave end living 6, reserve 1.

## Pass criteria

- Acceptance for `bonus-cities` passes.
- Threshold awards go to reserve only; restore only at wave end; six-city cap on place.
- Bonus-city earned events recorded once per threshold crossed.
- Living cities never exceed six.

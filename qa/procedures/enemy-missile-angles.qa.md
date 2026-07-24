# QA: Enemy missile angles

**Task:** `enemy-missile-angles`  
**Suite:** enemy-missile-angles  
**Gherkin:** `features/enemy-missile-angles.feature`

Verify enemy ballistic missiles enter from sky origins that are **not locked to the vertical** above their targets: diagonal paths, unintercepted angled impacts still destroy cities/batteries, and multi-enemy wave launches use **varied origin x** along the top of the sky.

## Preconditions

- US-07/US-08 present (enemies, waves, host spawn).
- Documented QA launch (README):
  - `bb play --qa` — telemetry on
  - `--qa-scenario <file.edn>` — optional staged enemies with `:origin`
  - `--qa-events <file>` — optional timed input; `wait` is wall-clock seconds
  - `--qa-speed <n>` — sim-time multiplier for shorter wall waits

## UI Event Boundary

- Automated: documented unit/acceptance commands only.
- Running app: **scenario + events + telemetry only** (no private core API).
- Look-and-feel: **explicit human approval** required (trails should fan, not all drop straight down).

## Scenario staging

**Angled enemy toward city 0 (origin left of target):**
```edn
{:enemies [{:origin [50 0] :target [:city 0]}]}
```

**Angled enemy toward city 5 (origin left of rightmost city):**
```edn
{:enemies [{:origin [100 0] :target [:city 5]}]}
```

**Angled enemy toward left battery:**
```edn
{:enemies [{:origin [200 0] :target [:battery :left]}]}
```

**Wave variety (no staged enemies — host launches schedule):**
```edn
{}
```
or omit `:enemies` and rely on normal wave-1 spawn after `bb play --qa`.

## Telemetry

Under `--qa`, assert at least:

| Field | Use |
|-------|-----|
| `enemy_origin_x=` / `enemy_origin_y=` | Sky entry; y must be top of playfield (`0`) |
| `enemy_x=` / `enemy_y=` | Current position along path |
| `enemy_target=` / target coords | Target city or battery |
| `enemy_missiles=` | In-flight count |

Diagonal flight: after a short wait, both x and y move toward the target (origin x ≠ target x at launch).

## Procedure

### A. Automated

1. Run documented unit tests; assert success.
2. Run documented acceptance including `enemy-missile-angles`; assert success.
3. Run arch-check if documented; assert success.

### B. Explicit angled path (scenario)

4. Launch with angled city scenario (`origin [50 0]`, target city 0).
5. From early telemetry, assert `enemy_origin_y=0` and `enemy_origin_x` equals the staged origin (not the city x).
6. Advance sim (`--qa-speed` + `wait` or real time). Assert position moves toward the city on **both** axes (x between origin and city; y increasing).
7. Allow impact with no defense. Assert city 0 not living; enemy count 0.

### C. Angled battery impact

8. Scenario enemy from offset origin toward left battery; no defense.
9. Assert left battery destroyed; enemy gone.

### D. Wave variety (normal schedule)

10. `bb play --qa` (or empty scenario) so wave 1 schedule launches without forced vertical origins.
11. With **≥3** enemies in flight (or after schedule spawn), assert:
    - every `enemy_origin_y` is `0`
    - more than one distinct `enemy_origin_x`
    - at least one enemy has origin x different from its target’s x (not all vertical)
12. Visually confirm trails are diagonal / fan across the sky, not a set of pure vertical drops.

### E. Look-and-feel

13. Observe a full wave of trails at full native resolution.
14. **Request user approval** for entry angles and trail readability.

## Pass criteria

- Acceptance for `enemy-missile-angles` passes.
- Staged offset origin is honored in telemetry and still destroys the target if unintercepted.
- Multi-enemy wave launch uses varied sky origins (not exclusively straight-down).
- User approves look-and-feel of angled approaches.

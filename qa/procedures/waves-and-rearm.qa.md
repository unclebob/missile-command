# QA: Waves and rearm

**Task:** `waves-and-rearm`  
**Suite:** waves-and-rearm  
**Gherkin:** `features/waves-and-rearm.feature`

Verify waves start at 1, do not complete while enemies remain, complete when the last scheduled enemy is gone, rearm surviving batteries to 10, leave destroyed batteries destroyed, higher waves are harder (count and/or speed), and the **on-screen HUD shows the current wave number** matching core/telemetry.

## Preconditions

- Checkout includes US-08 (depends on US-07).
- Documented QA launch (README):
  - `bb play --qa` — telemetry on
  - `--qa-scenario <file.edn>` — initial state (ammo, cities, batteries destroyed, enemies, wave)
  - `--qa-events <file>` — optional timed input (wall-clock `wait` seconds)
  - `--qa-speed <n>` — sim-time multiplier (e.g. `10`) so host waits stay short

## UI Event Boundary

- Automated: documented unit/acceptance commands only.
- Running app: **scenario + events + telemetry only** (no private core API).

## Scenario staging (wave transition)

Write EDN under `tmp/` (or equivalent) and pass `--qa-scenario`. Examples:

**Depleted ammo + one enemy (rearm after clear):**
```edn
{:batteries {:left {:ammo 2} :center {:ammo 2} :right {:ammo 2}}
 :enemies [{:target [:city 0]}]}
```

**Cities map + enemy:**
```edn
{:cities {:destroyed [4 5]}
 :enemies [{:target [:city 0]}]}
```

**Destroyed left battery + depleted survivors:**
```edn
{:batteries {:left {:destroyed true}
             :center {:ammo 1}
             :right {:ammo 1}}
 :enemies [{:target [:city 1]}]}
```

Telemetry after launch must reflect scenario ammo, city living/dead, and battery destroyed flags before the wave is cleared.

## Telemetry

Under `--qa`, expect at least `wave=`, `wave_complete=`, per-battery ammo/destroyed, city living state, enemy counts (and speed/count metrics for hardness). Full field list: README.

## Procedure

### A. Automated

1. Run unit tests; assert success.
2. Run acceptance including waves-and-rearm; assert success.
3. Run arch-check if documented; assert success.

### B. Running app

4. `bb play --qa --qa-scenario tmp/wave-rearm-depleted.edn` (ammo 2/2/2, ≥1 enemy). Assert `wave=1` and configured ammo in telemetry.
5. Assert the **HUD text includes the current wave** (e.g. `Wave: 1` or equivalent documented label) and that it matches telemetry `wave=1`.
6. While enemies remain, assert wave not complete; HUD still shows wave 1.
7. Clear wave enemies (events and/or let impact). Assert wave completes and advances (e.g. `wave=2`). Host continuous play then launches the next wave’s scheduled enemies.
8. Assert **HUD updates to the new wave number** (matches telemetry).
9. Assert each non-destroyed battery has **10** missiles (rearm from depleted scenario) on a telemetry line at/after the advance.
10. Relaunch with destroyed-left scenario; complete a wave; assert left stays destroyed and cannot fire; others rearm to 10.
11. Compare wave 1 vs higher wave hardness (count and/or speed via telemetry).
12. Quit cleanly (`quit` in events or UI).

## Pass criteria

- Unit/acceptance pass; scenario EDN stages ammo/cities/enemies; rearm and destroyed rules hold; higher waves harder.
- **HUD shows current wave** and stays in sync with core after advance.

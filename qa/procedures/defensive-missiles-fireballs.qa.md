# QA: Defensive missiles and fireballs

**Task:** `defensive-missiles-fireballs`  
**Suite:** defensive-missiles-fireballs  
**Gherkin:** `features/defensive-missiles-fireballs.feature`

Verify defensive missiles fly to the aim point, become fireballs that expand then contract and expire, destroy in-blast test targets, leave out-of-blast targets alone, and clamp large `dt` spikes. QA must observe **fireball phase timing** (start, max radius, shrinking) via telemetry. Automated unit/acceptance remain; end-to-end QA drives the **running app** with real input, QA telemetry, and explicit user look-and-feel approval.

## Preconditions

- Checkout includes US-06 implementation (depends on fire/aim).
- Commands are run from the project root.
- Documented launch command and QA switches (see README):
  - `--qa-telemetry` — fire events, missiles in flight, flight vectors, fireball state (count, center, radius), and **fireball phase timing** (start, max, shrink)
  - `--qa-target x,y` (or equivalent) to place a destroyable test target

## UI Event Boundary

### Automated test portion

- Run documented unit and acceptance commands from the project root; use exit status and output only.
- Do not call core namespaces as a substitute for documented commands.

### Running-app portion

- Launch with `--qa-telemetry` (and any documented target-spawn switch).
- Deliver **real** aim, fire (key and/or click), and time progression as the app normally runs (wall clock while the app runs, or a documented QA “advance time” UI affordance if provided for deterministic checks).
- Read **telemetry** for missiles, fireballs, phase times, and targets — not private APIs.
- **Request explicit human approval** for look and feel before pass.

## Telemetry contract (extends README)

With `--qa-telemetry`, after relevant simulation updates the app must report at least:

| Field | Meaning |
|-------|---------|
| `missiles_in_flight=` | Defensive missiles still flying |
| Per missile | `origin_*` / `target_*` (or equivalent vector) |
| `fireballs=` | Count of active fireballs |
| **Each fireball** | **Required:** `center_x`, `center_y`, `radius` on every live-fireball line; plus **phase timing** below |
| Destroyable targets (if present) | identity/position and `destroyed=true\|false` |

### Fireball center and radius (required)

For **each** active fireball, every telemetry sample (start, max, shrink, and any tick snapshot) must include:

- `center_x` / `center_y` — blast center in playfield coordinates
- `radius` — current radius

QA must parse these fields and use them to verify detonation location and expand/shrink. If multiple fireballs exist, each has its own center and radius in the stream.

### Fireball phase timing (required)

Telemetry must expose times (seconds of simulation time, or documented epoch) so QA can distinguish the three phases. Phase lines for a live fireball still carry **center and radius**:

| Event / field | Meaning |
|---------------|---------|
| **Fireball start** | Missile arrived; expand begins. e.g. `phase=start t=... center_x=... center_y=... radius=...` |
| **Fireball max** | Peak radius. e.g. `phase=max t=... center_x=... center_y=... radius=...` |
| **Fireball shrinking** | After max. e.g. `phase=shrink t=... center_x=... center_y=... radius=...` and `phase=end t=...` when gone |

Times must be **monotonic** for a single fireball: `start_t` ≤ `max_t` ≤ shrink samples ≤ `end_t`.

Exact field names are defined in the README; QA parses that documented form.

## Procedure

### A. Automated acceptance/unit

1. Open README for test commands, launch, and QA switches.
2. Run documented unit tests; assert success.
3. Run documented acceptance; assert success including defensive-missiles-fireballs scenarios.
4. Run architecture check if documented; assert success.

### B. Running app: flight, detonation, fireball phase times

5. Start the app with `--qa-telemetry`. Capture the full telemetry stream with timestamps as printed.
6. Aim at a known sky point `(ax, ay)` and fire (key or click). Assert a missile is in flight with target `(ax, ay)`.
7. Allow simulation time to pass until telemetry reports **fireball start** at center `(ax, ay)`:
   - `missiles_in_flight=0` (for that missile)
   - fireball present with `phase=start` (or equivalent)
   - assert line includes `center_x`/`center_y` matching `(ax, ay)` (tolerance as documented) and a numeric `radius`
   - record `t_start` and `radius_start` from telemetry
8. Continue until telemetry reports **fireball max**:
   - `phase=max` with `center_x`/`center_y`/`radius` present
   - record `t_max` and `radius_max`
   - assert `t_max` ≥ `t_start`
   - assert `radius_max` > `radius_start`
   - assert center still matches `(ax, ay)`
9. Continue into **fireball shrinking**:
   - assert shrink-phase samples include `center_x`, `center_y`, `radius` with `t` ≥ `t_max`
   - assert at least one shrink sample has **radius less than** `radius_max`
   - assert center still matches `(ax, ay)`
10. Continue until fireball **end** / expiry (`fireballs=0` or `phase=end`); record `t_end` if present; assert `t_end` ≥ `t_max` (and ≥ last shrink sample).
11. Quit cleanly.

### C. Running app: destroyable targets

12. Start the app with `--qa-telemetry` and `--qa-target` on the planned detonation point.
13. Fire at that point; wait until a fireball exists with radius covering the target (at or after start, typically by max).
14. Assert telemetry reports the target **destroyed**.
15. Quit; restart with a target **far outside** the planned blast.
16. Fire and wait through **fireball max** (use telemetry max time); assert target **not** destroyed.
17. Quit cleanly.

### D. Manual look-and-feel

18. Observe missile trails, explosion growth at start→max, shrink, and clarity of the blast.
19. **Request user approval** for look and feel; fail the suite if rejected until fixed and re-approved.

## Pass criteria

- Unit and acceptance pass, including defensive-missiles-fireballs Gherkin.
- Telemetry reports **each fireball’s center (`center_x`, `center_y`) and `radius`** on every live-fireball line.
- Telemetry reports **fireball start**, **fireball max**, and **shrinking** with times; QA verifies `t_start` ≤ `t_max` ≤ shrink/end and radius grows then shrinks at a stable center.
- Destroyable hit/miss behaves correctly relative to the blast.
- **User has explicitly approved look and feel.**

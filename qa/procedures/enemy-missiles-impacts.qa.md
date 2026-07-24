# QA: Enemy missiles impacts

**Task:** `enemy-missiles-impacts`  
**Suite:** enemy-missiles-impacts  
**Gherkin:** `features/enemy-missiles-impacts.feature`

Verify enemy ballistic missiles travel toward cities/batteries, destroy targets on unintercepted impact, are **destroyed when they pass within a fireball’s radius**, leave targets intact when so intercepted, ignore fireballs they never enter, and that destroyed batteries cannot fire. Automated unit/acceptance remain; end-to-end QA drives the running app with documented QA switches, real input where needed, telemetry, and explicit user look-and-feel approval.

## Preconditions

- Checkout includes US-07 (depends on US-06 fireballs).
- Documented launch (`bb play`) and switches (README):
  - `--qa-telemetry` — enemy missiles (count, path/position, target), cities/batteries alive state, fireballs with **center and radius**, defensive fire as before
  - Documented way to **spawn** an enemy missile toward a city or battery (`--qa-enemy` and/or `--qa-events`)
  - Documented way to create or wait for a fireball at a known center/radius (defensive fire to a known aim, or documented QA fireball setup)

## UI Event Boundary

- Automated: documented `bb test` / `bb accept` only.
- Running app: launch with QA switches; spawn enemies and produce fireballs via documented CLI/events/UI; read **telemetry** only (no private core API).
- Look-and-feel: **request explicit human approval** before pass.

## Telemetry contract (extends README)

With `--qa-telemetry`, report at least:

| Field | Meaning |
|-------|---------|
| `enemy_missiles=` | Count in flight |
| Per enemy | origin, current position, target (city index or battery id) |
| Per fireball | `center_x`, `center_y`, `radius` (required while live) |
| Cities / batteries | living vs destroyed |
| On **radius intercept** | enemy removed while distance to a fireball center ≤ that fireball’s radius; target still living |
| On impact | enemy gone; target destroyed |

QA uses enemy position and fireball center/radius from telemetry to confirm **within-radius** destruction (not merely “fireball exists somewhere”).

## Procedure

### A. Automated

1. Run documented unit tests; assert success.
2. Run documented acceptance including enemy-missiles-impacts; assert success (including inside-radius destroy and outside-radius non-destroy scenarios).
3. Run arch-check if documented; assert success.

### B. Running app: impact

4. Start `bb play --qa-telemetry` with documented enemy spawn toward a living city; capture telemetry.
5. Allow time until impact (no defensive fire). Assert telemetry: city not living; enemy count 0.
6. Quit; restart; spawn enemy toward **left** battery; wait for impact. Assert left battery destroyed; key-fire left → no successful fire (`battery=none` / no new defensive missile).

### C. Running app: fireball radius destroys enemy

7. Quit; restart with telemetry. Spawn an enemy toward a living city.
8. Create a fireball whose disk the enemy path will enter (aim/fire defensive missile to a point on the path, or documented QA setup). Record fireball `center_x`, `center_y`, `radius` from telemetry.
9. Advance until telemetry shows the enemy is gone **without** the target city dying.
10. From the last samples before destruction (or documented intercept event), assert the enemy’s position was within **radius** of the fireball center (distance ≤ `radius`, using documented coordinate units/tolerance).
11. Assert city still living and `enemy_missiles=0` (for that enemy).

### D. Running app: outside radius does not save the city

12. Restart with telemetry. Spawn an enemy toward a city and a fireball (or detonation) **far** from the enemy path so the path stays outside the fireball radius.
13. Wait until impact or resolution. Assert the city is destroyed (enemy was not removed by that fireball) and telemetry never showed a within-radius intercept for that enemy/fireball pair.

### E. Manual look-and-feel

14. Observe enemy trails, near-miss vs intercept when the warhead clips the blast disk, impacts vs saves.
15. **Request user approval** for look and feel; fail until approved if rejected.

## Pass criteria

- Unit and acceptance pass for enemy-missiles-impacts.
- Enemy missiles that **pass within a fireball’s radius** are destroyed; their targets survive.
- Enemy missiles that stay **outside** all fireball radii are not destroyed by those fireballs (and destroy their target if unintercepted).
- Destroyed batteries cannot fire.
- **User has explicitly approved look and feel.**

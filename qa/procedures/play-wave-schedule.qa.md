# QA: Play wave schedule

**Task:** `play-wave-schedule`  
**Suite:** waves-and-rearm + advanced enemies (MIRV / smart / flyers)  
**Gherkin:** `features/waves-and-rearm.feature` (plus schedule unit/property)

Verify that when a wave **starts** (new game start and after wave-banner), the host activates the **full schedule** for that wave: ballistics, MIRVs, smart bombs, bombers, and satellites per `waves` metrics / difficulty.

## Schedule rules (arcade)

| Kind | When |
|------|------|
| Ballistic | Always; count = `wave_enemy_count` |
| MIRV parents | `mirv-count = max(0, floor(wave/2) - 1)` |
| Smart bombs | From wave 3; `floor((wave-2)/2)` |
| Bomber | From wave 4 (1) |
| Satellite | From wave 5 (1) |

Difficulty scales ballistic count/speed only; advanced counts follow arcade tables.

## Preconditions

- US-08 waves/rearm; US-11–13 enemy types implemented.
- Host `ensure-wave-enemies` / `activate-wave-schedule` on start and after banner.

## UI Event Boundary

- Telemetry: `wave_enemy_count`, `wave_mirv_count`, `wave_smart_bomb_count`, `wave_bomber_count`, `wave_satellite_count`, live `mirv_parents`, `smart_bombs`, `flyers_bomber`, `flyers_satellite`.
- Scenario: `:wave N` then `start` (or playing) to spawn schedule.

## Procedure

### A. Automated — unit + acceptance (waves) + arch-check.

### B. Wave 1 — start → only ballistics; counts match metrics; no MIRV/bomber/satellite.

### C. Wave 9 — scenario wave 9 + start → ballistics + MIRVs + smarts + bomber + satellite match metrics.

### D. Continuous — clear wave 1 → banner → wave 2 schedule spawns (ballistics; MIRV still 0).

## Pass criteria

- Unit/accept green; host B–D match metrics.

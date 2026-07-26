# QA: Random sky origins

**Task:** `random-sky-origins`  
**Suite:** waves-and-rearm / enemy-missile-angles  
**Gherkin:** `features/waves-and-rearm.feature`, `features/enemy-missile-angles.feature` (varied sky origins)

Verify wave **salvo** enemies enter from **random sky x** positions (not fixed equal spacing). Each origin has **y = 0** (top of sky) and moves toward its city/battery target (angled trails).

## Rules

| Item | Detail |
|------|--------|
| Origin y | Always 0 (top) |
| Origin x | Random in `[0, playfield-width)` per enemy |
| Salvo size | 3 ballistics per attack |
| Visual | Trails from different tops toward targets |

## Procedure

### A. Automated — unit + accept (waves / angles) + arch-check.

### B. Host start / attack 1 — three enemies; `enemy_origin_y=0`; origin x in range.

### C. Variety — distinct origin x values among the salvo (not all identical).

## Pass criteria

- Accept green; host B–C hold.

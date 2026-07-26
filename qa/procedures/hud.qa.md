# QA: HUD

**Task:** `hud`  
**Suite:** `hud`  
**Gherkin:** `features/hud.feature`

Verify the in-game **HUD** during **playing** and **paused** exposes and matches core: **score**, **wave**, **multiplier**, **L/C/R ammo**, **living cities**, **bonus cities in reserve**. Title need not show the full playing HUD. Host draws values legibly (modern vector).

Depends on US-09/US-10 (score, mult, bonus cities) and US-15/US-16 (playing/paused screens).

## HUD fields (single source of truth)

| Field | Source |
|-------|--------|
| Score | core score |
| Wave | core wave |
| Multiplier | core multiplier |
| Left / center / right ammo | each battery missiles |
| Living cities | count of living cities |
| Bonus cities | reserve count |

## Preconditions

- Start from title into playing.
- `bb play --qa` with telemetry that mirrors HUD fields.

## UI Event Boundary

- Scenario + events + telemetry / visible HUD only.
- Telemetry should include the same fields the HUD shows (`score=`, `wave=`, `multiplier=`, `battery_*_ammo=`, living/bonus cities).

## Procedure

### A. Automated — unit + acceptance (`hud`) + arch-check.

### B. Fresh play

1. Start game. Assert HUD/telemetry: score 0, wave 1, mult 1, ammo 10/10/10, living 6, bonus 0.

### C. Score / wave / mult

2. Stage score and wave; assert HUD matches core.

### D. Ammo

3. Fire left/center/right once each (or separately); HUD ammo decrements for that battery only.

### E. Cities / reserve

4. Destroy a city; set bonus reserve; HUD living and bonus match.

### F. Pause

5. Pause; assert HUD still shows current score/wave/mult/cities/bonus (overlay may sit on top).

### G. Title

6. On title, full playing HUD not required.

## Pass criteria

- Acceptance for `hud` passes.
- HUD values always match core during playing and paused.

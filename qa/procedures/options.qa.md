# QA: Options

**Task:** `options`  
**Suite:** options  
**Gherkin:** `features/options.feature`

Verify **options** from title: **mute**, **key remaps** (fire/pause), **difficulty** (`easy` / `normal` / `arcade`); defaults unmuted + arcade + Z/X/C (and 1/2/3 duals) + P/Esc; difficulty scales wave count/speed (arcade 1.0, normal **0.85**, easy **0.7**); settings stored and re-read; host **persists** across restart.

Depends on US-15 title/start. Out of scope: full gamepad schemes, graphics quality; SFX playback is US-20 (mute flag only here).

## Defaults

| Option | Default |
|--------|---------|
| Mute | false |
| Difficulty | arcade |
| Fire left | z, 1 |
| Fire center | x, 2 |
| Fire right | c, 3 |
| Pause | p, escape |

## Difficulty scaling (on schedule metrics)

`scaled = arcade_value * factor`, count floored at least 1 when arcade count ≥ 1 (or as implemented and locked by examples).

| Preset | Factor |
|--------|--------|
| arcade | 1.0 |
| normal | 0.85 |
| easy | 0.7 |

Examples wave 1 arcade: count 3, speed 50 → easy count 2, speed 35.

## Preconditions

- Title → options → leave → start documented.
- Persistence path same family as high scores (README).

## UI Event Boundary

- Scenario/events for open options, set mute/difficulty/bind key, leave, start, press remapped key.
- Manual host UI shows mute as a clickable checkbox, not as an `M` keyboard prompt.
- Telemetry: mute, difficulty, key bindings, wave metrics after start.

## Procedure

### A. Automated — unit + acceptance (`options`) + arch-check.

### B. Open / defaults — title → options; mute false, arcade, default keys.

### C. Mute — click the mute checkbox; leave; reopen; still true.

### D. Remap — bind left to `q`; start; press `q`; left battery fires.

### E. Difficulty — set easy/normal/arcade; start; wave metrics match table.

### F. Persist — change options; quit host; relaunch; values restored (host).

### G. Leave — leave options → title.

## Pass criteria

- Acceptance for `options` passes.
- Mute, keys, difficulty behave per Gherkin; host persistence verified.

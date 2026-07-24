# QA: Sound events

**Task:** `sound-events`  
**Suite:** sound-events  
**Gherkin:** `features/sound-events.feature`

Verify core emits SFX events for combat and shell moments, and the host plays synthesized sounds when **not muted**. Mute suppresses **playback only**; core still emits events for tests.

Depends on US-06 combat and US-19 mute. Out of scope: licensed samples, background music.

## Event catalog

| Event | When |
|-------|------|
| `sfx/launch` | Defensive missile fired |
| `sfx/explosion` | Fireball destroys an enemy (or major blast) |
| `sfx/city-destroyed` | City hit |
| `sfx/battery-destroyed` | Battery hit |
| `sfx/low-ammo` | Fire leaves battery at low threshold (default **2** → after fire **1**) |
| `sfx/wave-clear` | Wave completed |
| `sfx/bonus-city` | Bonus city threshold crossed |
| `sfx/the-end` | Enter THE END |
| `sfx/ui` | Optional menu navigate/confirm |

## Preconditions

- Mute option available.
- Host has audible SFX path (synthesized).
- `bb play --qa` with telemetry or event log of SFX types.

## UI Event Boundary

- Core: event list / last events on state after commands and ticks.
- Host: play when unmuted; silent when muted (QA: process flag, log “played”, or no audio device call).
- Do not use private core APIs for play checks beyond documented telemetry.
- Look-and-feel: optional; **request approval** that SFX feel arcade-appropriate when unmuted.

## Procedure

### A. Automated — unit + acceptance (`sound-events`) + arch-check.

### B. Launch — fire battery; assert `sfx/launch` in event log; hear blip if unmuted.

### C. Explosion — intercept enemy; assert `sfx/explosion`.

### D. Impacts — city hit → `sfx/city-destroyed`; battery hit → `sfx/battery-destroyed`.

### E. Low ammo — set ammo 2; fire; assert `sfx/low-ammo`.

### F. Wave clear — complete wave; assert `sfx/wave-clear`.

### G. Bonus city — score ≥ 10000; assert `sfx/bonus-city`.

### H. THE END — all cities gone, reserve 0; assert `sfx/the-end`.

### I. Mute

1. Mute on; fire; core still has `sfx/launch`.
2. Host does **not** play (no audible / play counter 0).
3. Mute off; fire; host plays again.

### J. Look-and-feel — **request user approval** for SFX character (optional).

## Pass criteria

- Acceptance for `sound-events` passes.
- All required events emit at the right moments.
- Mute blocks host playback only.
- User approves SFX if reviewed.

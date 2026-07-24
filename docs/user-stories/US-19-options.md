# US-19 — Options (mute, keys, difficulty)

**Status:** in-specifier  
**Depends on:** US-15  
**Design:** §5.3, §6.5, §8

## Story

**As a** player,  
**I want** options for mute, key remapping, and difficulty,  
**So that** I can tailor control and challenge.

## In scope

- Options reachable from title (and optionally pause).
- Mute boolean (affects whether SFX play when US-20 exists; until then, mute is stored state).
- Remap fire/pause keys; defaults remain Z/X/C and 1/2/3 dual bindings until changed.
- Difficulty presets: easy, normal, arcade — adjust wave pacing/counts/speeds only, not different rules.
- Persist options with the same storage approach as high scores.

## Acceptance criteria

- Changing difficulty affects documented wave parameters on the next new game (or immediately if specified).
- Remapped keys fire the corresponding batteries.
- Mute is stored and readable by the audio host.
- Options survive restart on a given host.

## Out of scope

- Full control schemes beyond keyboard + mouse; graphics quality settings.

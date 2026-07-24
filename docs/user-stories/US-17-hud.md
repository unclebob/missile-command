# US-17 — In-game HUD

**Status:** backlog  
**Depends on:** US-09, US-10  
**Design:** §6.2, §7

## Story

**As a** player,  
**I want** an on-screen HUD showing score, wave, multiplier, ammo, and cities,  
**So that** I can make tactical decisions without guessing.

## In scope

- During playing and paused, HUD exposes: score, wave, multiplier, L/C/R ammo, living cities, bonus cities in reserve.
- Values match core state (single source of truth).
- Readable modern-vector presentation when drawn by a host.

## Acceptance criteria

- After scoring events, HUD-reflected score matches core score.
- Ammo counts match batteries after firing and rearm.
- Wave and multiplier match core.
- City and bonus-city counts match core.
- HUD is not required on pure title before first play, but must appear in playing/paused.

## Out of scope

- Full visual polish pass beyond legibility; options screens.

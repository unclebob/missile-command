# US-16 — Pause and resume

**Status:** backlog  
**Depends on:** US-15  
**Design:** §5.1, §5.3, §6.3

## Story

**As a** player,  
**I want** to pause and resume the game,  
**So that** I can step away without losing the run.

## In scope

- Toggle pause from playing (default P and Esc).
- While paused: no simulation advance; fire commands ignored.
- Resume returns to playing; enemies and missiles continue from prior state.
- Pause overlay is user-visible when a host is present; core exposes paused screen state.

## Acceptance criteria

- Pause freezes enemy and missile motion across ticks.
- Fire while paused does not spend ammo or create missiles.
- Resume allows motion and firing again.
- Pause is only meaningful during playing (not required on title).

## Out of scope

- Options from pause menu (may link later); full host polish.

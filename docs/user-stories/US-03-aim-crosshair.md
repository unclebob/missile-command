# US-03 — Aim the crosshair

**Status:** backlog  
**Depends on:** US-02  
**Design:** §5.2, §5.3

## Story

**As a** player,  
**I want** to move a crosshair with the pointer,  
**So that** I can choose where defensive missiles will explode.

## In scope

- Aim command updates crosshair position.
- Position is clamped to the current playfield.

## Acceptance criteria

- After aiming to a point inside the playfield, the crosshair is at that point.
- Aiming outside the playfield clamps to the nearest in-bounds position.
- Aiming does not change cities, batteries, ammo, or score.

## Out of scope

- Firing, rendering the crosshair in Quil (host stories), enemies.

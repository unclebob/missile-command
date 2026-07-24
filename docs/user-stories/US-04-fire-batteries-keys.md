# US-04 — Fire batteries with keys

**Status:** backlog  
**Depends on:** US-03  
**Design:** §5.3, §5.4

## Story

**As a** player,  
**I want** to fire the left, center, or right battery with dedicated keys,  
**So that** I choose which launcher spends a missile toward my aim point.

## In scope

- Fire-left, fire-center, fire-right commands (default keys defined in design; remapping is US-19).
- Each non-destroyed battery with ammo > 0 launches one defensive missile toward the current crosshair and decrements ammo.
- Cannot fire from empty or destroyed batteries.
- Default dual key bindings (letter and number) are part of options defaults when input mapping exists; core accepts battery identity.

## Acceptance criteria

- Firing a stocked battery reduces that battery’s ammo by one and creates an in-flight defensive missile aimed at the crosshair.
- Firing does not reduce other batteries’ ammo.
- Firing an empty battery has no effect (no missile, ammo stays 0).
- Firing a destroyed battery has no effect.
- Center battery missiles are configured to travel faster than left/right (speed observable in flight or via documented property).

## Out of scope

- Click-to-fire zones (US-05), fireball detonation (US-06), key remapping UI (US-19).

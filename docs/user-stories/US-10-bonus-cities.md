# US-10 — Bonus cities from score

**Status:** in-specifier  
**Depends on:** US-09  
**Design:** §5.4, §5.7

## Story

**As a** player,  
**I want** extra cities awarded at score thresholds and held in reserve,  
**So that** I can recover from losses and keep defending longer.

## In scope

- Every N points (default 10,000) awards a bonus city to reserve.
- Reserve cities restore destroyed cities when rules apply (after wave resolution / when earned — per design).
- Never more than six living cities; extras stay in reserve.
- Emit a bonus-city event for later SFX (event may be silent until US-20).

## Acceptance criteria

- Crossing a threshold increases bonus-city reserve by one (per threshold).
- A destroyed city can be restored from reserve when restoration rules fire.
- Living city count never exceeds six.
- Threshold is parameterized (default 10000).

## Out of scope

- THE END (US-14), HUD (US-17).

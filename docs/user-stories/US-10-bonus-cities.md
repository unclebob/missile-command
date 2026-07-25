# US-10 — Bonus cities from score

**Status:** done  
**Depends on:** US-09  
**Design:** §5.4, §5.7

## Story

**As a** player,  
**I want** extra cities awarded at score thresholds and held in reserve,  
**So that** I can recover from losses and keep defending longer.

## In scope

- Every N points (default 10,000) awards a bonus city to reserve.
- Reserve cities restore destroyed cities **only at wave end** (never mid-wave).
- Never more than six living cities; extras stay in reserve.
- Emit a bonus-city SFX event when a city is placed from reserve.
- Wave banner may show Bonus City when a city was restored this wave.

## Acceptance criteria

- Crossing a threshold increases bonus-city reserve by one (per threshold).
- Award is mid-wave (score sync); place from reserve runs only when the wave completes.
- Living city count never exceeds six.
- Threshold is parameterized (default 10000).
- THE END when living cities = 0 and reserve = 0 (no mid-wave place to save the run).

## Out of scope

- THE END presentation (US-14), HUD layout (US-17).

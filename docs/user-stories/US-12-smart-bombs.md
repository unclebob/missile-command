# US-12 — Smart bombs

**Status:** in-specifier  
**Depends on:** US-06, US-08  
**Design:** §5.5, §5.6, §5.7

## Story

**As a** player,  
**I want** smart bombs that evade poorly aimed blasts,  
**So that** I must use precise, preferably fast center-battery intercepts.

## In scope

- Smart bombs move toward cities/batteries.
- Evasion when a fireball would destroy them under “near miss / edge of blast” conditions (constants tunable; behavior deterministic for tests).
- Point value 125 × multiplier when destroyed.
- Center battery’s higher speed remains advantageous at range.

## Acceptance criteria

- A well-centered fireball destroys a smart bomb.
- A poorly aimed fireball allows documented evasion instead of destruction (examples lock the constants).
- Destroyed smart bombs award the correct score.
- Smart bombs that reach a target destroy it like other warheads.

## Out of scope

- Flyer enemies (US-13).

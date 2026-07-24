# US-09 — Score and multiplier

**Status:** backlog  
**Depends on:** US-08  
**Design:** §5.7

## Story

**As a** player,  
**I want** points for destroying enemies and for leftover missiles and cities at wave end,  
**So that** I can compete for a high score.

## In scope

- Point values for destroying missiles (and placeholders for later enemy types as they appear).
- End-of-wave bonuses for unused defensive missiles and surviving cities.
- Multiplier starts at 1×, increases by 1 every two waves, max 6×; applies to destruction and bonuses.
- Score never decreases.

## Acceptance criteria

- Destroying an enemy missile adds 25 × current multiplier (or documented value).
- Wave-end bonuses match design table × multiplier.
- Multiplier schedule matches design examples (parameterized).
- Score only increases or stays the same.

## Out of scope

- Bonus city awards (US-10), high-score persistence (US-18), HUD display (US-17).

## Notes for specifier

- Put all numeric score values in Gherkin parameters for acceptance mutation.

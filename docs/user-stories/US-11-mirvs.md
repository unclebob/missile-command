# US-11 — MIRV warheads

**Status:** backlog  
**Depends on:** US-08  
**Design:** §5.5

## Story

**As a** player,  
**I want** some enemy missiles to split into multiple warheads mid-descent,  
**So that** later waves force harder interception choices.

## In scope

- MIRV-capable enemy missiles that split into multiple independently targeted warheads at a documented progress point.
- Children inherit threat behavior (can destroy cities/batteries; can be destroyed by fireballs).
- Waves introduce MIRVs in mid/later bands per design intent.

## Acceptance criteria

- Before split, one MIRV parent is a single target.
- After split, multiple warheads exist and proceed toward targets.
- Destroying the parent before split prevents children from appearing.
- Children can destroy cities if unintercepted; fireballs can destroy children.

## Out of scope

- Smart bombs (US-12), flyers (US-13).

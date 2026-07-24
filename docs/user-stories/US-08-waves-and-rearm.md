# US-08 — Waves complete and batteries rearm

**Status:** backlog  
**Depends on:** US-07  
**Design:** §5.4, §5.5

## Story

**As a** player,  
**I want** attacks to come in waves that end when threats are gone,  
**So that** I get a breather, rearm surviving batteries, and face a harder next wave.

## In scope

- Wave number starts at 1 (or documented initial) and advances.
- A wave ends when all enemies scheduled for that wave are destroyed or have impacted.
- At wave start (including after a completed wave), each non-destroyed battery refills to 10 missiles.
- Destroyed batteries remain destroyed for the run.
- Difficulty increases with wave (speed and/or count); exact tables from design, parameterized for mutation.

## Acceptance criteria

- While enemies remain, the wave does not complete.
- When the last enemy of the wave is gone, the wave completes and the next wave can begin.
- Surviving batteries show 10 missiles after rearm; destroyed batteries stay destroyed and empty/unusable.
- Higher waves are observably harder by documented metrics (count and/or speed).

## Out of scope

- End-of-wave point bonuses (US-09), bonus cities (US-10), special enemy types (US-11–13).

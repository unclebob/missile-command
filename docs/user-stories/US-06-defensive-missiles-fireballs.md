# US-06 — Defensive missiles and fireballs

**Status:** done  
**Depends on:** US-04  
**Design:** §5.2, §5.4

## Story

**As a** player,  
**I want** my missiles to fly to the aim point and explode into fireballs,  
**So that** I can destroy enemy warheads that enter the blast.

## In scope

- Defensive missiles advance over time toward their aim point.
- On arrival, they become fireballs that expand then contract over a lifetime.
- Entities marked as destroyable by fireballs are removed when inside the fireball radius (use simple test doubles / enemy stubs if full enemies are not yet complete; integrate with US-07 enemies).
- Tick uses clamped real time (`dt`).

## Acceptance criteria

- After enough time, a fired missile reaches its target and is replaced by a fireball at that point.
- Fireball radius changes over its lifetime (expand then contract) and eventually disappears.
- An enemy-like target inside the fireball is destroyed; one outside is not.
- Large `dt` spikes do not skip critical collision unfairly beyond the documented clamp behavior.

## Out of scope

- Full enemy wave AI (US-07+), smart-bomb evasion (US-12), sound (US-20).

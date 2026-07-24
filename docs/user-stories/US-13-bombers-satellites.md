# US-13 — Bombers and satellites

**Status:** backlog  
**Depends on:** US-08  
**Design:** §5.5, §5.7

## Story

**As a** player,  
**I want** bombers and satellites to cross the sky and drop missiles,  
**So that** threats can appear from moving platforms as well as ballistic arcs.

## In scope

- Flyer entities that traverse the sky on a path.
- Periodic or scripted spawning of enemy missiles from flyers.
- Destroyable by fireballs.
- Point value 100 × multiplier for bomber and for satellite when destroyed.

## Acceptance criteria

- A flyer moves across the playfield over time.
- A flyer can spawn one or more enemy missiles during its pass.
- Destroying a flyer with a fireball awards correct points and stops further spawns from that flyer.
- Spawned missiles behave as normal enemy missiles toward cities/batteries.

## Out of scope

- New player weapons; only fireballs destroy flyers.

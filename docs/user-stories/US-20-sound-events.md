# US-20 — Sound effects from game events

**Status:** backlog  
**Depends on:** US-06, US-19  
**Design:** §8

## Story

**As a** player,  
**I want** arcade-style sound effects for combat and shell events,  
**So that** the game feels responsive and tense.

## In scope

- Core emits SFX events: launch, explosion, city destroyed, battery destroyed, low ammo, wave clear, bonus city, THE END, optional UI.
- Host plays synthesized sounds for those events when not muted.
- Mute suppresses playback without removing events from the log if tests observe events.

## Acceptance criteria

- Firing emits a launch event.
- Destroying an enemy with a fireball emits an explosion event.
- City/battery destruction emit their events.
- Wave clear and bonus city emit their events when those rules fire.
- When muted, the host does not produce audible output (QA/host check); core may still emit events.

## Out of scope

- Licensed samples; background music soundtrack.

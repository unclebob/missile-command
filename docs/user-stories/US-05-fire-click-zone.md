# US-05 — Fire by click zone

**Status:** backlog  
**Depends on:** US-04  
**Design:** §4.2, §5.3

## Story

**As a** player,  
**I want** a click in the sky to fire the battery for that horizontal third of the screen,  
**So that** I can play without using the battery keys.

## In scope

- Fire-at (x, y) maps x to left / center / right by equal horizontal thirds of current width.
- Fires toward the click position (crosshair should match).
- Same ammo and destroyed-battery rules as key fire.
- Key fire and click fire both remain available (not modes).

## Acceptance criteria

- Click in the left third fires the left battery (when able).
- Click in the center third fires the center battery (when able).
- Click in the right third fires the right battery (when able).
- Zone boundaries use the current width (recompute after resize).
- Examples cover edges of thirds unambiguously.

## Out of scope

- UI chrome clicks for menus (later screen stories).

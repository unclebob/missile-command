# US-05 — Fire by click zone

**Status:** done  
**Depends on:** US-04  
**Design:** §4.2, §5.3

## Story

**As a** player,  
**I want** a click in the sky to fire the battery for that horizontal third of the screen,  
**So that** I can play without using the battery keys.

## In scope

- Fire-at (x, y) maps x to left / center / right by equal horizontal thirds of current width.
- Fires toward the click position (crosshair should match).
- If the preferred zone battery cannot fire (empty or destroyed), an adjacent battery that can fire is used instead, in a fixed order.
- Key fire still has no adjacent fallback (empty/destroyed key fire remains a no-op).
- Key fire and click fire both remain available (not modes).

## Acceptance criteria

- Click in the left third prefers the left battery when able.
- Click in the center third prefers the center battery when able.
- Click in the right third prefers the right battery when able.
- If the preferred battery cannot fire, fallback order is:
  - left zone: left → center → right
  - center zone: center → left → right
  - right zone: right → center → left
- If no battery can fire, the click still updates the crosshair but launches nothing.
- Zone boundaries use the current width (recompute after resize).
- Examples cover edges of thirds unambiguously.

## Out of scope

- UI chrome clicks for menus (later screen stories).

# US-02 — New game at full resolution

**Status:** in-implementation  
**Depends on:** US-01  
**Design:** §3.4, §4, §5.2

## Story

**As a** player,  
**I want** a new game that places six cities and three batteries on the full screen,  
**So that** the battlefield matches my window size and is ready for play.

## In scope

- Starting a new game with explicit width and height.
- Six cities and three batteries (left, center, right) laid out from those dimensions.
- Ground band and horizontal placement consistent with design layout intent.
- Resizing the playfield reflows layout while preserving that a new game can be created at any size.
- Center battery is distinct (will fire faster in later stories; may only be marked/configured here).

## Acceptance criteria

- A new game at a given width/height has exactly six living cities and three non-destroyed batteries with full ammo (10 each).
- City and battery positions change appropriately when width/height change (not a scaled copy of a fixed 800×600 buffer).
- Layout remains sensible at more than one aspect ratio (e.g. wide and square-ish), covered by examples.
- Progress-preserving resize behavior is defined at least for a fresh game (full preserve rules apply as more state appears in later stories).

## Out of scope

- Aiming, firing, enemies, scoring, screens other than “game exists.”

## Notes for specifier

- Use Gherkin parameters for width, height, and counts.
- Prefer observable state queries over rendering assertions for this story.

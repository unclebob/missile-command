# US-15 — Title screen and start

**Status:** in-specifier  
**Depends on:** US-02  
**Design:** §5.1, §6.1

## Story

**As a** player,  
**I want** a title screen that lets me start a new game,  
**So that** I have a clear entry point into play.

## In scope

- Initial screen is title (or returns to title after a completed flow).
- Start action transitions to playing with a fresh game at current dimensions.
- Title shows game name and a start affordance (key and/or click).
- Optional light attract animation is nice-to-have, not required for acceptance.

## Acceptance criteria

- On launch (or new session), the screen is title.
- Starting from title enters playing with six cities and three batteries ready.
- Starting resets score/wave appropriate to a new run.

## Out of scope

- Options menu contents (US-19), high-score table view (US-18), pause (US-16).

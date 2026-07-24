# US-14 — THE END when cities are gone

**Status:** done  
**Depends on:** US-10  
**Design:** §5.1, §5.4, §5.8

## Story

**As a** player,  
**I want** the run to end with “THE END” when I have no cities left and none in reserve,  
**So that** the game communicates that nuclear war has no winner.

## In scope

- Detect game-over condition: zero living cities and no bonus city available to place (and none earned that would save the player under design rules).
- Transition to a THE END screen/state (not the words “Game Over”).
- Final score remains available for high-score flow (US-18).

## Acceptance criteria

- With at least one living city, the game does not end.
- With zero living cities and zero usable reserve, the game enters THE END.
- THE END presentation uses the phrase “THE END” (observable in state and/or UI string).
- Player cannot continue firing as if the run were still active after THE END (commands ignored or screen leaves playing).

## Out of scope

- High-score initials entry UI (US-18), title return wiring beyond state allowance.

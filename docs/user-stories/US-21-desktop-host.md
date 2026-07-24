# US-21 — Play on the desktop (JVM Quil)

**Status:** backlog  
**Depends on:** US-16, US-17, US-20  
**Design:** §3, §4, §7, §10

## Story

**As a** player,  
**I want** to play Missile Command in a desktop window using Quil,  
**So that** I get a full native-resolution mouse-and-keyboard experience on the JVM.

## In scope

- Launchable desktop app (documented command).
- Quil sketch: full window resolution, resize support, modern vector render of core state.
- Mouse aim, key fire, click-zone fire wired to core commands.
- Pause, title, HUD, THE END, options, high scores, SFX, file-backed persistence.
- Draw order per design §7.2.

## Acceptance criteria

- User can start the app, start a game from title, aim, fire, pause, and reach THE END or quit cleanly.
- Window resize updates playfield layout without magnifying a fixed low-res buffer.
- High scores and options persist across app restarts on the same machine.
- End-to-end QA procedures drive the real UI (flags/QA affordances allowed only as UI, per six-pack rules).

## Out of scope

- Browser host (US-22); pixel-perfect arcade cabinet bezel.

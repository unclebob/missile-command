# US-22 — Play in the browser (ClojureScript Quil)

**Status:** in-specifier  
**Depends on:** US-21  
**Design:** §3, §4, §7, §10

## Story

**As a** player,  
**I want** the same game in the browser,  
**So that** I can play without installing a JVM desktop build.

## In scope

- ClojureScript + Quil host using the same pure core.
- Mouse/keyboard parity with desktop for aim, fire keys, click zones, pause, menus.
- Modern vector rendering at full canvas/window resolution with resize.
- `localStorage` for high scores and options.
- Synthesized SFX with mute.

## Acceptance criteria

- Documented way to build/open the browser game.
- Player can complete the same core loop: title → play → pause → THE END → high score when qualifying.
- Behavior of rules matches desktop (shared core; acceptance tests on core remain green).
- Scores/options persist across page reload.
- End-to-end QA covers the browser UI surface.

## Out of scope

- Mobile touch-first redesign; offline PWA packaging beyond what falls out naturally.

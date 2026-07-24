# Missile Command — User Story Backlog

**Source of truth for product intent:** [design specification](../superpowers/specs/2026-07-24-missile-command-design.md)  
**Execution:** [SwarmForge six-pack](https://github.com/unclebob/swarm-forge) (`specifier` → `coder` → `cleaner` → `architect` → `hardender` → `QA`)

## How to run these with six-pack

1. Install and start six-pack in this project (`BRANCH=six-pack` per SwarmForge README).
2. Give the **specifier** **one story at a time**, in the order below (or the next ready story whose dependencies are done).
3. Point the specifier at the design doc for numbers, constants, and non-goals; the story is the scope boundary for that cycle.
4. Specifier produces Gherkin + end-to-end QA procedures; after your approval, work flows through the pack.
5. When QA signals complete, pick the next story.

### Story sizing rules (for this backlog)

- Each story is one **externally visible behavior slice** suitable for one six-pack feature cycle.
- Stories stay **ignorant of code structure** (no “add namespace X”); architecture constraints live in the design doc and constitution.
- **Core** behavior is specified and tested without requiring graphics where possible; **host** stories cover UI, audio, and persistence at the user-visible surface.
- Do not implement later stories early “while we’re here” unless the current story’s acceptance criteria require it.

### Priority order

| ID | Story | Depends on |
|----|--------|------------|
| [US-01](US-01-project-foundation.md) | Project foundation and acceptance harness | — |
| [US-02](US-02-new-game-layout.md) | New game at full resolution | US-01 |
| [US-03](US-03-aim-crosshair.md) | Aim the crosshair | US-02 |
| [US-04](US-04-fire-batteries-keys.md) | Fire batteries with keys | US-03 |
| [US-05](US-05-fire-click-zone.md) | Fire by click zone | US-04 |
| [US-06](US-06-defensive-missiles-fireballs.md) | Defensive missiles and fireballs | US-04 |
| [US-07](US-07-enemy-missiles-impacts.md) | Enemy missiles destroy cities and batteries | US-06 |
| [US-08](US-08-waves-and-rearm.md) | Waves complete and batteries rearm | US-07 |
| [US-09](US-09-scoring-and-multiplier.md) | Score and multiplier | US-08 |
| [US-10](US-10-bonus-cities.md) | Bonus cities from score | US-09 |
| [US-11](US-11-mirvs.md) | MIRV warheads | US-08 |
| [US-12](US-12-smart-bombs.md) | Smart bombs | US-06, US-08 |
| [US-13](US-13-bombers-satellites.md) | Bombers and satellites | US-08 |
| [US-14](US-14-the-end.md) | THE END when cities are gone | US-10 |
| [US-15](US-15-title-screen.md) | Title screen and start | US-02 |
| [US-16](US-16-pause.md) | Pause and resume | US-15 |
| [US-17](US-17-hud.md) | In-game HUD | US-09, US-10 |
| [US-18](US-18-high-scores.md) | High-score table | US-14, US-15 |
| [US-19](US-19-options.md) | Options (mute, keys, difficulty) | US-15 |
| [US-20](US-20-sound-events.md) | Sound effects from game events | US-06, US-19 |
| [US-21](US-21-desktop-host.md) | Play on the desktop (JVM Quil) | US-16, US-17, US-20 |
| [US-22](US-22-browser-host.md) | Play in the browser (CLJS Quil) | US-21 |

Stories **US-11–US-13** may proceed in parallel after US-08 once US-06 is done (smart bombs need fireballs). Prefer serial order if the swarm runs one feature at a time.

### Global constraints (every story)

- Pure game rules must remain free of Quil, filesystem, and browser storage calls (design §3).
- Playfield uses **full native resolution**; no fixed buffer magnified to the window (design §4).
- Faithful arcade **rules** for in-scope mechanics; modern vector **look** for presentation stories.
- BDD/TDD: see behavior fail before implementing; no no-op step definitions.
- Out of scope for the product (not just one story): multiplayer, ROM-perfect timing, licensed assets, mobile-first touch UI, campaign/power-ups beyond arcade enemy types.

### Status legend

Use these labels in each story file’s status line as work progresses:

`backlog` → `in-specifier` → `in-implementation` → `done`

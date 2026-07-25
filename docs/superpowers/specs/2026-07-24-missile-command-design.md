# Missile Command — Design Specification

**Date:** 2026-07-24  
**Status:** Draft for implementation planning  
**Stack:** Clojure / ClojureScript, Quil (Processing)

## 1. Purpose

Build a dual-platform remake of the 1980 Atari arcade game *Missile Command*: defend cities from ballistic missiles using three anti-missile batteries. Game rules live in a pure, shared core; Quil hosts render and handle I/O on the JVM and in the browser.

## 2. Goals and non-goals

### Goals (v1)

- Faithful arcade **core rules** (six cities, three batteries, waves, fireballs, MIRVs, smart bombs, bombers/satellites, scoring, bonus cities, inevitable loss).
- **Shared pure game logic** in `.cljc` for JVM and browser.
- **Full native resolution** (window or fullscreen); layout derived from actual width/height — **not** a fixed logical buffer (e.g. 800×600) magnified to the screen.
- Dual input: mouse aim + three fire keys **and** click-to-fire by horizontal zone.
- Clean **modern vector** visuals.
- Full arcade-style **SFX** (synthesized).
- Shell: **title**, **HUD**, **pause**, **high scores** (local), **options** (mute, key remap, difficulty).
- **BDD/TDD**: Given/When/Then scenarios, custom runner, no no-op steps; core testable without Quil.

### Non-goals (v1)

- Online multiplayer or co-op.
- ROM-perfect timing / cycle accuracy.
- Licensed arcade audio samples or trademarked cabinet art.
- Mobile-first touch UI (mouse/keyboard are primary).
- Campaign narrative or power-up systems beyond arcade enemy types.

## 3. Architecture

> **Living plan:** module inventory, decisions, and phased extraction PRs are maintained in  
> [`docs/architecture/ADR-001-modular-core-and-extraction-plan.md`](../../architecture/ADR-001-modular-core-and-extraction-plan.md).  
> Prefer that ADR over this section when they disagree (layout and wave/bonus rules evolve).

### 3.1 Approach

**Pure functional core + thin Quil hosts.**
```
Host frame loop
  → raw mouse/keyboard
  → commands
  → core/handle + core/tick
  → {state, events}
  → host render(state) + host audio(events) + host storage when needed
```

### 3.2 Boundaries

| Layer | May | Must not |
|-------|-----|----------|
| **Core (`.cljc`)** | Own all rules, immutable state, emit events | Call Quil, audio APIs, filesystem, `localStorage` |
| **Hosts (`.clj` / `.cljs`)** | Draw, play sound, persist scores, map OS input | Implement scoring, collisions, wave generation, game-over rules |

### 3.3 Project layout

```
missile-command/
├── deps.edn
├── shadow-cljs.edn          ; or equivalent CLJS config
├── src/
│   ├── missile_command/     ; pure .cljc
│   │   ├── core.cljc        ; public API: new-game, handle, tick
│   │   ├── world.cljc       ; state shape, init, resize/layout
│   │   ├── batteries.cljc
│   │   ├── missiles.cljc    ; enemy + defensive missiles, fireballs
│   │   ├── cities.cljc
│   │   ├── waves.cljc
│   │   ├── scoring.cljc
│   │   ├── input.cljc       ; raw intent → commands; click zone → battery
│   │   ├── screens.cljc     ; screen state machine
│   │   └── events.cljc      ; event keywords / helpers
│   ├── missile_command/jvm/
│   │   ├── main.clj
│   │   ├── sketch.clj
│   │   ├── render.clj
│   │   ├── audio.clj
│   │   └── storage.clj
│   └── missile_command/browser/
│       ├── main.cljs
│       ├── sketch.cljs
│       ├── render.cljs
│       ├── audio.cljs
│       └── storage.cljs
├── test/                    ; scenarios + unit tests against core
└── docs/superpowers/specs/
```

Namespace names may be adjusted slightly during implementation; the separation of pure core vs hosts is mandatory.

### 3.4 Core public API (conceptual)

```clojure
(new-game {:width w :height h :options opts})  ;; → state
(resize state w h)                             ;; → state (reflow layout; preserve progress)
(handle state command)                         ;; → {:state s :events [...]}
(tick state dt-seconds)                        ;; → {:state s :events [...]}
```

- `dt` is real elapsed time, **clamped** (e.g. max 50ms) to avoid spiral-of-death after stalls.
- Simulation uses **seconds** (or a single consistent unit); speeds are defined relative to viewport (see §5).

## 4. Coordinate space and layout

### 4.1 Full resolution

- The playfield is the **current canvas pixel size** (`width` × `height`).
- **Do not** render into a fixed low-res buffer and scale it up as the primary mode.
- On window resize or fullscreen toggle, host calls `resize` with new dimensions.

### 4.2 Relative layout

Positions of fixed scenery are derived from width/height, for example:

| Element | Placement (normative intent) |
|---------|------------------------------|
| Ground band | Bottom ~8–12% of height |
| Six cities | Evenly spaced along ground, inward margins; left-to-right indices 0–5 |
| Left battery | Near left ground corner |
| Center battery | Bottom center |
| Right battery | Near right ground corner |
| Click zones | Three equal horizontal thirds of **width** (full sky height for click targeting) |

Exact pixel formulas live in `world` layout functions and are covered by tests at multiple aspect ratios (e.g. 16:9, 4:3, ultrawide).

### 4.3 Scaling gameplay feel

Speeds (px/s), fireball max radius, and similar quantities scale with a **size factor**, e.g. `s = min(width, height) / reference` where `reference` is a design constant (e.g. 900). Rules tests may use a fixed size for determinism; hosts always pass real size.

## 5. Game model

### 5.1 Screen state machine

```
:title ──start──► :playing ⇄ :paused
                     │
                     ▼
                  :the-end ──► :high-score-entry? ──► :title
:options ◄──► :title   (and optionally from :paused)
```

- From `:the-end`, if score qualifies for the high-score table, go to `:high-score-entry`; else return to `:title` on confirm.
- Pause freezes `tick` simulation; aim may still update for UX if desired — **spec: while paused, no simulation and no firing**.

### 5.2 Entity overview

Immutable maps/vectors in one `state` value.

| Kind | Fields (minimum) |
|------|------------------|
| **City** | `:id`, `:x`, `:y`, `:alive?` |
| **Battery** | `:id` (`:left`/`:center`/`:right`), `:x`, `:y`, `:missiles` (0–10), `:destroyed?`, `:missile-speed` |
| **Enemy missile** | `:id`, `:x0 :y0 :x1 :y1` (or parametric path), `:t` progress, `:split?` / MIRV children rules, `:target` |
| **Smart bomb** | position, velocity or guidance, evasion vs fireballs |
| **Flyer** (bomber/satellite) | path across sky, spawn timer for missiles |
| **Defensive missile** | from battery, toward aim point, speed, in-flight |
| **Fireball** | center, `:radius`, `:phase` (expand/contract), lifetime |
| **Crosshair** | `:x`, `:y` |
| **Meta** | `:screen`, `:score`, `:multiplier`, `:wave`, `:bonus-cities`, `:options`, `:high-scores` snapshot as needed |

### 5.3 Player actions

1. **Aim:** update crosshair to mouse position (clamped to playfield).
2. **Fire battery (key):** if battery not destroyed and missiles > 0, spawn defensive missile from battery toward current crosshair; decrement ammo.
3. **Fire at point (click):** map click `x` to battery by horizontal third; same fire rules toward **click position** (crosshair should match).
4. **Pause / resume / start / menu navigation:** per screen machine.

Default keys (remappable in options). **Both** letter and number bindings are active by default:

| Action | Default keys |
|--------|----------------|
| Fire left | `Z` and `1` |
| Fire center | `X` and `2` |
| Fire right | `C` and `3` |
| Pause | `P` and `Esc` |
| Confirm / start | `Enter` or primary UI click |

Both key and click firing are **always available** during `:playing` (not alternate modes).

### 5.4 Simulation rules

1. **Defensive missile** travels from battery to aim point; on arrival becomes a **fireball**.
2. **Fireball** expands then contracts; any enemy munition or flyer whose position intersects the fireball disk is destroyed (smart bombs may **evade** if the fireball is poorly centered — see §5.6).
3. **Enemy missile** that reaches a city or battery destroys that target (single hit). Batteries destroyed have missiles remaining set ineffective (cannot fire).
4. **Center battery** missiles use higher speed than left/right (required for effective smart-bomb defense at range).
5. **Wave ends** when all scheduled enemies for the wave are destroyed or have impacted.
6. **Between waves:** award bonuses; apply bonus cities from reserve to replace dead cities as needed; **restore all batteries** (clear destroyed, refill to 10). Cities can also return via bonus cities.
7. **Game over:** zero living cities and zero bonus cities available to place → transition toward **THE END**.
8. There is **no win** state.

**Battery rearm:** At wave start, each non-destroyed battery is refilled to 10 missiles.

**City restoration:** Bonus cities in reserve are applied when the player has fewer than 6 living cities (after wave resolution / when earned), one-for-one.

### 5.5 Waves and enemies

Waves increase difficulty: more missiles, higher speed, introduce new types.

| Wave band | Content (intent) |
|-----------|------------------|
| Early | Ballistic missiles only; moderate speed |
| Mid | MIRVs (split into multiple warheads mid-descent) |
| Later | Smart bombs; bombers and satellites that traverse and drop missiles |

Exact counts and speeds are functions of `:wave` and difficulty preset (`:easy` / `:normal` / `:arcade`). **`:arcade`** is the target balance; easy/normal reduce counts and speeds by fixed ratios (e.g. 0.7 / 0.85).

Enemy targeting: choose among living cities and non-destroyed batteries (weighted or uniform — implementation picks one deterministic policy for tests).

### 5.6 Smart bombs

- Move toward a target (city/battery).
- If a fireball would destroy them but the bomb is near the edge of the blast (or blast center is far relative to bomb), they **steer away** once per threat (arcade-like “smart” behavior).
- Center battery’s faster intercepts remain the reliable answer at distance.

Precise evasion parameters are constants in `missiles` / `waves`, tuned in playtests, locked by regression scenarios.

### 5.7 Scoring (arcade-inspired defaults)

Base values **before** multiplier:

| Event | Points |
|-------|--------|
| Destroy enemy missile | 25 |
| Destroy bomber | 100 |
| Destroy satellite | 100 |
| Destroy smart bomb | 125 |
| Unused defensive missile at wave end | 5 |
| Surviving city at wave end | 100 |

- **Multiplier:** starts at 1×; increases by 1 every two waves; **max 6×**. Applies to destruction scores and end-of-wave bonuses.
- **Bonus city:** every **10,000** points (threshold configurable constant); added to reserve, not immediately exceeding 6 living cities without need.

Score never decreases. HUD shows current score, multiplier, wave, ammo per battery, living cities, bonus cities in reserve.

### 5.8 THE END

- Display **“THE END”** (not “Game Over”).
- Show ruined cities / final score.
- Then high-score entry if applicable.

## 6. Shell features

### 6.1 Title

- Game title, prompt to start, pointer to options/high scores.
- Optional light attract animation (non-interactive missiles) — nice-to-have; not required for first vertical slice.

### 6.2 HUD

- Always on during `:playing` and `:paused` (paused shows overlay).
- Score, wave, multiplier, battery ammo (L/C/R), cities summary, bonus cities.

### 6.3 Pause

- Toggles `:playing` ⇄ `:paused`.
- No `tick` advancement; no fire commands applied (or fire commands ignored).

### 6.4 High scores

- Table of top **N** (default 10): initials (3 chars) + score.
- **JVM:** read/write a file under user config or project-local path (e.g. `~/.missile-command/scores.edn`).
- **Browser:** `localStorage` key for the same EDN/JSON shape.
- Host loads scores into state at startup; core validates insert; host persists on new entry.

### 6.5 Options

| Option | Values |
|--------|--------|
| Mute | boolean |
| Key bindings | map of action → key |
| Difficulty | `:easy` \| `:normal` \| `:arcade` |

Options persist with the same storage mechanism as high scores (single settings blob allowed).

## 7. Rendering

### 7.1 Visual style

**Clean modern vector:** gradient sky and ground, soft fireballs, smooth trails, simple geometric cities and batteries, readable sans-style HUD text (Quil text), no CRT scanline requirement.

### 7.2 Draw order

1. Background sky  
2. Enemy trails and missiles  
3. Flyers  
4. Defensive missiles  
5. Fireballs  
6. Ground, cities, batteries  
7. Crosshair  
8. HUD and screen overlays (title, pause, THE END, menus)

### 7.3 Host-only

All drawing in `render` namespaces. Core exposes data only (positions, radii, flags).

## 8. Sound

Core emits events; hosts play synthesized SFX unless muted.

| Event | When |
|-------|------|
| `:sfx/launch` | Defensive missile fired |
| `:sfx/boom` | Expanding combat fireball onset (defensive arrival or ground impact) |
| `:sfx/intercepted` | Fireball destroys an enemy (intercept) |
| `:sfx/warning` | Title screen (once per visit) |
| `:sfx/explosion` | Other major blast (legacy; not used for intercept) |
| `:sfx/city-destroyed` | City hit |
| `:sfx/battery-destroyed` | Battery hit |
| `:sfx/low-ammo` | Battery ammo hits a low threshold (e.g. 2) on fire |
| `:sfx/wave` | Wave banner shown (between waves) |
| `:sfx/wave-clear` | Legacy alias (unused; banner uses `:sfx/wave`) |
| `:sfx/bonus-city` | Bonus city earned |
| `:sfx/the-end` | THE END sequence |
| `:sfx/ui` | Menu confirm/navigate (optional) |

No streaming music required for v1.

## 9. Testing strategy

### 9.1 BDD

- Natural-language **Given / When / Then** scenarios for behaviors (firing, intercepts, city destruction, scoring, wave end, THE END, pause, high-score qualification, click-zone battery selection, resize layout invariants).
- Custom **parser/runner** maps scenario language to core API calls.
- **No pending or no-op step definitions.**
- Watch each new scenario **fail** before implementing production code (TDD three laws).

### 9.2 Isolation

- Core tests run without Quil, audio, or real storage (storage ports faked if tested).
- Prefer `.cljc` tests so both runtimes share rule verification where practical.

### 9.3 Coverage intent

- High line/branch coverage on core rules.
- Host code kept thin; optional manual smoke on JVM and browser.

## 10. Technology

| Concern | Choice |
|---------|--------|
| Build | `deps.edn` |
| CLJS | shadow-cljs |
| Graphics | quil (JVM + CLJS) |
| Shared logic | `.cljc` |
| Scores/settings | EDN file (JVM), `localStorage` (browser) |

## 11. Implementation slices (for planning)

Executable backlog for SwarmForge six-pack: **[docs/user-stories/](../../user-stories/README.md)** (US-01 … US-22).

Suggested vertical order (mirrors the user stories):

1. Project skeleton + core `state` / `new-game` / `resize` + BDD/APS harness (US-01–02).  
2. Aim + fire + defensive missiles + fireballs (US-03–06).  
3. Enemy missiles + city/battery hits (US-07).  
4. Waves, scoring, multiplier, bonus cities (US-08–10).  
5. MIRVs, smart bombs, flyers (US-11–13).  
6. Screens: THE END, title, pause, HUD, high scores, options (US-14–19).  
7. SFX events + hosts (US-20–22).

## 12. Decisions log

| Decision | Choice |
|----------|--------|
| Platforms | JVM + browser, shared `.cljc` |
| Architecture | Pure core + thin Quil hosts |
| Fidelity | Faithful arcade core rules |
| Resolution | Full native; no fixed-buffer magnify |
| Input | Keys for L/C/R **and** click-by-third |
| Visuals | Clean modern vector |
| Sound | Full synthesized SFX via events |
| Shell | Title, HUD, pause, high scores, options |
| Testing | BDD scenarios + TDD on pure core |

## 13. Open parameters (constants, not open design)

These are tunable constants with defaults above; changing them does not require redesign:

- Bonus city score interval (default 10000)  
- Multiplier schedule (default +1× every 2 waves, cap 6×)  
- Point table (§5.7)  
- Smart-bomb evasion thresholds  
- Size reference for speed scaling  
- High-score table length N (default 10)  
- Low-ammo SFX threshold  

---

*End of design specification.*

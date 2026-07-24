# Missile Command

Dual-platform remake of the Atari arcade game *Missile Command*: pure shared
core rules with thin Quil hosts for JVM and browser.

Design specification: [docs/superpowers/specs/2026-07-24-missile-command-design.md](docs/superpowers/specs/2026-07-24-missile-command-design.md)

User stories: [docs/user-stories/](docs/user-stories/)

## Requirements

- [Clojure CLI](https://clojure.org/guides/install_clojure)
- [Babashka](https://babashka.org/) (`bb`)
- APS tools on `PATH`: `gherkin-parser` (and optionally `gherkin-mutator`) from
  [Acceptance-Pipeline-Specification](https://github.com/unclebob/Acceptance-Pipeline-Specification)

## Layout

```text
src/missile_command/          pure core (.cljc) and acceptance runtime
spec/missile_command/         Speclj unit tests
features/                     Gherkin feature files
acceptance/generated/         generated acceptance entrypoints (build product)
build/acceptance/ir/          Gherkin JSON IR (build product)
```

Host namespaces (`missile-command.jvm`, `missile-command.browser`) will be added
in later stories. Core unit tests do not load host code.

## Run tests

From the project root:

### Unit tests

```sh
bb test
```

Runs Speclj structure check on `spec/`, then Speclj unit tests.

### Acceptance tests (normal APS pipeline)

```sh
bb accept
```

Runs `gherkin-parser` → project acceptance entrypoint generator → generated
executable tests.

### Architecture check

```sh
bb arch-check
```

Fails if pure core (`.cljc` game rules) requires hosts, acceptance, or IO libs.

### Property tests

```sh
bb property
```

Runs generative property tests (`test-property/`) separately from unit coverage.

### Launch (JVM host)

```sh
bb play
```

Opens a resizable Quil window at full playfield resolution (default 800×600;
optional `bb play 1280 720`). The window opens on the **screen under the mouse
pointer** (not forced onto the main display) and **does not steal keyboard
focus**. Mouse moves the crosshair (clamped to the playfield). **Click** fires by
horizontal third (with empty/destroyed fallback). Default fire keys: left `Z`/`1`,
center `X`/`2`, right `C`/`3`. Esc quits. OS cursor is hidden; only the game
crosshair is shown. Host only draws and routes input; rules stay in the pure core.

### QA telemetry and setup switches

These flags are **user-facing CLI affordances** on the normal launch command
(not a private in-process API).

#### `--qa-telemetry`

Prints a line after each fire attempt (click or key) on stdout:

```text
qa-fire battery=left missiles_in_flight=1 origin_x=40 origin_y=540 target_x=200 target_y=120
```

| Field | Meaning |
|-------|---------|
| `battery=` | `left`, `center`, `right`, or `none` |
| `missiles_in_flight=` | Defensive missiles currently in flight |
| `origin_x=` / `origin_y=` | Launch point of each in-flight missile |
| `target_x=` / `target_y=` | Aim/detonation point of each in-flight missile |

When several missiles are in flight, origin/target pairs repeat on the same line.

```sh
bb play --qa-telemetry
# or: bb play -- --qa-telemetry
```

#### `--destroy-batteries <list>`

Starts with named batteries already destroyed (`left`, `center`, `right`, comma-separated).

```sh
bb play --qa-telemetry --destroy-batteries left
bb play --qa-telemetry --destroy-batteries left,center
bb play --qa-telemetry --destroy-batteries left,center,right
```

Destroyed batteries cannot key-fire. Click-zone fire skips them in the zone
fallback order (`features/fire-click-zone.feature`).

#### `--qa-events <file>`

Host automation: after launch, the host applies one event per frame from a text
file (same path as mouse/key handlers — not a private core API). Lines:

```text
aim 400 200
click 100 150
key z
key 1
quit
```

```sh
bb play --qa-telemetry --qa-events tmp/qa-events.txt
```

### Hardening (mutation / CRAP / DRY)

```sh
bb mutate src/missile_command/core.cljc --max-workers 8
bb accept-mutate
bb crap
bb dry
```

Language mutation uses differential manifests embedded in source files.
Gherkin acceptance mutation uses `gherkin-mutator` with the project runner
adapter (`clojure -M:acceptance-mutation-runner`).

## Core smoke API

```clojure
(require '[missile-command.core :as core])

(def state (core/new-game {:width 800 :height 600}))
(core/playfield-width state)   ; => 800
(core/playfield-height state)  ; => 600
```

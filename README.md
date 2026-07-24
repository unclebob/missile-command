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
optional `bb play 1280 720`). The window opens on the **screen of the terminal
where `bb play` was typed** (tmux client TTY → Terminal.app window match; then
process TTY; then frontmost window; then pointer) and **does not steal keyboard
focus**. Mouse moves the crosshair (clamped to the playfield). **Click** fires by
horizontal third (with empty/destroyed fallback). Default fire keys: left `Z`/`1`,
center `X`/`2`, right `C`/`3`. Esc quits. OS cursor is hidden; only the game
crosshair is shown. Host only draws and routes input; rules stay in the pure core.

### QA telemetry and setup switches

These flags are **user-facing CLI affordances** on the normal launch command
(not a private in-process API).

#### `--qa-telemetry`

Prints lines on stdout for fires, simulation snapshots, fireball phases, and targets:

```text
qa-fire battery=left missiles_in_flight=1 origin_x=40 origin_y=540 target_x=200 target_y=120
qa-fireball id=3 phase=start t=1.2 center_x=200 center_y=120 radius=0.0
qa-fireball id=3 phase=max t=1.6 center_x=200 center_y=120 radius=40.0
qa-fireball id=3 phase=shrink t=1.7 center_x=200 center_y=120 radius=30.0
qa-fireball id=3 phase=end t=2.0 center_x=0 center_y=0 radius=0.0
qa-sim t=1.5 missiles_in_flight=0 fireballs=1 center_x=200 center_y=120 radius=20.0
```

| Field | Meaning |
|-------|---------|
| `battery=` | `left`, `center`, `right`, or `none` (on `qa-fire`) |
| `missiles_in_flight=` | Defensive missiles currently in flight |
| `origin_x=` / `origin_y=` | Launch point of each in-flight missile |
| `target_x=` / `target_y=` | Aim/detonation point of each in-flight missile |
| `phase=` | Fireball lifecycle: `start`, `expand`, `max`, `shrink`, `end` |
| `center_x` / `center_y` / `radius` | Fireball blast geometry (required on every live-fireball line) |
| `fireballs=` | Count of active fireballs (when reported on `qa-sim`) |
| `destroyed=` | Destroyable target status when targets are present |
| `enemy_missiles=` | Enemy ballistic missiles in flight |
| Per enemy missile | origin, current position or progress, target (city index or battery id) |
| Cities / batteries | living vs destroyed state as needed for QA |

When several missiles are in flight, origin/target pairs repeat on the same line
(or one line per missile). Multiple fireballs each report their own center and radius.

##### Fireball phase timing

| Phase | Meaning |
|-------|---------|
| `start` | Fireball created; expand begins (`t`, `center_x`, `center_y`, `radius`) |
| `max` | Peak radius (`t`, center, radius) |
| `shrink` | Contracting samples after max (`t`, center, radius) |
| `end` | Fireball gone (`t`; center/radius optional) |

Ordering: `start.t` ≤ `max.t` ≤ `shrink.t` ≤ `end.t`. Radius at max > start;
shrink radius < max. Center stays at the detonation point while live.

```sh
bb play --qa-telemetry
```

#### `--qa-target <x>,<y>`

Spawn a destroyable test target at playfield coordinates (repeatable flag) for
fireball hit/miss checks (see `features/defensive-missiles-fireballs.feature`
and `qa/procedures/defensive-missiles-fireballs.qa.md`).

```sh
bb play --qa-telemetry --qa-target 400,200
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
wait 2.5
quit
```

`wait SECONDS` pauses scripted events for wall-clock time while simulation ticks.

```sh
bb play --qa-telemetry --qa-events tmp/qa-events.txt
```

#### `--qa-enemy <spec>`

Spawns a scripted enemy ballistic missile for tests (wave system may still be
minimal). `<spec>` forms:

- `city:<index>` — target living city index `0`–`5`
- `battery:left` | `battery:center` | `battery:right` — target that battery

```sh
bb play --qa-telemetry --qa-enemy city:0
bb play --qa-telemetry --qa-enemy battery:left
```

Equivalent lines may also appear in `--qa-events` files, e.g. `enemy city 0` or
`enemy battery left` (exact spelling documented here if it differs).

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

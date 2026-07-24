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
optional `bb play 1280 720`). Mouse moves the crosshair (clamped to the playfield).
Default fire keys: left `Z`/`1`, center `X`/`2`, right `C`/`3`. Esc quits.
Host only draws and routes input; game rules stay in the pure core.

### Hardening (mutation / CRAP / DRY)

```sh
bb mutate src/missile_command/core.cljc --max-workers 8
bb accept-mutate
bb crap
bb dry src
```

Language mutation uses differential manifests embedded in source files.
Gherkin acceptance mutation uses `gherkin-mutator` with the project runner
adapter (`clojure -M:acceptance-mutation-runner`).

## Run the app

Desktop host launch (added with the aim/fire host slice; exact alias may match
`bb run` or `clojure -M:run` as implemented):

```sh
bb run
```

### QA telemetry and setup switches

These flags are **user-facing CLI affordances** for QA (not a private API).
They must be available on the normal launch command once the host exists.

#### `--qa-telemetry`

Prints a line after each fire attempt (stdout), including at least:

| Field | Meaning |
|-------|---------|
| `battery=` | `left`, `center`, `right`, or `none` (on fire attempts) |
| `missiles_in_flight=` | Count of defensive missiles currently in flight |
| Per missile in flight | Flight vector: origin and target in playfield coordinates |
| `fireballs=` | Count of active fireballs (after ticks / arrivals) |
| **Each fireball** | **Required on every fireball line:** `center_x`, `center_y`, `radius`, plus phase timing (below) |
| Destroyable targets (if any) | position and `destroyed=true\|false` |

#### Fireball center and radius (required)

Every telemetry line that describes a fireball (phase sample, tick sample, or
snapshot of active fireballs) **must** include for **each** fireball:

| Field | Meaning |
|-------|---------|
| `center_x=` | Fireball center X in playfield coordinates |
| `center_y=` | Fireball center Y in playfield coordinates |
| `radius=` | Current fireball radius in playfield units |

If multiple fireballs are active, each fireball’s `center_x`, `center_y`, and
`radius` are printed (one line per fireball, or clearly grouped on one line).

#### Fireball phase timing

Telemetry must report simulation times for fireball lifecycle so QA can verify
expand and contract. Preferred line shape (one event per phase transition or
tick sample). **Every phase sample that refers to a live fireball still includes
`center_x`, `center_y`, and `radius`:**

| Phase | Meaning | Example fields |
|-------|---------|----------------|
| `start` | Fireball created (missile arrived); expand begins | `phase=start t=<seconds> center_x=... center_y=... radius=...` |
| `max` | Radius at peak; expand ends / shrink begins | `phase=max t=<seconds> center_x=... center_y=... radius=...` |
| `shrink` | Contracting (samples after max, before end) | `phase=shrink t=<seconds> center_x=... center_y=... radius=...` |
| `end` | Fireball gone | `phase=end t=<seconds>` (center/radius optional once gone) |

Ordering for one fireball: `start.t` ≤ `max.t` ≤ `shrink.t` ≤ `end.t`.
Radius at `max` must be greater than at `start`; a `shrink` sample must show
radius less than at `max`. Center should stay at the detonation point for the
life of the fireball.

Example shapes (exact spacing may vary; fields must be parseable):

```text
qa-fire battery=left missiles_in_flight=1 origin_x=40 origin_y=540 target_x=200 target_y=120
qa-fireball phase=start t=1.20 center_x=200 center_y=120 radius=1
qa-fireball phase=max t=1.45 center_x=200 center_y=120 radius=40
qa-fireball phase=shrink t=1.55 center_x=200 center_y=120 radius=28
qa-fireball phase=end t=1.80
```

When several missiles or fireballs are active, each entity’s fields are included
(one line with repeated groups, or one line per entity—keep this section in sync
if the chosen form differs).

```sh
bb run -- --qa-telemetry
```

#### `--destroy-batteries <list>`

Starts the game with the named batteries already destroyed. `<list>` is a
comma-separated set of `left`, `center`, and/or `right`.

```sh
bb run -- --qa-telemetry --destroy-batteries left
bb run -- --qa-telemetry --destroy-batteries left,center
bb run -- --qa-telemetry --destroy-batteries left,center,right
```

Destroyed batteries cannot fire on key press. Click-zone fire skips them in the
zone fallback order (see `features/fire-click-zone.feature` and
`qa/procedures/fire-click-zone.qa.md`).

#### `--qa-target <x>,<y>`

Places a single destroyable test target at playfield coordinates `(x, y)` for
fireball hit/miss checks (see `features/defensive-missiles-fireballs.feature`
and `qa/procedures/defensive-missiles-fireballs.qa.md`).

```sh
bb run -- --qa-telemetry --qa-target 400,200
```

## Core smoke API

```clojure
(require '[missile-command.core :as core])

(def state (core/new-game {:width 800 :height 600}))
(core/playfield-width state)   ; => 800
(core/playfield-height state)  ; => 600
```

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
| `battery=` | `left`, `center`, `right`, or `none` |
| `missiles_in_flight=` | Count of defensive missiles currently in flight |
| Per missile in flight | Flight vector: origin and target in playfield coordinates |

Example shape (exact spacing may vary; fields must be parseable):

```text
qa-fire battery=left missiles_in_flight=1 origin_x=40 origin_y=540 target_x=200 target_y=120
```

When several missiles are in flight, each missile’s vector is included (one line
with repeated origin/target groups, or one line per missile—document the chosen
form in this section if it differs).

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

## Core smoke API

```clojure
(require '[missile-command.core :as core])

(def state (core/new-game {:width 800 :height 600}))
(core/playfield-width state)   ; => 800
(core/playfield-height state)  ; => 600
```

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
focus** — the previous frontmost app (e.g. Terminal) is restored after open;
click the game window when you want to type into it. Mouse moves the crosshair
(clamped to the playfield). **Click** fires by horizontal third (with empty/destroyed
fallback). Default fire keys: left `Z`/`1`, center `X`/`2`, right `C`/`3`. Esc quits.
OS cursor is hidden; only the game crosshair is shown. Host only draws and routes
input; rules stay in the pure core.

### QA mode (CLI affordances)

QA uses a small, stable launch surface—not a private core API:

| Flag | Role |
|------|------|
| `--qa` | Enable QA mode: **telemetry on**, accept scenario/events |
| `--qa-scenario <file>` | Initial world state (EDN) |
| `--qa-events <file>` | Timed input script (text) |

```sh
bb play --qa
bb play --qa --qa-scenario tmp/wave-rearm.edn
bb play --qa --qa-scenario tmp/setup.edn --qa-events tmp/clicks.txt
bb play 1280 720 --qa --qa-scenario tmp/setup.edn
```

Optional: `--qa-events` alone with `--qa` (default new-game state, scripted input only).

#### Scenario file (EDN)

One structured map for **setup**. New stories add keys; they do not add new CLI
flags. Omitted keys keep normal new-game defaults.

```edn
{:width 800
 :height 600
 :wave 1
 :batteries {:left   {:ammo 2}
             :center {:ammo 2}
             :right  {:ammo 2 :destroyed false}}
 :cities {:destroyed [4 5]}          ; indices 0–5 left-to-right; others living
 :enemies [{:target [:city 0]}
           {:target [:battery :left]}]
 :targets [{:x 400 :y 200}]}         ; optional destroyable stubs (fireball tests)
```

| Key | Meaning |
|-----|---------|
| `:width` / `:height` | Playfield size (else CLI size / default) |
| `:wave` | Starting wave number |
| `:batteries` | Per `:left` / `:center` / `:right`: `:ammo` (0–10), optional `:destroyed` |
| `:cities` | `:destroyed` and/or `:alive` vectors of city indices `0`–`5`; layout positions follow normal world layout |
| `:enemies` | Scripted enemy missiles at start; each `:target` is `[:city n]` or `[:battery :left|:center|:right]` |
| `:targets` | Optional destroyable test targets at playfield coordinates |

Examples for common setups:

```edn
;; Depleted ammo, two cities ruined — stage a wave rearm check
{:batteries {:left {:ammo 2} :center {:ammo 2} :right {:ammo 2}}
 :cities {:destroyed [4 5]}
 :enemies [{:target [:city 0]}]}

;; Left battery destroyed; empty preferred click-zone fallback tests
{:batteries {:left {:destroyed true :ammo 10}}}

;; Fireball hit/miss stub target
{:targets [{:x 400 :y 200}]}
```

#### Events file (text)

**Actions over time** only (not initial state). Host applies one line per frame
(or documented pacing) through the same input path as mouse/keyboard:

```text
aim 400 200
click 100 150
key z
key 1
key x
wait 10
quit
```

| Line | Meaning |
|------|---------|
| `aim <x> <y>` | Move crosshair (clamped) |
| `click <x> <y>` | Click-zone fire at point |
| `key <name>` | Key fire / UI key (`z`, `1`, `x`, `2`, `c`, `3`, …) |
| `wait <n>` | Optional: wait `n` frames (if implemented) |
| `quit` | Exit cleanly |

#### Telemetry (stdout when `--qa`)

Line-oriented `key=value` records after fires and simulation updates:

```text
qa-fire battery=left missiles_in_flight=1 origin_x=40 origin_y=540 target_x=200 target_y=120
qa-fireball id=3 phase=start t=1.2 center_x=200 center_y=120 radius=0.0
qa-fireball id=3 phase=max t=1.6 center_x=200 center_y=120 radius=40.0
qa-fireball id=3 phase=shrink t=1.7 center_x=200 center_y=120 radius=30.0
qa-fireball id=3 phase=end t=2.0 center_x=0 center_y=0 radius=0.0
qa-sim t=1.5 missiles_in_flight=0 fireballs=1 enemy_missiles=1 center_x=200 center_y=120 radius=20.0
  enemy_x=... enemy_y=... enemy_target=city:0 cities_alive=6
```

| Field | Meaning |
|-------|---------|
| `battery=` | `left`, `center`, `right`, or `none` (fire attempts) |
| `missiles_in_flight=` | Defensive missiles in flight |
| `origin_*` / `target_*` | Per defensive missile flight vector |
| **Each live fireball** | **Required:** `center_x`, `center_y`, `radius` |
| Fireball `phase=` | `start` \| `max` \| `shrink` \| `end` with monotonic `t=` |
| `enemy_missiles=` | Enemy missiles in flight |
| `enemy_x` / `enemy_y` / `enemy_target=` | Per-enemy position and target (`city:N` or `battery:id`) |
| `cities_alive=` / battery destroyed flags | Living cities / battery state |
| `wave=` / `wave_complete=` | Wave lifecycle |
| Destroyable targets | position and `destroyed=true\|false` |

Fireball phase order for one blast: `start.t` ≤ `max.t` ≤ `shrink.t` ≤ `end.t`.
Radius at max > start; shrink radius < max; center stays on detonation point.

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

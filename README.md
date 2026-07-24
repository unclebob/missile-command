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
| `--qa-events <file>` | Timed input script (text); `wait` is wall-clock seconds |
| `--qa-speed <n>` | Multiply sim-time advance vs wall clock (default `1`) |

Prints lines on stdout for fires, simulation snapshots, fireballs, enemies, and targets:

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
| `battery=` | `left`, `center`, `right`, or `none` (on `qa-fire`) |
| `missiles_in_flight=` | Defensive missiles currently in flight |
| `origin_x=` / `origin_y=` | Launch point of each in-flight missile |
| `target_x=` / `target_y=` | Aim/detonation point of each in-flight missile |
| `phase=` | Fireball lifecycle: `start`, `expand`, `max`, `shrink`, `end` |
| `center_x` / `center_y` / `radius` | Fireball blast geometry (required while live) |
| `enemy_missiles=` | Enemy ballistic missiles in flight |
| `enemy_x` / `enemy_y` / `enemy_target=` | Per-enemy position and target (`city:N` or `battery:id`) |
| `cities_alive=` / battery destroyed flags | Living cities / battery state |
| `battery_*_ammo=` | Remaining missiles per battery |
| `wave=` / `wave_complete=` | Current wave number / completion flag |
| `wave_enemy_count=` / `wave_enemy_speed=` | Scheduled hardness metrics for current wave |
| `destroyed=` | Destroyable target status when targets are present |

Ordering for fireball phases: `start.t` ≤ `max.t` ≤ `shrink.t` ≤ `end.t`.

```sh
bb play --qa
# equivalent: bb play --qa-telemetry
```

#### `--qa-speed <n>`

Multiply simulation time advance relative to wall clock (default `1`). Host
substeps at the normal physics max-dt so large factors stay stable. `wait` in
event scripts remains **wall-clock seconds**.

```sh
# ~10× faster sim: a 5.7s enemy flight finishes in ~0.6s wall clock
bb play --qa --qa-speed 10 --qa-enemy city:0 --qa-events tmp/events.txt
```

#### `--qa-scenario <file.edn>`

Load initial world state from EDN (ammo, destroyed batteries/cities, enemies, wave):

```edn
{:batteries {:left {:ammo 2} :center {:ammo 2} :right {:ammo 2}}
 :enemies [{:target [:city 0]}]}
```

```sh
bb play --qa --qa-speed 10 --qa-scenario tmp/wave-rearm-depleted.edn --qa-events tmp/events.txt
```

| Key | Meaning |
|-----|---------|
| `:width` / `:height` | Playfield size (else CLI size / default) |
| `:wave` | Starting wave number |
| `:batteries` | Per `:left` / `:center` / `:right`: `:ammo` (0–10), optional `:destroyed` |
| `:cities` | `:destroyed` and/or `:alive` vectors of city indices `0`–`5`; layout positions follow normal world layout |
| `:enemies` | Scripted enemy missiles at start; each `:target` is `[:city n]` or `[:battery :left|:center|:right]` |
| `:targets` | Optional destroyable test targets at playfield coordinates |

Scenario EDN examples:

```edn
;; Left battery destroyed; empty preferred click-zone fallback tests
{:batteries {:left {:destroyed true :ammo 10}}}

;; Fireball hit/miss stub target
{:targets [{:x 400 :y 200}]}
```

```sh
bb play --qa-telemetry --qa-target 400,200
```

#### `--qa-enemy city:N` or `--qa-enemy battery:left|center|right`

Spawn one enemy ballistic missile toward a city index or battery.

```sh
bb play --qa-telemetry --qa-enemy city:0
bb play --qa-telemetry --qa-enemy battery:left
```

#### Events file (text)

**Actions over time** only (not initial state). Host applies events through the
same input path as mouse/keyboard:

```text
aim 400 200
click 100 150
key z
key 1
key x
wait 1.2
quit
```

| Line | Meaning |
|------|---------|
| `aim <x> <y>` | Move crosshair (clamped) |
| `click <x> <y>` | Click-zone fire at point |
| `key <name>` | Key fire / UI key (`z`, `1`, `x`, `2`, `c`, `3`, …) |
| `wait <n>` | Wait `n` **wall-clock** seconds (sim advances `n * qa-speed`) |
| `quit` | Exit cleanly |

#### Telemetry (stdout when `--qa`)

Line-oriented `key=value` records after fires and simulation updates:

```text
qa-fire battery=left missiles_in_flight=1 origin_x=40 origin_y=540 target_x=200 target_y=120
qa-fireball phase=start t=1.20 center_x=200 center_y=120 radius=1
qa-fireball phase=max t=1.45 center_x=200 center_y=120 radius=40
qa-fireball phase=shrink t=1.55 center_x=200 center_y=120 radius=28
qa-fireball phase=end t=1.80
```

| Field | Meaning |
|-------|---------|
| `battery=` | `left`, `center`, `right`, or `none` (fire attempts) |
| `missiles_in_flight=` | Defensive missiles in flight |
| `origin_*` / `target_*` | Per defensive missile flight vector |
| **Each live fireball** | **Required:** `center_x`, `center_y`, `radius` |
| Fireball `phase=` | `start` \| `max` \| `shrink` \| `end` with monotonic `t=` |
| `enemy_missiles=` | Enemy missiles in flight; per-enemy origin/position/target |
| Cities / batteries | Living vs destroyed; ammo as needed |
| `wave=` / `wave_complete=` | Wave lifecycle |
| Destroyable targets | position and `destroyed=true\|false` |

Fireball phase order for one blast: `start.t` ≤ `max.t` ≤ `shrink.t` ≤ `end.t`.
Radius at max > start; shrink radius < max; center stays on detonation point.


#### `--qa-wave N` / wave control (US-08)

When documented by the host, force or report the current wave index for QA
(rearm, schedule). Prefer README flags; if only events exist, use `wave N` in
`--qa-events`.

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

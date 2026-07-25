# ADR-001 — Modular core and extraction plan

**Status:** Accepted  
**Date:** 2026-07-25  
**Context:** Missile Command dual-host (JVM Quil + browser) remake under SwarmForge.

## Decision

Keep the **pure functional core + thin hosts** architecture. Evolve `core` from a god module into a **small facade**. Move combat, wave policy, bonus cities, and shell transitions into focused pure modules. Hosts must not own game rules (including “when does the next attack start?”).

## Architecture thesis (unchanged)

```
Host frame loop
  → raw mouse/keyboard → commands
  → core/handle + core/tick
  → state (+ sound/event side channel)
  → host render(state) + host audio + host storage
```

| Layer | May | Must not |
|-------|-----|----------|
| **Pure `.cljc`** | Rules, immutable state, pure transforms | Quil, audio APIs, filesystem, `localStorage` |
| **Hosts** | Draw, play sound, persist, map OS input | Scoring, collisions, wave generation, game-over |
| **Acceptance** | Steps against core public API / host UI | Private layout internals (`world`), host namespaces |

Enforced by `script/arch_check.bb`.

## Current structure (as of this ADR)

### Pure modules (healthy)

| Module | Responsibility |
|--------|----------------|
| `entities`, `cities`, `batteries` | Scenery entities |
| `missiles`, `flyers` | Motion / fireballs / flyers |
| `waves` | Counts, speed, multipliers, sky origins |
| `wave-schedule` | Sequential attacks, spawn salvos/specials |
| `wave-lifecycle` | Wave complete, rearm, banner handoff |
| `wave-banner` | Inter-wave banner state |
| `scoring`, `game-end`, `sfx`, `screens`, `options`, `high-scores`, `hud`, `input`, `world` | As named |

### Facade pressure

`missile-command.core` (~1.3k LOC, ~90 public defns) still holds:

- Combat tick (enemies, MIRVs, smart bombs, flyers, fireballs)
- Shell transitions (title, pause, options, high scores, THE END)
- Bonus city award/place
- Wave wiring (hooks into schedule/lifecycle)
- Large **test/staging** surface (`route-*`, fixed fireballs, etc.)

### Hosts

- **JVM:** modular shell renders; `sketch.clj` + fat `input.clj` (CLI, QA telemetry, scenarios).
- **Browser:** thin `render.cljs` composing combat/scenery/shells.
- **Shared issue:** both hosts call `ensure-wave-enemies` → `activate-wave-schedule` for attack 1; attacks 2..N advance inside `core/tick`.

### Events

- Design API returns `{:state s :events [...]}`.
- Production SFX use cumulative `:sfx-events`; hosts play by length delta.
- Prefer converging on one model (see plan).

### Game rules (normative)

- Wave = **3 sequential salvos** of **3 ballistics**; specials on **last** attack only.
- Sky entry **x is random** per weapon (`waves/random-sky-origin-x`).
- Bonus cities: **award** on score threshold; **place from reserve only at wave end**.
- THE END when living cities = 0 and reserve = 0 (no mid-wave place).

## Consequences

### Positive

- Shared rules stay host-agnostic and highly testable.
- Wave policy is already partly modular (schedule + lifecycle).
- Arch check prevents host leakage into pure core.

### Negative / debt

- `core` remains hard to navigate and mutate.
- Hosts can desync on wave start policy.
- Dual event channels confuse host authors.
- Acceptance step volume (~4k LOC) multiplies change cost.
- Design doc §3 lags real modules.

## Target shape

```
acceptance / QA
      │
      ▼
core (facade only)
  new-game, handle, tick, resize
  thin re-exports for hosts
      │
      ├── combat          enemy/flyer/fireball simulation
      ├── bonus-cities    award + place + banner flag
      ├── shell           title/pause/options/high-score/THE END entry
      ├── wave-schedule   (existing)
      ├── wave-lifecycle  (existing)
      └── domains         missiles, waves, scoring, …
      │
hosts (I/O only)
  no wave policy; optional seed/RNG for QA
```

### Core public surface (host-facing)

Required by hosts and continuous play:

- `new-game`, `resize`, `handle`, `tick`
- Screen predicates and draw inputs (`hud`, cities, batteries, missiles, flyers, fireballs, banner, end-fireball, options, high scores)
- `activate-wave-schedule` **or** equivalent internalized so hosts need not call it
- SFX read/drain API (after event unification)

Move to **`missile-command.testing`** (or `core.testing`):

- `route-*`, `add-static-fireball`, `set-flyer-drops-*`, most absolute score/reserve setters used only by specs
- Keep thin setup helpers on core only if acceptance steps require stable names

## Extraction plan (PR-sized steps)

Do **one PR (or SwarmForge task) at a time**. Each step: move code → re-export from `core` if needed → unit + acceptance green → hand off.

**Detailed per-PR plans:** [`plans/README.md`](plans/README.md) (hand off one task at a time; start with `wave-start-in-core`).

### PR 0 — Document (this ADR)

- Land this file.
- Optional one-paragraph pointer from design doc §3 to this ADR.

### PR 1 — Wave start owned by core (P0)

**Goal:** Hosts never decide when attack 1 starts.

1. Add pure helper, e.g. `wave-schedule/ensure-attack-started` or `core` private:

   ```clojure
   (when (and (playing? state)
              (sky-clear? state)
              (nil? (:wave-attack state))
              (not (wave-complete? state)))
     (activate-wave-schedule state))
   ```

2. Call it at end of `tick` playing path (and/or after banner→playing transition inside core if applicable).
3. Remove `ensure-wave-enemies` policy from `jvm/sketch.clj` and `browser/main.cljs` (or reduce to no-op / delete).
4. Keep host calls to `activate-wave-schedule` only if QA scenarios need force-spawn; prefer scenario staging via core test API.

**Done when:** continuous play works with hosts calling only `handle`/`tick` after start; unit + wave QA pass.

### PR 2 — Event / SFX contract (P1)

**Goal:** One documented side-channel for hosts.

**Preferred option A — per-step events:**

- `handle`/`tick` return new SFX in `:events` (or dedicated `:sfx`).
- Hosts play returned list; state need not accumulate forever.

**Option B — drainable log:**

- Keep `:sfx-events` cumulative.
- Add `sfx/drain` or `core/take-sfx` returning `[events state-without-played]`.
- Document that `handle`/`tick` `:events` is unused/legacy and remove empty wrappers later.

Update both hosts + acceptance SFX steps.

**Done when:** hosts use one API; long sessions do not grow unbounded logs (B) or events are ephemeral (A).

### PR 3 — Extract `bonus-cities` (P1)

**Move from core:**

- `sync-bonus-cities-from-score` (award only)
- `apply-bonus-cities-from-reserve` (place + banner flag)
- threshold helpers / reserve accessors as pure API

**Call sites:**

- `add-score` / `set-score` → award only
- `wave-lifecycle` complete path → place (already injects apply-bonus-fn; can call module directly)

**Done when:** core re-exports thin wrappers; Gherkin bonus-cities + the-end still pass.

### PR 4 — Extract `combat` (P1)

**Move from core** (largest chunk):

- `tick-enemy-missiles`, `tick-flyers`, `tick-defensive-missiles`, `tick-fireballs`
- fireball contact, impact, MIRV split, smart evade, flyer drops
- related private helpers

**Keep on core temporarily:** public spawn helpers used by acceptance; or move spawns to `combat` and re-export.

**Done when:** `tick` playing path is a short pipeline calling `combat/tick-…` + wave advance/complete + game-over.

### PR 5 — Extract `shell` (P2)

**Move:**

- `start-game`, pause/resume, options open/leave, high-score open/close/submit
- `confirm-end-screen`, `enter-the-end` (or leave enter next to game-end)
- title warning SFX coordination if still in core

**Done when:** `handle` command map dispatches to `shell/*` and `core/tick` shell branches are thin.

### PR 6 — `core.testing` / API trim (P2)

1. New ns `missile-command.testing` with route/spawn/staging helpers.
2. Point unit specs and property tests at testing ns.
3. Acceptance: keep step text; step handlers may require testing ns **or** keep a minimal set on core for stability.
4. Delete unused re-exports after grep.

**Done when:** host-facing public API fits on one screen of names; tests still green.

### PR 7 — Host cleanup (P2)

1. Split `jvm/input.clj` into:
   - command mapping (keys/mouse → core commands)
   - QA/CLI parse + telemetry formatters
   - scenario application
2. Shared pure layout constants for banner subtitle / HUD band (no Quil).
3. Mirror key-routing style between JVM and browser where cheap.

### PR 8 — Seedable RNG (P2)

1. Optional state key e.g. `:rng` (fn or seed protocol).
2. `random-sky-origin-x` / wave spawn use state RNG when present.
3. QA scenarios may set seed for deterministic salvos.

### PR 9 — Docs + invariants (P3)

1. Update design doc §3 to point here; list real modules.
2. Property tests for invariants:
   - living cities ≤ 6
   - place only on wave resolution
   - THE END ⇔ living = 0 ∧ reserve = 0
   - `wave-attack` ∈ `{nil} ∪ {1..attacks-per-wave}`
3. Thin multi-tick acceptance steps opportunistically as features touch them.

## Out of scope / non-goals

- ECS or engine rewrite
- Merging hosts into core
- Big-bang acceptance rewrite
- ROM-perfect arcade timing

## Verification per PR

For every extraction PR:

1. `bb script/arch_check.bb`
2. Unit specs (`bb test` or project equivalent)
3. Acceptance for touched features
4. Manual or QA script smoke: start game → one full wave → banner → next wave; browser + JVM if host changed

## SwarmForge task naming (suggested)

| PR | Task name |
|----|-----------|
| 1 | `wave-start-in-core` |
| 2 | `sfx-event-contract` |
| 3 | `extract-bonus-cities` |
| 4 | `extract-combat` |
| 5 | `extract-shell` |
| 6 | `core-testing-api` |
| 7 | `host-input-split` |
| 8 | `seedable-sky-rng` |
| 9 | `arch-docs-invariants` |

Specifier owns Gherkin/QA only when behavior changes. Structural moves without behavior change can start at coder with this ADR as the plan.

## Success metrics

- `core.cljc` under ~400–500 LOC (facade + wiring)
- Hosts contain **zero** wave-start policy
- One SFX/event API used by both hosts
- Arch check remains green
- No regression in continuous play or bonus-city wave-end placement

## References

- Design: `docs/superpowers/specs/2026-07-24-missile-command-design.md` §3
- Arch check: `script/arch_check.bb`
- Wave modules: `src/missile_command/wave_schedule.cljc`, `wave_lifecycle.cljc`, `waves.cljc`
- Hosts: `src/missile_command/jvm/sketch.clj`, `browser/main.cljs`

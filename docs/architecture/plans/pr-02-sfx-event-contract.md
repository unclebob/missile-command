# PR 2 — SFX / event contract

**Task:** `sfx-event-contract`  
**Priority:** P1  
**Depends on:** PR 1 (preferred; can parallel if careful)  
**Behavior change:** none audible; host plumbing only

## Goal

One documented way for hosts to learn which SFX to play each frame. Eliminate dual mental models (`:events` vs cumulative `:sfx-events`).

## Current state

- `sfx/emit` appends to `:sfx-events`.
- Hosts (`browser/main`, `jvm/sketch`) compare previous/current log lengths and play the tail.
- `handle`/`tick` often return `{:events []}` even when SFX fired.
- Log grows for the whole run (memory / QA noise).

## Decision for this PR (choose B unless product wants A)

### Option B (recommended, smaller change): drainable log

1. Add to `sfx.cljc`:

   ```clojure
   (defn take-new
     "Events from index `from` (inclusive). Returns vec."
     [state from]
     (subvec (events state) (min from (count (events state)))))

   (defn truncate-to
     "Keep only first `n` events (or clear if 0)."
     [state n]
     (assoc state :sfx-events (vec (take n (events state)))))

   ;; Or: drain all since last mark stored on state :sfx-cursor
   (defn drain
     "Return [new-events state'] clearing played events from log."
     [state]
     (let [ev (events state)]
       [ev (assoc state :sfx-events [])]))
   ```

2. Prefer **cursor** approach if acceptance relies on full log for “emitted?” checks:
   - Keep cumulative log for tests.
   - Hosts store `sfx-cursor` locally (already use prev count)—document that as the official contract.
   - Optionally cap log length in production hosts only.

3. Document in `sfx.cljc` ns docstring:

   - Source of truth: `:sfx-events`
   - Hosts: play `events[prev-count …]` then set prev-count
   - `:events` on handle/tick is unused for SFX (or wire it later)

4. Wire `handle`/`tick` return value optionally:
   - Either stop pretending `:events` is used, or populate `:events` with **new** SFX only this step and keep log for tests.

### Option A (larger): per-step events only

1. `emit` collects into a step-local atom/vector, not state log.
2. `handle`/`tick` return those events; state has no log (or log only under test flag).
3. Update all `sfx-emitted?` acceptance/unit tests to inspect returned events or a test recorder.

Prefer **B + document** for this PR; schedule A only if product wants pure ephemeral events.

## Files

| File | Change |
|------|--------|
| `sfx.cljc` | drain/take-new helpers + docstring |
| `core.cljc` | optional: return new sfx in `:events` each tick/handle |
| `browser/main.cljs`, `jvm/sketch.clj` | use documented API; optional drain |
| `browser/audio.cljs`, `jvm/audio.clj` | no rule changes |
| `sfx_spec.clj`, acceptance sfx steps | adjust if drain clears log |
| `docs/architecture/...` or sfx ns | contract text |

## Tests

- Unit: emit three events; take-new from 1 returns two; drain clears.
- Existing: bonus-city / launch / boom emit still detectable.
- Host: no regression in title warning / launch sounds (manual or QA sound-events).

## Out of scope

- New sound assets  
- Wave schedule  

## Verification

- [ ] Arch check  
- [ ] `sfx_spec`, sound-events acceptance  
- [ ] Title warning + fire launch still play JVM + browser  

## Done when

Ns docstring defines the contract; both hosts use it; tests green; long play does not OOM from unbounded growth **or** growth is documented as test-only with host-side cursor.

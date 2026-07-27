# Global Score Pure Client Seam Plan

**Task:** `global-score-pure-client-seam`  
**Behavior change:** none intended for gameplay or displayed high-score content  
**Goal:** make global-score behavior testable without a real or mock HTTP server

## Problem

The desktop and browser global-score clients currently mix:

- game-state to leaderboard payload transformation
- player-name selection
- submit/read state transitions
- asynchronous execution
- HTTP transport

That makes tests choose between touching Cloudflare, starting a local mock HTTP
server, or avoiding the submit/read client behavior entirely. We want tests to
short-circuit the network at the function-call level.

## Required Result

Split the pure global-score logic from the host transport code. Tests,
acceptance/Gherkin steps, and QA procedures must be able to exercise global
score read/submit behavior with injected functions and no HTTP server lifecycle.

## Implementation Plan

### 1. Extract Pure Shared Functions

Add pure functions to `missile-command.global-scores` or a small adjacent shared
namespace. Keep names concise and host-neutral.

Required functions:

- `leaderboard-ready-state`
  - Inputs: wire payload, url, configured name, now-ms.
  - Output: normalized global-score state ready to merge into `global-state`.
  - Must preserve existing `normalize-response` behavior.

- `player-create-payload`
  - Inputs: selected display name, configured leaderboard name.
  - Output: `{:display_name ...}` or equivalent host-neutral map.

- `select-player-display-name`
  - Inputs: current global state, submitted display name, initials.
  - Output: configured player name, else submitted display name, else initials,
    else configured leaderboard name where appropriate.

- `score-submit-payload`
  - Inputs: game state, player, initials, host string, run id, game version.
  - Output: the exact map currently sent to `/scores`.
  - Must include score, wave, duration, host, run id, player id/token, and
    normalized initials.

- `submit-status-from-response`
  - Inputs: Worker response map.
  - Output: `:accepted` when `:accepted` is true, else `:failed`.

Keep the existing public helpers (`normalize-entry`, `entry-label`, `attach`,
etc.) stable unless tests require a clearer name. Do not reintroduce public id
display on the high-score screen.

### 2. Thin the JVM Client

In `missile-command.jvm.global-scores`:

- Keep HTTP helpers private.
- Replace inline payload construction and response interpretation with the new
  pure functions.
- Ensure `fetch-leaderboard!` and `submit-score!` return the `future` they
  create when work is scheduled. Existing callers may ignore the return value,
  but tests can deref it.
- Do not change timeout behavior or skip rules:
  - disabled global scores still skip
  - submit still skips until a leaderboard read has succeeded
  - failures still mark state failed and do not affect gameplay

### 3. Thin the Browser Client

In `missile-command.browser.global-scores`:

- Replace inline payload construction and response interpretation with the new
  pure functions.
- Keep Promise-based async behavior for production.
- Return the Promise chain from `fetch-leaderboard!` and `submit-score!` when
  work is scheduled so browser tests can wait if needed.
- Preserve localStorage player persistence behavior.

### 4. Add Function-Level Test Seams

Add an optional function-level client/transport injection point for tests. Prefer
domain functions over HTTP-shaped mocks.

Acceptable shape:

```clojure
{:fetch-leaderboard (fn [global-state] ...)
 :ensure-player (fn [display-name] ...)
 :submit-score (fn [player payload] ...)}
```

or a similarly small explicit map. Do not require a local HTTP server.

Production defaults must use the real Cloudflare Worker transport. Test clients
must be usable from Clojure specs, acceptance/Gherkin steps, and QA scripts.

### 5. Unit Specs

Add or update unit specs to cover:

- leaderboard payload normalization through `leaderboard-ready-state`
- score payload construction for desktop and browser host strings
- selected player display name precedence
- accepted and failed submit response status mapping
- submit skipped until `:read-succeeded?` is true
- failed transport updates `:status` or `:submit-status` without throwing to
  gameplay callers

These specs should not perform HTTP calls.

### 6. Gherkin Acceptance Coverage

If global-score behavior gets Gherkin coverage, the Gherkin step implementation
must use the function-level mock seam. It must not:

- call the deployed Cloudflare Worker
- start a mock HTTP server
- depend on network availability

Acceptance scenarios should verify externally visible behavior only:

- high-score view remains local until a mocked leaderboard read succeeds
- after a mocked successful read, local/global pages rotate
- a qualifying submitted score invokes the mocked submit path
- a mocked slow/failing service does not block local high-score behavior

Keep Gherkin parameters minimal and deterministic.

### 7. QA Procedure And Script Updates

Update relevant QA procedures and scripts so the global-score path uses the
function-level mock seam.

Required QA expectations:

- QA must not touch the deployed Cloudflare Worker.
- QA must not start or tear down a mock HTTP server.
- QA should document how the mock function is selected, for example a
  test-only launch option, environment variable, or QA event/scenario key.
- QA should verify:
  - local high-score behavior still works when the mocked network is failed
  - mocked leaderboard read enables the global page
  - mocked submit records the expected score payload or observable submit status

Do not restore manual look-and-feel QA sections.

## Verification

Coder should run:

- unit specs
- acceptance tests touched by this change
- relevant QA script(s) that were updated

No test should require the real Cloudflare Worker or a local mock HTTP server.

## Notes

The Cloudflare Worker itself can keep its current HTTP-level tests. This plan is
about game client testability, not replacing Worker validation/rate-limit tests.

# PR 5 — Extract `shell` module

**Task:** `extract-shell`  
**Priority:** P2  
**Depends on:** PR 4 preferred (smaller core)  
**Behavior change:** none

## Goal

Screen transitions (title, pause, options, high scores, THE END entry/confirm) live in `missile-command.shell`, not mixed with combat.

## Move from core

| Area | Functions |
|------|-----------|
| Start | `start-game` |
| Pause | `pause-game`, `resume-game` |
| Options | `open-options`, `leave-options`, `set-mute`, `set-difficulty`, `bind-fire-key` (or keep options ns) |
| High scores | `open-high-scores`, `close-high-scores`, `submit-high-score-initials`, `confirm-end-screen` path |
| End | `enter-the-end` (coord with `game-end`), end-fireball tick may stay combat/game-end |
| Title SFX | title warning emit coordination |

`handle` command map becomes thin dispatches to `shell/*`.

## Tick branches

```clojure
(cond
  (playing? state)  (playing-pipeline ...)
  (wave-banner? state) ...
  (the-end? state)  (shell/tick-end or game-end tick)
  (paused? state)   ...
  :else             (shell/tick-idle or advance-clock only))
```

## Files

| File | Action |
|------|--------|
| `shell.cljc` | **create** |
| `core.cljc` | command-handlers → shell; re-export |
| `high_scores.cljc`, `options.cljc`, `game_end.cljc` | already separate; shell orchestrates |
| title/pause/high-score acceptance | still via core re-exports |

## Tests

- title-screen, pause, options, high-scores, the-end features  
- unit: shell transitions  

## Verification

- [ ] Arch check  
- [ ] Shell feature acceptance  
- [ ] Browser + JVM: open options, high scores, pause, THE END confirm  

## Done when

No large shell transition bodies in core; `handle` is a dispatch table.

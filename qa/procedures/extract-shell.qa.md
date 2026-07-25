# QA: Extract shell module

**Task:** `extract-shell`  
**Suite:** title / pause / options / high-scores  
**Gherkin:** `features/title-screen.feature`, `pause.feature`, `options.feature`, `high-scores.feature`  
**Plan:** `docs/architecture/plans/pr-05-extract-shell.md`

Verify screen shell transitions live in `missile-command.shell` with core re-exports. Player-visible pause/title/options/high-score behavior unchanged.

## Rules

| Item | Detail |
|------|--------|
| Module | pause/resume, start-game, settings export/import, end confirm orchestration |
| Core | Re-exports shell API; no large transition bodies |
| Behavior | Unchanged shell navigation |

## Procedure

### A. Automated — unit + acceptance (title, pause, options, high-scores) + arch + property.

### B. Static — shell owns pause-game/resume-game/start-game; core re-exports.

### C. Host — start from title → playing; pause key path if telemetry exposes screen=paused.

### D. Look-and-feel — deferred until further notice.

## Pass criteria

- Automated green; host C holds; L&F skipped.

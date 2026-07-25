# PR 7 — Host input / QA split

**Task:** `host-input-split`  
**Priority:** P2  
**Depends on:** PR 1 (hosts simpler after ensure removal)  
**Behavior change:** none

## Goal

Separate production input mapping from QA/CLI/telemetry so `jvm/input.clj` is maintainable. Align browser key routing where cheap.

## JVM split (suggested ns)

| Namespace | Contents |
|-----------|----------|
| `jvm.input` | keys/mouse → core commands; pause/options/high-score routing used in live play |
| `jvm.qa` or `jvm.cli` | `--qa*` arg parse, scenario EDN load/apply, telemetry formatters |
| keep `scores_store` / `persist` | as today |

Alternatively packages:

- `missile-command.jvm.input.commands`
- `missile-command.jvm.input.telemetry`
- `missile-command.jvm.input.scenario`

## Browser

- Keep thin; extract shell key helpers only if `main.cljs` grows again.
- Share pure layout constants with JVM via new `missile-command.layout` or `ui-metrics.cljc` (banner subtitle offset, HUD height) — no Quil.

## Steps

1. Identify pure functions in `jvm/input.clj` (format-telemetry-line, parse-args, apply-scenario).  
2. Move without behavior change.  
3. `sketch.clj` requires new ns for QA paths only.  
4. Specs under `spec/missile_command/jvm/` updated.

## Verification

- [ ] `bb play` still works  
- [ ] `bb play --qa` + scenario EDN still works  
- [ ] desktop-host / browser-host acceptance  
- [ ] telemetry fields used by QA scripts unchanged  

## Done when

Live-play input file has no large QA scenario engine; telemetry/CLI in dedicated ns.

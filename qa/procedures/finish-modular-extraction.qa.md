# QA: Finish modular extraction

**Task:** `finish-modular-extraction`  
**Suite:** full combat + host split + testing facade  
**Plans:** PR 4 combat (full), PR 6 testing, PR 7 host-input-split  
**Gherkin:** defensive, enemy impacts, mirv, smart-bombs, bombers-and-satellites, waves

Verify the modular-core extraction is complete:

1. **Combat** owns defensive + enemies + MIRV/smart + flyers; core playing tick uses `combat/tick-playing-combat`.
2. **Testing** holds route/static staging helpers; hosts do not require `missile-command.testing`.
3. **JVM input split** — `cli` / `telemetry` / `scenario` modules exist; `jvm.input` re-exports; sketch QA path still works.
4. **Docs** — plan index marks PR 4 and PR 7 **Done**.

## Procedure

### A. Automated — unit + acceptance + arch-check + property.

### B. Static — combat has `tick-enemies`, `tick-flyers`, `tick-playing-combat`; core has no private enemy tick bodies; jvm modules present; testing ns documented non-host.

### C. Host — scenario playing + fire → attack 1 and `missiles_in_flight` path live.

### D. Look-and-feel — deferred until further notice.

## Pass criteria

- Suite green; B–C hold; L&F skipped.

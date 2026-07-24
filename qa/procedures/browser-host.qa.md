# QA: Browser host

**Task:** `browser-host`  
**Suite:** browser-host  
**Gherkin:** `features/browser-host.feature`

Verify the **ClojureScript Quil browser host** plays the same game as desktop: documented build/open, full canvas resolution + resize reflow, mouse aim, key fire, click zones, title/start, pause, HUD, THE END, options, high scores, SFX, **`localStorage`** persistence, shared pure core, draw order §7.2.

Depends on US-21 desktop host (shared shell behaviors). Out of scope: mobile touch-first redesign, full PWA packaging.

## Launch (document in README)

Exact commands are project-defined; examples:

```sh
bb browser          # or shadow-cljs watch + serve
# open the documented page (e.g. index.html / localhost URL)
```

QA may use documented browser QA flags or URL params only if they are published UI affordances.

## UI Event Boundary

- Real browser UI: keyboard, mouse, canvas.
- Persistence via **`localStorage`** (not JVM files).
- No private core API for play checks.
- Look-and-feel: **request user approval** (parity with desktop style).

## Procedure

### A. Automated

1. Unit + acceptance including `browser-host` + arch-check — success.
2. Confirm shared core acceptance suite still green (rules parity).

### B. Build and open

3. Run documented browser build/serve.
4. Open game in a desktop browser; canvas fills window / uses full resolution.

### C. Core loop

5. Title → start → play (aim, key fire, click zones).
6. Pause / resume; HUD visible.
7. Options and high scores reachable; SFX when unmuted.
8. Reach THE END (or stage via documented QA); confirm flow toward title / high-score entry.

### D. Resize

9. Resize browser window; layout reflows; not a stretched fixed buffer.

### E. localStorage

10. Set mute + difficulty (and a high score if possible).
11. Reload page; options/scores restored from **localStorage**.

### F. Parity

12. Spot-check same rules as desktop (wave, intercept, score) via shared core tests + short play.

### G. Look-and-feel

13. Full visual/audio pass in browser.
14. **Request user approval**.

## Pass criteria

- Acceptance for `browser-host` passes.
- Documented build/open works; full loop + resize + localStorage + core isolation.
- User approves browser presentation.

# QA: Browser host

**Task:** `browser-host`  
**Suite:** browser-host  
**Gherkin:** `features/browser-host.feature`

Verify the **ClojureScript Quil browser host** plays the same game as desktop: documented build/open, full canvas resolution + resize reflow, mouse/touch aim, key fire, click zones, title/start, pause, HUD, THE END, options on desktop/tablet, high scores, SFX, **`localStorage`** persistence, shared pure core, draw order §7.2.

Depends on US-21 desktop host (shared shell behaviors). Out of scope: full PWA packaging.

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
7. Options and high scores reachable on desktop/tablet browser; their **Title** buttons return to the title screen; SFX when unmuted. The normal mouse cursor is visible on every non-play screen, and the game crosshair replaces it only while playing.
8. Reach THE END (or stage via documented QA); qualifying scores open a browser player-name prompt, then return to title after submission.

### D. Resize

9. Resize browser window; layout reflows inside the largest 4:3 playfield that fits the viewport; not a stretched fixed buffer.

### E. localStorage

10. Set mute + difficulty (and a high score if possible).
11. Reload page; options/scores restored from **localStorage**.

### F. Parity

12. Spot-check same rules as desktop (wave, intercept, score) via shared core tests + short play.

### G. Phone

13. Open in a phone-sized/mobile browser. Assert the title shows a tappable **High Scores** button, Options is not shown or reachable, and a qualifying score opens the browser name prompt so the phone keyboard appears.

## Pass criteria

- Acceptance for `browser-host` passes.
- Documented build/open works; full loop + resize + localStorage + phone title controls + core isolation.

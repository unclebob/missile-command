# QA: Browser shell screens

**Task:** `browser-shell-screens`  
**Suite:** browser-host  
**Gherkin:** `features/browser-host.feature`

Verify the **ClojureScript browser host** draws the same shell screens as desktop: **title**, **options**, **high-scores** / entry, **pause**, **THE END**, **wave-banner**, with documented controls and localStorage-backed options/scores.

## Preconditions

- Desktop shells already approved (title/options/high-scores/pause/end/banner).
- Browser modules: `browser/render_shells.cljs`, `browser/main.cljs`, `bb browser`, `index.html`.

## Procedure

### A. Automated — unit + acceptance (`browser-host`) + arch-check.

### B. Structure — shell overlay functions exist for title, high-score entry/table, options, pause, end, wave-banner.

### C. Docs — README documents `bb browser` and `index.html`.

### D. Build — `bb browser` produces usable open path (or documents compile skip gracefully).

### E. Look-and-feel — request approval for browser shell parity (or desktop shell screens as proxy when browser assets unavailable).

## Pass criteria

- Acceptance for browser-host green.
- Shell modules cover all shell screens.
- User approves presentation / parity.

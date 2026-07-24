# QA: Desktop host

**Task:** `desktop-host`  
**Suite:** desktop-host  
**Gherkin:** `features/desktop-host.feature`

Verify the **JVM Quil desktop host** is a complete playable app: documented **`bb play`**, full native resolution + resize reflow (no fixed low-res buffer), mouse aim, key fire, click-zone fire, title/start, pause, HUD, THE END, options, high scores, SFX, file persistence, draw order per design §7.2, and pure core isolation.

Depends on US-16, US-17, US-20 (and prior stories). Out of scope: browser host (US-22), cabinet bezel.

## Launch

```sh
bb play              # default size (e.g. 800×600)
bb play 1280 720     # explicit size
bb play --qa …       # QA telemetry / scenario / events
```

Window: resizable; opens on launch screen; should not steal keyboard focus (existing host behavior). OS cursor hidden; game crosshair shown.

## UI Event Boundary

- Real desktop UI via documented CLI and window input.
- QA flags only as published UI affordances (`--qa`, scenario, events, speed).
- No private core API instead of the host.
- Look-and-feel: **request user approval** for full visual + audio package.

## Draw order (design §7.2)

1. Sky  
2. Enemy trails / missiles  
3. Flyers  
4. Defensive missiles  
5. Fireballs  
6. Ground, cities, batteries  
7. Crosshair  
8. HUD and overlays (title, pause, THE END, menus)

## Persistence

- High scores and options in a file under user config or project-local path (README).
- Survive quit and relaunch on the same machine.

## Procedure

### A. Automated

1. Unit tests, acceptance including `desktop-host`, arch-check — success.

### B. Launch and shell

2. `bb play` from project root; window opens at documented size.
3. Title shows; start → playing; HUD visible.
4. Pause / resume; options from title; high scores from title.

### C. Input and combat

5. Mouse aim clamps to playfield.
6. Z/X/C (or 1/2/3) fire left/center/right.
7. Click left/center/right thirds fire correct battery (with fallback rules).
8. Enemies, intercepts, waves playable.

### D. Resize

9. Resize window larger/smaller; layout reflows (cities/batteries reposition); not a stretched 800×600 buffer.

### E. THE END and meta

10. Lose all cities with no reserve → THE END presentation.
11. Qualifying score → initials if high-scores implemented; else title.
12. SFX audible when unmuted; silent when muted.

### F. Persist

13. Set mute + difficulty; add a high score if possible; quit.
14. Relaunch; options and scores restored.

### G. Architecture

15. `bb arch-check` — pure core free of Quil/IO.

### H. Look-and-feel

16. Full session visual pass (draw order, vector style, HUD, THE END fireball).
17. **Request user approval**.

## Pass criteria

- Acceptance for `desktop-host` passes.
- Documented launch works; full play loop + resize + persist + SFX + arch isolation.
- User approves desktop presentation and audio.

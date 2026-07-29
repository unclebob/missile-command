# QA: Title screen

**Task:** `title-screen`  
**Suite:** title-screen  
**Gherkin:** `features/title-screen.feature`

Verify launch begins on the **title** screen showing the game name **Missile Command**, a **start** affordance, and title buttons; start enters **playing** with a fresh six-city / three-battery run at current dimensions, score 0 and wave 1; fire does nothing on title; confirming **THE END** (when no high-score entry applies) returns to title.

Out of scope: options menu contents (US-19), high-score table contents (US-18), pause (US-16), attract-mode polish (nice-to-have).

## Preconditions

- Host launch: `bb play` / `bb play --qa`.
- Start input: documented key and/or click/tap (e.g. Enter / primary click outside title buttons — match README).

## UI Event Boundary

- Scenario + events + telemetry only.
- Telemetry: `screen=title|playing|the_end`, title game name, start affordance flag if exposed, score, wave, cities/batteries after start.
- Events may include `start` / `confirm` / `key enter` as documented.

## Scenario / events

**Start from title:**
```text
start
```
or
```text
key enter
```
(as documented)

**THE END → title:**
```edn
{:cities {:destroyed [0 1 2 3 4 5]}
 :bonus-cities 0}
```
```text
wait 1
confirm
```
(or documented confirm key after THE END presentation)

## Procedure

### A. Automated — unit + acceptance (`title-screen`) + arch-check.

### B. Launch

1. `bb play --qa`. Assert `screen=title` (not playing immediately).
2. Visible: game name **Missile Command**, start prompt/affordance, and title buttons.

### C. Start

3. Trigger start. Assert `screen=playing`, 6 living cities, 3 batteries ammo 10, score 0, wave 1, dimensions match launch size.
4. Optional: stage dirty score/wave via scenario before start; after start still 0 / 1.

### D. Title ignore fire

5. On title, aim + fire keys/click. Assert no defensive missiles; still title.

### E. High Scores Button

6. Click/tap the **High Scores** button. Assert `screen=high-scores`.

### F. Mobile Options Boundary

7. In a phone-sized browser session, assert the **High Scores** button is present and Options is not visible or reachable.

### G. THE END return

8. Stage THE END; complete/confirm end screen without high-score path. Assert return to **title**.

## Pass criteria

- Acceptance for `title-screen` passes.
- Launch → title; start → fresh playing; high-score button opens the table; phone hides Options; THE END confirm → title.

# QA: High scores

**Task:** `high-scores`  
**Suite:** high-scores  
**Gherkin:** `features/high-scores.feature`

Verify top-**N** high scores (default **10**): player name/initials + score + date/time; after **THE END**, qualifying scores open the host player-name prompt from **high-score-entry**; non-qualifying scores go to **title**; submit inserts in ranked order; table capped at N; view table from **title**; the **Title** button and `H` return from the table; desktop `Esc` quits instead of closing the table; host **persists** scores across relaunch.

Depends on US-14 THE END and US-15 title. Out of scope: online leaderboards.

## Rules summary

| Rule | Detail |
|------|--------|
| Capacity | Default 10 (parameterized) |
| Qualify | Positive score beats the lowest table entry, or table not full |
| Name / initials | Host asks for a player name; stored initials are the first 3 normalized A–Z/digits |
| Order | Descending by score |
| Cap | Insert then drop lowest beyond N |
| Flow | THE END → (entry if qualify) → title; title can open high-scores view |
| Table return | **Title** button and `H` return to title; desktop `Esc` is reserved for host quit |
| Persist | JVM file (e.g. `~/.missile-command/scores.edn` or project path); browser `localStorage` |

## Preconditions

- THE END + title/start available.
- Documented paths: confirm end, host player-name prompt or scripted `initials` event, open high scores from title button/shortcut, persistence location in README.

## UI Event Boundary

- Core: qualify, insert, order, cap, screen transitions via scenario/events.
- Host: load/save file or localStorage; QA relaunch to verify persistence.
- Telemetry: `screen=`, pending score, table ranks if exposed.

## Scenario staging

**Seed table (capacity 3):** via QA scenario keys once documented, e.g.
```edn
{:high-scores [{:initials "AAA" :score 1000}
               {:initials "BBB" :score 900}
               {:initials "CCC" :score 800}]
 :high-score-capacity 3
 :score 700
 :cities {:destroyed [0 1 2 3 4 5]}
 :bonus-cities 0}
```

**Events (illustrative):**
```text
confirm
# if entry in scripted QA:
initials BOB
```

## Procedure

### A. Automated — unit + acceptance (`high-scores`) + arch-check.

### B. Non-qualify — full table, score below lowest → confirm THE END → **title**, no entry.

### C. Qualify — empty or beat lowest → confirm/end sequence completion → **high-score-entry** with pending score, then host prompts for the player's name outside QA.

### D. Insert — enter a player name through the host prompt, or use scripted `initials` in automated QA; table ordered; rank correct; return **title**.

### E. Cap — full table, mid score; length stays N; old lowest dropped.

### F. Name / initials — full display name is retained, stored initials are normalized uppercase length 3.

### G. View — from title open high scores with the button or `H`; ranks visible; **Title** button and `H` close the table.

### H. Escape boundary — Escape while viewing high scores is not consumed as a return-to-title action; on desktop it quits the host.

### I. Persist — write a score; quit; relaunch; table still contains it (host file/localStorage).

## Pass criteria

- Acceptance for `high-scores` passes.
- Qualify/entry/order/cap/view/Escape boundary match Gherkin.
- Persistence verified on host.

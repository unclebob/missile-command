# QA: High scores

**Task:** `high-scores`  
**Suite:** high-scores  
**Gherkin:** `features/high-scores.feature`

Verify top-**N** high scores (default **10**): **3-character initials** + score; after **THE END**, qualifying scores open **high-score-entry**; non-qualifying go to **title**; submit inserts in ranked order; table capped at N; view table from **title**; host **persists** scores across relaunch.

Depends on US-14 THE END and US-15 title. Out of scope: online leaderboards.

## Rules summary

| Rule | Detail |
|------|--------|
| Capacity | Default 10 (parameterized) |
| Qualify | Score beats the lowest table entry, or table not full (empty table: any score ≥ 0 qualifies) |
| Initials | Exactly 3 chars; A–Z and digits; lowercase normalized to uppercase |
| Order | Descending by score |
| Cap | Insert then drop lowest beyond N |
| Flow | THE END → (entry if qualify) → title; title can open high-scores view |
| Persist | JVM file (e.g. `~/.missile-command/scores.edn` or project path); browser `localStorage` |

## Preconditions

- THE END + title/start available.
- Documented paths: confirm end, enter initials, open high scores from title, persistence location in README.

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
# if entry:
initials BOB
# or key strokes A B C enter
```

## Procedure

### A. Automated — unit + acceptance (`high-scores`) + arch-check.

### B. Non-qualify — full table, score below lowest → confirm THE END → **title**, no entry.

### C. Qualify — empty or beat lowest → confirm → **high-score-entry** with pending score.

### D. Insert — enter initials; table ordered; rank correct; return **title**.

### E. Cap — full table, mid score; length stays N; old lowest dropped.

### F. Initials — lowercase becomes uppercase; length 3.

### G. View — from title open high scores; ranks visible.

### H. Persist — write a score; quit; relaunch; table still contains it (host file/localStorage).

## Pass criteria

- Acceptance for `high-scores` passes.
- Qualify/entry/order/cap/view match Gherkin.
- Persistence verified on host.

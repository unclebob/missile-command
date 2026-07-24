# US-18 — High-score table

**Status:** in-specifier  
**Depends on:** US-14, US-15  
**Design:** §5.1, §6.4

## Story

**As a** player,  
**I want** to enter my initials when my score ranks and see a top-score table,  
**So that** I can track the best runs on this machine.

## In scope

- Top N scores (default 10): initials (3 characters) + score.
- After THE END, if score qualifies, enter high-score-entry; otherwise return toward title.
- Persist scores: file on desktop host, `localStorage` on browser host (host adapters; core validates insert/order).
- View high scores from title (or documented navigation).

## Acceptance criteria

- A qualifying score opens initials entry; a non-qualifying score does not.
- After entry, the table contains the new score in correct order.
- Table length is capped at N; lowest is dropped when full.
- Reloading the host still shows persisted scores (host-level acceptance / QA).
- Initials length and allowed character rules are deterministic and documented.

## Out of scope

- Online leaderboards.

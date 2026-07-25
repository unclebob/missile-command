# QA: THE END

**Task:** `the-end`  
**Suite:** the-end  
**Gherkin:** `features/the-end.feature`

Verify the run ends with **THE END** when there are **zero living cities** and **no usable bonus-city reserve**; reserve or remaining cities prevent THE END; final score stays available; fire does not launch after THE END.

## Presentation (required look)

Centered on the playfield:

1. A **screen-filling fireball** expands, then contracts (same expand/contract idea as combat fireballs, but scaled to fill the playfield at max).
2. In the **middle** of that fireball are the words **THE END**.
3. At max expanse, the lettering **fills the final fireball disk** (glyph bounds match the max blast size).
4. Letters are **gradually exposed** as the fireball grows (revealed with radius).
5. Letters are **not visible outside** the fireball (clip / stencil to the disk only).

Not “Game Over”. Copy is exactly **THE END**.

Depends on **US-10**. Out of scope: high-score initials (US-18), title return (US-15).

## Rules summary

| Condition | Result |
|-----------|--------|
| Living cities ≥ 1 | Not THE END |
| Living = 0 and reserve ≥ 1 | Not THE END; cities stay down until wave-end restore (reserve is not placed mid-wave) |
| Living = 0 and reserve = 0 | Enter THE END + presentation sequence |
| After THE END | No defensive launches; score available |

## Preconditions

- US-10 reserve/restore; staged destroyed cities.
- `bb play --qa` with scenario/events; host draws THE END sequence.

## UI Event Boundary

- Scenario + events + telemetry only.
- Telemetry: THE END state, end message, end-fireball center/radius/phase, score, living, reserve.
- Presentation clipping may be host-visible; core should expose fireball radius and message clip radius for acceptance.
- **Look-and-feel approval required** for the fireball + letter reveal.

## Scenario staging

```edn
{:cities {:destroyed [0 1 2 3 4 5]}
 :bonus-cities 0
 :score 2500}
```

Reserve save:
```edn
{:cities {:destroyed [0 1 2 3 4 5]}
 :bonus-cities 2}
```

## Procedure

### A. Automated — unit + acceptance (`the-end`) + arch-check.

### B–F. Logic — not ended / THE END / reserve save / score / no fire (as Gherkin).

### G. Presentation (host)

7. Trigger THE END. Assert fireball **centered** on playfield.
8. During expand: radius grows; **THE END** only appears **inside** the disk; more letters/more of the type block become visible as radius increases.
9. At max: disk **fills** the playfield; **THE END** lettering fills that max expanse.
10. During contract: fireball shrinks; letters remain clipped to the disk (never drawn outside).
11. Confirm message is **THE END**, never “Game Over”.

### H. Look-and-feel — **request user approval** for expand/reveal/contract drama.

## Pass criteria

- Acceptance including presentation scenarios passes.
- Logic: zero living + zero reserve ↔ THE END; no post-end fire.
- Visual: centered screen-fill fireball; THE END fills max blast; gradual reveal; no letters outside fireball.
- User approves presentation.

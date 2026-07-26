# QA: Core testing API + seedable sky RNG

**Task:** `core-testing-api`  
**Suite:** waves-and-rearm / enemy angles (sky origins)  
**Gherkin:** `features/waves-and-rearm.feature`  
**Plans:** `docs/architecture/plans/pr-06-core-testing-api.md`, `pr-08-seedable-sky-rng.md`

Verify:

1. **`missile-command.testing`** is the staging facade for route/setup helpers; production hosts do not require it.
2. **Seedable RNG** (`rng.cljc` + scenario `:rng-seed`) makes sky origins deterministic when set; unseeded play still spawns in-range origins.

## Rules

| Item | Detail |
|------|--------|
| Testing ns | Documented non-host; re-exports route-*, add-static-fireball, with-rng-seed, etc. |
| Hosts | Only `handle`/`tick` (+ start/aim/fire); may apply scenario `:rng-seed` via UI/QA scenario loader |
| Same seed | Identical `enemy_origin_x` for a 3-missile attack 1 salvo |
| Different seeds | Origin vectors differ (almost always) |

## Procedure

### A. Automated — unit + accept + arch-check + property.

### B. Static — `testing.cljc` and `rng.cljc` exist; JVM/browser hosts do not require `missile-command.testing`.

### C. Host deterministic — scenario `:rng-seed 42` + playing attack 1 twice → same three origin xs.

### D. Host different seed — seed 42 vs 99 → origin vectors not equal.

## Pass criteria

- Automated green; B–D hold.

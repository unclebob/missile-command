# QA: Seedable sky RNG

**Task:** `seedable-sky-rng`  
**Suite:** waves-and-rearm / random sky origins  
**Plan:** `docs/architecture/plans/pr-08-seedable-sky-rng.md`

Verify optional `:rng-seed` makes attack-1 sky origins deterministic; unseeded play still uses random in-range origins.

## Procedure

### A. Automated — unit + accept + arch + property (rng properties).

### B. Static — `rng.cljc` + scenario loader `:rng-seed` / `with-rng-seed`.

### C. Host — same seed twice → identical origin xs; seed 42 ≠ seed 99.

## Pass criteria

- Suite green; host C holds.

# US-01 — Project foundation and acceptance harness

**Status:** backlog  
**Depends on:** —  
**Design:** §3, §9, §10

## Story

**As a** developer working with the six-pack,  
**I want** a runnable Clojure project with a shared pure-core layout and a working acceptance-test pipeline,  
**So that** later stories can specify and implement game behavior with TDD and Gherkin.

## In scope

- `deps.edn` (and CLJS tooling hooks as needed for later hosts).
- Source layout separating pure `.cljc` core from future JVM/browser hosts.
- Acceptance Pipeline Specification (APS) tools available as project commands (`gherkin-parser`, etc.).
- Project-specific acceptance entrypoint generator, runtime, and step-handler skeleton sufficient to run an empty or smoke feature.
- Unit-test runner wired for core tests.
- Ability to run tests from the project root with documented commands.

## Acceptance criteria

- A documented command runs unit tests successfully (even if only a smoke test).
- A documented command runs the normal acceptance pipeline (parser → generator → tests) successfully against at least one trivial approved feature or smoke scenario.
- Pure core code has a place to live that hosts are not required to load for core tests.
- No game rules beyond a minimal placeholder needed to prove the harness.

## Out of scope

- Playable game behavior, Quil windows, sound, high scores.

## Notes for specifier

- Keep Gherkin focused on “project can verify behavior,” not game rules.
- End-to-end QA may be “run documented test commands and observe pass,” since there is no game UI yet.

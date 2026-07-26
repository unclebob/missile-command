# Recommendation 2 — Shared host input policy

**Task:** `shared-host-input-policy`  
**Priority:** P1  
**Behavior change:** none intended; browser/JVM parity should improve

## Problem

The JVM and browser hosts independently translate shell keys, fire keys,
clicks, and initials editing into core commands. This duplicates behavior and
creates a risk that title/options/high-score/pause input works differently
between hosts.

## Goal

Create a shared pure `.cljc` policy for host input decisions. Hosts should
continue to read raw platform events, but shared code should decide which core
command or host-local draft update is intended.

## Plan

1. Compare JVM and browser key/click behavior for title, playing, paused,
   options, high scores, THE END, and high-score initials entry.
2. Define a small pure input result shape, for example:
   - `{:command {...}}`
   - `{:draft next-draft}`
   - `{:command {...} :draft next-draft}`
   - `nil` for ignored input
3. Add a shared namespace such as `missile-command.host-input` or
   `missile-command.shell-input`.
4. Move platform-neutral decisions into that namespace:
   - pause/resume key policy
   - options/high-score open/close keys
   - Enter confirm/start/submit behavior
   - initials append/backspace policy
   - fire-key command routing by key name
5. Keep platform-specific concerns in hosts:
   - raw key-code normalization
   - browser audio unlock/focus
   - JVM no-keyfocus callback gate
   - mouse coordinate acquisition
6. Update JVM and browser hosts to call the shared policy.

## Guardrails

- Preserve existing visible behavior.
- Keep browser audio unlock rules in the browser host.
- Keep JVM `--no-keyfocus` behavior in the JVM host.
- Do not make shared pure code require Quil, JS, JVM, audio, storage, or CLI.

## Verification

- Unit specs for shared input policy.
- Existing JVM input specs.
- `bb test`
- `bb accept`
- Representative QA scripts for title/options/high-scores/pause.

## Done When

- Shell key behavior is defined once in `.cljc`.
- JVM and browser hosts only normalize platform input and apply returned
  commands/draft changes.
- No feature file needs to distinguish host input semantics unless the host
  platform itself differs.

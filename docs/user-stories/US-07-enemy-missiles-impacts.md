# US-07 — Enemy missiles destroy cities and batteries

**Status:** in-implementation  
**Depends on:** US-06  
**Design:** §5.2, §5.4, §5.5

## Story

**As a** player,  
**I want** enemy ballistic missiles to fall toward cities and batteries and destroy what they hit,  
**So that** I must intercept them or lose defenses.

## In scope

- Enemy missiles travel from sky origins toward living cities or non-destroyed batteries.
- Impact destroys the targeted city or battery (single hit).
- Destroyed batteries cannot fire; destroyed cities are no longer living.
- Fireballs destroy enemy missiles that pass **within the fireball radius** (distance from enemy position to fireball center ≤ radius).
- Enemy missiles that never enter any fireball disk are not destroyed by those fireballs.
- At least a controllable way to spawn or script enemy missiles for tests (wave system may be minimal until US-08).

## Acceptance criteria

- An unintercepted enemy missile destroys its target city or battery.
- An enemy missile that enters a fireball’s radius is destroyed and does not destroy its target.
- An enemy missile that stays outside a fireball’s radius is not destroyed by that fireball.
- Destroying a battery prevents further fires from that battery.
- Multiple cities can be destroyed independently.

## Out of scope

- Full wave scheduling (US-08), MIRVs (US-11), smart bombs (US-12), flyers (US-13), scoring (US-09).

# QA: Bonus cities at wave end

**Task:** `bonus-cities-wave-end`  
**Suite:** bonus-cities  
**Gherkin:** `features/bonus-cities.feature` (scenarios 04–06 wave-end placement)

Verify **reserve is awarded immediately** when score crosses threshold, but **cities are restored only after wave resolution** (wave complete / banner path), never mid-wave; living cities cap at 6; extras stay in reserve.

## Procedure

### A. Automated — unit + acceptance (`bonus-cities`) + arch-check.

### B. Mid-wave — destroyed cities + score threshold → reserve > 0, living count **unchanged** while playing.

### C. Wave end — after last enemy of wave clears (banner / next wave), living cities increase and reserve drops.

### D. Cap — large reserve with few destroyed cities places at most 6 living; remainder stays reserve.

## Pass criteria

- Acceptance bonus-cities green; host B–D match.

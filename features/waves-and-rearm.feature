# Waves and rearm 01 new game starts at wave one
# Waves and rearm 02 wave does not complete while enemies remain
# Waves and rearm 03 wave completes when all enemies are gone
# Waves and rearm 04 surviving batteries rearm to full after wave
# Waves and rearm 05 destroyed batteries stay destroyed after rearm
# Waves and rearm 06 higher waves schedule more or faster enemies
# Waves and rearm 07 hud shows the current wave number
Feature: Waves and rearm

Background:
  Given a new game with width <width> and height <height>

Scenario: Waves and rearm 01 new game starts at wave one
  Then the wave number is <wave>
  And each non-destroyed battery has <ammo> missiles
  And the hud shows wave <wave>

Examples:
  | width | height | wave | ammo |
  | 800   | 600    | 1    | 10   |
  | 1920  | 1080   | 1    | 10   |

Scenario: Waves and rearm 02 wave does not complete while enemies remain
  Given the current wave has <remaining> scheduled enemies still active
  When time advances by <dt> seconds
  Then the wave number is <wave>
  And the wave is not complete

Examples:
  | width | height | remaining | dt  | wave |
  | 800   | 600    | 1         | 0.1 | 1    |
  | 800   | 600    | 3         | 0.1 | 1    |
  | 1920  | 1080   | 2         | 0.05| 1    |

Scenario: Waves and rearm 03 wave completes when all enemies are gone
  Given the current wave has <remaining> scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  Then the wave is complete
  And the wave number is <wave>
  And the hud shows wave <wave>

Examples:
  | width | height | remaining | wave |
  | 800   | 600    | 1         | 2    |
  | 800   | 600    | 2         | 2    |
  | 1920  | 1080   | 1         | 2    |

Scenario: Waves and rearm 07 hud shows the current wave number
  Then the wave number is <wave>
  And the hud shows wave <wave>

Examples:
  | width | height | wave |
  | 800   | 600    | 1    |
  | 1920  | 1080   | 1    |

Scenario: Waves and rearm 04 surviving batteries rearm to full after wave
  Given every non-destroyed battery has <spent_ammo> missiles
  And the current wave has <remaining> scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  And the next wave starts
  Then each non-destroyed battery has <ammo> missiles
  And the wave number is <wave>

Examples:
  | width | height | spent_ammo | remaining | ammo | wave |
  | 800   | 600    | 3          | 1         | 10   | 2    |
  | 800   | 600    | 7          | 1         | 10   | 2    |
  | 1920  | 1080   | 0          | 1         | 10   | 2    |

Scenario: Waves and rearm 05 destroyed batteries stay destroyed after rearm
  Given the <battery> battery has been destroyed
  And the current wave has <remaining> scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  And the next wave starts
  Then the <battery> battery is destroyed
  And each non-destroyed battery has <ammo> missiles
  When the player fires the <battery> battery
  Then there are <missile_count> defensive missiles in flight

Examples:
  | width | height | battery | remaining | ammo | missile_count |
  | 800   | 600    | left    | 1         | 10   | 0             |
  | 800   | 600    | center  | 1         | 10   | 0             |
  | 800   | 600    | right   | 1         | 10   | 0             |

Scenario: Waves and rearm 06 higher waves schedule more or faster enemies
  Given wave <low_wave> enemy schedule metrics are recorded
  When the game is at wave <high_wave>
  Then wave <high_wave> is harder than wave <low_wave> by enemy count or enemy speed

Examples:
  | width | height | low_wave | high_wave |
  | 800   | 600    | 1        | 2         |
  | 800   | 600    | 1        | 3         |
  | 1920  | 1080   | 2        | 4         |

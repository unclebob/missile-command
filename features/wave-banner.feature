# Wave banner 01 wave completion shows a banner announcing the next wave
# Wave banner 02 banner text moves in toward the center
# Wave banner 03 banner text moves out after arriving
# Wave banner 04 after the banner finishes play resumes on the announced wave
# Wave banner 05 enemies do not advance while the wave banner is showing
Feature: Wave banner

Background:
  Given a new game with width <width> and height <height>
  When the player starts the game

Scenario: Wave banner 01 wave completion shows a banner announcing the next wave
  And the current wave has 1 scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is wave-banner
  And the wave banner announces wave <announced_wave>
  And the wave banner text is <banner_text>

Examples:
  | width | height | announced_wave | banner_text | expected_width | expected_height |
  | 800   | 600    | 2              | WAVE 2      | 800            | 600             |
  | 1024  | 768    | 2              | WAVE 2      | 1024           | 768             |
  | 1920  | 1080   | 2              | WAVE 2      | 1920           | 1080            |

Scenario: Wave banner 02 banner text moves in toward the center
  And the current wave has 1 scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  Then the screen is wave-banner
  And the wave banner phase is enter
  When time advances by <dt> seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave banner phase is enter
  And the wave banner text has moved closer to the playfield center
  And the wave banner text is not fully off screen

Examples:
  | width | height | dt   | expected_width | expected_height |
  | 800   | 600    | 0.1  | 800            | 600             |
  | 800   | 600    | 0.2  | 800            | 600             |
  | 1920  | 1080   | 0.1  | 1920           | 1080            |

Scenario: Wave banner 03 banner text moves out after arriving
  And the current wave has 1 scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  And time advances until the wave banner text reaches the playfield center
  Then the wave banner phase is exit
  When time advances by <dt> seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave banner phase is exit
  And the wave banner text has moved farther from the playfield center

Examples:
  | width | height | dt   | expected_width | expected_height |
  | 800   | 600    | 0.1  | 800            | 600             |
  | 800   | 600    | 0.2  | 800            | 600             |
  | 1920  | 1080   | 0.1  | 1920           | 1080            |

Scenario: Wave banner 04 after the banner finishes play resumes on the announced wave
  And the current wave has 1 scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  And time advances until the wave banner finishes
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And the wave number is <wave>
  And the hud shows wave <wave>
  And each non-destroyed battery has <ammo> missiles

Examples:
  | width | height | wave | ammo | expected_width | expected_height |
  | 800   | 600    | 2    | 10   | 800            | 600             |
  | 1920  | 1080   | 2    | 10   | 1920           | 1080            |
  | 1024  | 768    | 2    | 10   | 1024           | 768             |

Scenario: Wave banner 05 enemies do not advance while the wave banner is showing
  And the current wave has 1 scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  Then the screen is wave-banner
  And there are 0 enemy missiles in flight
  When time advances by <dt> seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is wave-banner
  And there are 0 enemy missiles in flight

Examples:
  | width | height | dt  | expected_width | expected_height |
  | 800   | 600    | 0.5 | 800            | 600             |
  | 800   | 600    | 1.0 | 800            | 600             |
  | 1920  | 1080   | 0.5 | 1920           | 1080            |

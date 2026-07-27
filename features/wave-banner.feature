# mutation-stamp: sha256=d7fd72d0c33d012614ad38bb0577c747f8dc6d3c1d2011ec3091d5c54482e1cc
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-27T18:35:13.919694Z","feature_name":"Wave banner","feature_path":"features/wave-banner.feature","background_hash":"a9ec6e117022da9c15cf9c45cedc294cdddfdf941f078d5b2d9a8432079752db","implementation_hash":"sha256:9e4d688e52e8b5c2d4b3479c1d5c522e9763e2aab2214c2918ec6df8ebf72660","scenarios":[{"index":0,"name":"Wave banner 01 wave completion shows a banner announcing the next wave","scenario_hash":"89245bb49a90bd856f76702d397b83ad34c944f96c01da278757fdb2e3c9488a","mutation_count":18,"result":{"Total":18,"Killed":18,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:27:47.192859Z"},{"index":1,"name":"Wave banner 02 banner text moves in toward the center","scenario_hash":"b10ba0d04f03cbdad6fed828abd43d866e6d67441e286b64fedf0c0b5efe4b10","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:27:47.192859Z"},{"index":2,"name":"Wave banner 03 banner text moves out after arriving","scenario_hash":"26f3f36a47af2b11e6dbfecc63ba76ed86dadcbad3e8a3b2aac52360698bfafd","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:27:47.192859Z"},{"index":3,"name":"Wave banner 04 after the banner finishes play resumes on the announced wave","scenario_hash":"bbbcde1aa6cc24a13cf2a7c24274f2079d84fb3ebf1a28d3a45595b5726bdc2c","mutation_count":18,"result":{"Total":18,"Killed":18,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:27:47.192859Z"},{"index":4,"name":"Wave banner 05 enemies do not advance while the wave banner is showing","scenario_hash":"cce10d0ae8a4350e5a1519dbbb13ddd1e6a9592522276b046c0aa1210c3c6ee0","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:27:47.192859Z"}]}
# acceptance-mutation-manifest-end

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
  When time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave banner phase is enter
  And the wave banner text has moved closer to the playfield center
  And the wave banner text is not fully off screen

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Wave banner 03 banner text moves out after arriving
  And the current wave has 1 scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  And time advances until the wave banner text reaches the playfield center
  Then the wave banner phase is exit
  When time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave banner phase is exit
  And the wave banner text has moved farther from the playfield center

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

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
  When time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is wave-banner
  And there are 0 enemy missiles in flight

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

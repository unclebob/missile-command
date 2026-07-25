# mutation-stamp: sha256=7634d868c6c2f6467cd59bb07e4a435baf2b8c0ec572cf071d38c07a49b7a330
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-24T21:23:54.715326Z","feature_name":"Options","feature_path":"features/options.feature","background_hash":"8e63f035c8dab0c09e62ed95cd3dfb2f8ecc23b566cec139f18d3206495fbae2","implementation_hash":"sha256:b0c6113edf1492e1536037028e745bda41d71e2a5a2e8ec156eb4462dad2c3b0","scenarios":[{"index":0,"name":"Options 01 options are reachable from the title screen","scenario_hash":"e2a1e058bf16897462c3c0ebf839dced6a4a565782d6577a0baaaffba06db597","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:54.715326Z"},{"index":1,"name":"Options 02 defaults are unmuted arcade difficulty and default fire keys","scenario_hash":"0f4317e2fca5bd93402cd0de6d4327d41c7d9bb3c9454251b0c34dadac2f907e","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:54.715326Z"},{"index":2,"name":"Options 03 mute can be toggled and is stored","scenario_hash":"ec910bf81e3993dc9417ffb055aa1a911b64315bcc2190c3804b3661b23b145b","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:54.715326Z"},{"index":3,"name":"Options 04 remapped fire keys launch the matching battery","scenario_hash":"61e753d42926c4f2b1867b819fa728e6f171f2b4f2aa9b5c0f793f2a4af02426","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:54.715326Z"},{"index":4,"name":"Options 05 difficulty scales wave enemy count and speed","scenario_hash":"835f4a24e439669076f9d79a4c3487ee6dc5eba941e1e11079141abf6f78bc8f","mutation_count":21,"result":{"Total":21,"Killed":21,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:54.715326Z"},{"index":5,"name":"Options 06 difficulty change is stored and applies on start","scenario_hash":"99527e00dc3ca32d6c567c63464ad041ae42959c58ee67b1967cbd278bf9d3b0","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:54.715326Z"},{"index":6,"name":"Options 07 leaving options returns to the title screen","scenario_hash":"cf6d7cd8c75047057817e3833472ba57fe20e4ee7ae939a024cc91a9daa1d275","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:54.715326Z"}]}
# acceptance-mutation-manifest-end

# Options 01 options are reachable from the title screen
# Options 02 defaults are unmuted arcade difficulty and default fire keys
# Options 03 mute can be toggled and is stored
# Options 04 remapped fire keys launch the matching battery
# Options 05 difficulty scales wave enemy count and speed
# Options 06 difficulty change applies on the next new game
# Options 07 leaving options returns to the title screen
Feature: Options

Background:
  Given a new game with width <width> and height <height>

Scenario: Options 01 options are reachable from the title screen
  Then the screen is title
  When the player opens options from the title
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is options

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

Scenario: Options 02 defaults are unmuted arcade difficulty and default fire keys
  When the player opens options from the title
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And mute is false
  And the difficulty is arcade
  And the fire key for left includes z
  And the fire key for center includes x
  And the fire key for right includes c
  And the pause key includes p

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Options 03 mute can be toggled and is stored
  When the player opens options from the title
  And the player sets mute to true
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And mute is true
  When the player leaves options
  And the player opens options from the title
  Then mute is true

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Options 04 remapped fire keys launch the matching battery
  When the player opens options from the title
  And the player binds fire left to key q
  And the player leaves options
  And the player starts the game
  And the player aims at 400 200
  And the player presses key q
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 1 defensive missiles in flight
  And a defensive missile from the left battery targets 400 200
  And the left battery has 9 missiles

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Options 05 difficulty scales wave enemy count and speed
  When the player opens options from the title
  And the player sets difficulty to easy
  And the player leaves options
  And the player starts the game
  And the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the difficulty is easy
  And wave <wave> enemy count is <enemy_count>
  And wave <wave> enemy speed is <enemy_speed>

Examples:
  | width | height | wave | enemy_count | enemy_speed | expected_width | expected_height |
  | 800   | 600    | 1    | 2           | 28.0        | 800            | 600             |
  | 800   | 600    | 3    | 2           | 35.0        | 800            | 600             |
  | 1920  | 1080   | 1    | 2           | 28.0        | 1920           | 1080            |

Scenario: Options 06 difficulty change is stored and applies on start
  When the player opens options from the title
  And the player sets difficulty to normal
  And the player leaves options
  When the player opens options from the title
  Then the difficulty is normal
  When the player leaves options
  And the player starts the game
  And the game is at wave 1
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave 1 enemy count is 2
  And wave 1 enemy speed is 34.0

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Options 07 leaving options returns to the title screen
  When the player opens options from the title
  Then the screen is options
  When the player leaves options
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

# mutation-stamp: sha256=16440c61f177064a942e9f56196e2c71969171b3703c1c8075c92c3f55b04a4d
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-26T15:19:31.974605Z","feature_name":"Enemy battery targets","feature_path":"features/enemy-battery-targets.feature","background_hash":"2ad6c53e758cb94bd90a7e42d0ca6dc51bfdcaa4e3368f8e8c551a4cb55a221c","implementation_hash":"sha256:eaf4e0fffcf75bb041036d5f080b31b9d78a8dff14f076d27b1778da21195e00","scenarios":[{"index":0,"name":"Enemy battery targets 01 wave enemies choose among cities and batteries","scenario_hash":"47be55b85ef7d39c9df7c4426709a40e10112fa274f627896b9f782266f37f32","mutation_count":18,"result":{"Total":18,"Killed":18,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:53:01.565577Z"},{"index":2,"name":"Enemy battery targets 03 a full target sweep includes every battery and city","scenario_hash":"fc29f595b4ae10b2c0405af06a3c63d49d889ccfd1c13d4047523254627b5f2e","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:53:01.565577Z"},{"index":3,"name":"Enemy battery targets 04 destroyed batteries are not chosen as wave targets","scenario_hash":"998bf7638f5aead98a3d282d505ec091904bcea52418c02f49cb987f0ae386f5","mutation_count":28,"result":{"Total":28,"Killed":28,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:53:01.565577Z"},{"index":1,"name":"Enemy battery targets 02 an unintercepted wave enemy destroys its battery target","scenario_hash":"301fac91c73651fb9393b04e726e6000af1a31dbe0b0679acdc960fb05a9a01f","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:51:45.567092Z"}]}
# acceptance-mutation-manifest-end

# Enemy battery targets 01 wave enemies choose among cities and batteries
# Enemy battery targets 02 an unintercepted wave enemy destroys its battery target
# Enemy battery targets 03 a full target sweep includes every battery and city
# Enemy battery targets 04 destroyed batteries are not chosen as wave targets
Feature: Enemy battery targets

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: Enemy battery targets 01 wave enemies choose among cities and batteries
  And the current wave has <remaining> scheduled enemies still active
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <expected_remaining> enemy missiles in flight
  And at least one enemy missile targets a city
  And at least one enemy missile targets a battery

Examples:
  | width | height | remaining | expected_remaining | expected_width | expected_height |
  | 800   | 600    | 9         | 9                  | 800            | 600             |
  | 800   | 600    | 12        | 12                 | 800            | 600             |
  | 1920  | 1080   | 9         | 9                  | 1920           | 1080            |

Scenario: Enemy battery targets 02 an unintercepted wave enemy destroys its battery target
  And a wave enemy missile targeting battery <battery>
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <battery> battery is destroyed
  And there are 0 enemy missiles in flight
  And there are <living_cities> living cities

Examples:
  | width | height | battery | living_cities | expected_width | expected_height |
  | 800   | 600    | left    | 6             | 800            | 600             |
  | 800   | 600    | center  | 6             | 800            | 600             |
  | 800   | 600    | right   | 6             | 800            | 600             |
  | 1920  | 1080   | left    | 6             | 1920           | 1080            |

Scenario: Enemy battery targets 03 a full target sweep includes every battery and city
  And the current wave has <remaining> scheduled enemies still active
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <expected_remaining> enemy missiles in flight
  And enemy missile targets include every living city
  And enemy missile targets include every non-destroyed battery

Examples:
  | width | height | remaining | expected_remaining | expected_width | expected_height |
  | 800   | 600    | 9         | 9                  | 800            | 600             |
  | 1920  | 1080   | 9         | 9                  | 1920           | 1080            |

Scenario: Enemy battery targets 04 destroyed batteries are not chosen as wave targets
  And the <battery> battery has been destroyed
  And the current wave has <remaining> scheduled enemies still active
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <expected_remaining> enemy missiles in flight
  And no enemy missile targets battery <battery>
  And every enemy missile targets a living city or a non-destroyed battery

Examples:
  | width | height | battery | remaining | expected_remaining | expected_width | expected_height |
  | 800   | 600    | left    | 8         | 8                  | 800            | 600             |
  | 800   | 600    | center  | 8         | 8                  | 800            | 600             |
  | 800   | 600    | right   | 8         | 8                  | 800            | 600             |
  | 1920  | 1080   | left    | 8         | 8                  | 1920           | 1080            |

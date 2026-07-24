# mutation-stamp: sha256=080ff3b3cb2340342e7a02b521780bc255054b5ccb8e8b714c646c2d92781466
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-24T19:34:41.851415Z","feature_name":"Waves and rearm","feature_path":"features/waves-and-rearm.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:92aab0c28954511f858c32744207fb14af09e7cd578fb63b2c8855bce7110dc2","scenarios":[{"index":1,"name":"Waves and rearm 02 wave does not complete while enemies remain","scenario_hash":"af333055b1b2722d2884ee730df1680bec54c7870b8dced74c2308082459cfc5","mutation_count":27,"result":{"Total":27,"Killed":27,"Survived":0,"Errors":0},"tested_at":"2026-07-24T19:34:41.851415Z"},{"index":6,"name":"Waves and rearm 06 higher waves schedule more or faster enemies","scenario_hash":"7c636fffb54532e5a5531e8a1b2f65b402f3a8022e9a61d58d878d352fc5fc89","mutation_count":30,"result":{"Total":30,"Killed":30,"Survived":0,"Errors":0},"tested_at":"2026-07-24T19:34:41.851415Z"},{"index":0,"name":"Waves and rearm 01 new game starts at wave one","scenario_hash":"c3e6ab3c5b584bf43edcf7efc48a894ba1fcf3cfc3b49087231eb66c73311dfb","mutation_count":14,"result":{"Total":14,"Killed":14,"Survived":0,"Errors":0},"tested_at":"2026-07-24T18:08:58.643319Z"},{"index":2,"name":"Waves and rearm 03 wave completes when all enemies are gone","scenario_hash":"2054a3ae5d8bdf32e4a4d0aaefd15af3053cd55e07454eb73bdda6a8b292ab07","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-24T18:08:58.643319Z"},{"index":3,"name":"Waves and rearm 07 hud shows the current wave number","scenario_hash":"4323ec19d7392c856ad7ed812d94fdcef91f5ef07b4af98daf3e363c8b534e30","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T18:08:58.643319Z"},{"index":4,"name":"Waves and rearm 04 surviving batteries rearm to full after wave","scenario_hash":"9dbb38eb27d50127a51ef2fc13de9e4b87c9d0d7c42ba0fb2a72a6bd019f8fa7","mutation_count":33,"result":{"Total":33,"Killed":33,"Survived":0,"Errors":0},"tested_at":"2026-07-24T18:08:58.643319Z"},{"index":5,"name":"Waves and rearm 05 destroyed batteries stay destroyed after rearm","scenario_hash":"eb48dfcb6c3b5dc9ad8e476eda7b7a81798cb7ad4afba09985a18b6e3d171136","mutation_count":27,"result":{"Total":27,"Killed":27,"Survived":0,"Errors":0},"tested_at":"2026-07-24T18:08:58.643319Z"}]}
# acceptance-mutation-manifest-end

# Waves and rearm 01 new game starts at wave one
# Waves and rearm 02 wave does not complete while enemies remain
# Waves and rearm 03 wave completes when all enemies are gone
# Waves and rearm 04 surviving batteries rearm to full after wave
# Waves and rearm 05 destroyed batteries stay destroyed after rearm
# Waves and rearm 06 higher waves schedule more or faster enemies
# Waves and rearm 07 hud shows the current wave number
Feature: Waves and rearm

Scenario: Waves and rearm 01 new game starts at wave one
  Given a new game with width <width> and height <height>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave number is <wave>
  And each non-destroyed battery has <ammo> missiles
  And the hud shows wave <expected_wave>

Examples:
  | width | height | wave | expected_wave | ammo | expected_width | expected_height |
  | 800   | 600    | 1    | 1             | 10   | 800            | 600             |
  | 1920  | 1080   | 1    | 1             | 10   | 1920           | 1080            |

Scenario: Waves and rearm 02 wave does not complete while enemies remain
  Given a new game with width <width> and height <height>
  And the current wave has <remaining> scheduled enemies still active
  When time advances by 0.05 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave number is <wave>
  And there are <enemy_count> enemy missiles in flight
  And the last applied dt is <expected_applied_dt>
  And an enemy missile has progressed toward city 0
  And the first enemy missile progress equals <expected_progress>
  And the wave is not complete

Examples:
  | width | height | remaining | wave | enemy_count | expected_applied_dt | expected_progress     | expected_width | expected_height |
  | 800   | 600    | 1         | 1    | 1           | 0.05                | 0.003869969040247678  | 800            | 600             |
  | 800   | 600    | 3         | 1    | 3           | 0.05                | 0.004376587469505331  | 800            | 600             |
  | 1920  | 1080   | 2         | 1    | 2           | 0.05                | 0.00236738178373661   | 1920           | 1080            |

Scenario: Waves and rearm 03 wave completes when all enemies are gone
  Given a new game with width <width> and height <height>
  And the current wave has <remaining> scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave is complete
  And the wave number is <wave>
  And the hud shows wave <expected_wave>
  And there are <enemy_count> enemy missiles in flight

Examples:
  | width | height | remaining | wave | expected_wave | enemy_count | expected_width | expected_height |
  | 800   | 600    | 1         | 2    | 2             | 0           | 800            | 600             |
  | 800   | 600    | 2         | 2    | 2             | 0           | 800            | 600             |
  | 1920  | 1080   | 1         | 2    | 2             | 0           | 1920           | 1080            |

Scenario: Waves and rearm 07 hud shows the current wave number
  Given a new game with width <width> and height <height>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave number is <wave>
  And the hud shows wave <expected_wave>

Examples:
  | width | height | wave | expected_wave | expected_width | expected_height |
  | 800   | 600    | 1    | 1             | 800            | 600             |
  | 1920  | 1080   | 1    | 1             | 1920           | 1080            |

Scenario: Waves and rearm 04 surviving batteries rearm to full after wave
  Given a new game with width <width> and height <height>
  And every non-destroyed battery has <spent_ammo> missiles
  And each non-destroyed battery has <expected_spent_ammo> missiles
  And the current wave has <remaining> scheduled enemies still active
  And there are <expected_remaining> enemy missiles in flight
  When time advances until all wave enemies are destroyed or have impacted
  And the next wave starts
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And each non-destroyed battery has <ammo> missiles
  And the wave number is <wave>
  And the hud shows wave <expected_wave>

Examples:
  | width | height | spent_ammo | expected_spent_ammo | remaining | expected_remaining | ammo | wave | expected_wave | expected_width | expected_height |
  | 800   | 600    | 3          | 3                   | 1         | 1                  | 10   | 2    | 2             | 800            | 600             |
  | 800   | 600    | 7          | 7                   | 1         | 1                  | 10   | 2    | 2             | 800            | 600             |
  | 1920  | 1080   | 0          | 0                   | 1         | 1                  | 10   | 2    | 2             | 1920           | 1080            |

Scenario: Waves and rearm 05 destroyed batteries stay destroyed after rearm
  Given a new game with width <width> and height <height>
  And the <battery> battery has been destroyed
  And the current wave has <remaining> scheduled enemies still active
  And there are <expected_remaining> enemy missiles in flight
  When time advances until all wave enemies are destroyed or have impacted
  And the next wave starts
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <battery> battery is destroyed
  And each non-destroyed battery has <ammo> missiles
  When the player fires the <battery> battery
  Then there are <missile_count> defensive missiles in flight

Examples:
  | width | height | battery | remaining | expected_remaining | ammo | missile_count | expected_width | expected_height |
  | 800   | 600    | left    | 1         | 1                  | 10   | 0             | 800            | 600             |
  | 800   | 600    | center  | 1         | 1                  | 10   | 0             | 800            | 600             |
  | 800   | 600    | right   | 1         | 1                  | 10   | 0             | 800            | 600             |

Scenario: Waves and rearm 06 higher waves schedule more or faster enemies
  Given a new game with width <width> and height <height>
  And wave <low_wave> enemy schedule metrics are recorded
  When the game is at wave <high_wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave <high_wave> is harder than wave <low_wave> by enemy count or enemy speed
  And wave <low_wave> has enemy count <low_count> and speed <low_speed>
  And wave <high_wave> has enemy count <high_count> and speed <high_speed>

Examples:
  | width | height | low_wave | high_wave | low_count | low_speed | high_count | high_speed | expected_width | expected_height |
  | 800   | 600    | 1        | 2         | 3         | 50.0      | 4          | 62.5       | 800            | 600             |
  | 800   | 600    | 1        | 3         | 3         | 50.0      | 5          | 75.0       | 800            | 600             |
  | 1920  | 1080   | 2        | 4         | 4         | 62.5      | 6          | 87.5       | 1920           | 1080            |

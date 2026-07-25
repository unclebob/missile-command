# mutation-stamp: sha256=a5c8c085eeac33c5292d7afb4a13a55b4255828f88caf52582e58360badbf61a
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-25T16:04:11.622824Z","feature_name":"Waves and rearm","feature_path":"features/waves-and-rearm.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:92aab0c28954511f858c32744207fb14af09e7cd578fb63b2c8855bce7110dc2","scenarios":[{"index":1,"name":"Waves and rearm 02 wave does not complete while enemies remain","scenario_hash":"77f7c5eafff46911bb98082328557d22a15f12a9f22d2e93c3d7ac155aa5d09a","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:54:22.935743Z"},{"index":6,"name":"Waves and rearm 06 higher waves schedule more or faster enemies","scenario_hash":"567aec43c87b097572decc37238c30f87c0aeda9b83ef1531d15c7dab5464268","mutation_count":30,"result":{"Total":30,"Killed":30,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:42.463165Z"},{"index":0,"name":"Waves and rearm 01 new game starts at wave one","scenario_hash":"41ae11d8f665dd96d59547f239592ad1e87c9c1ced0286eb63fe394f6408f11f","mutation_count":14,"result":{"Total":14,"Killed":14,"Survived":0,"Errors":0},"tested_at":"2026-07-25T02:14:52.562319Z"},{"index":2,"name":"Waves and rearm 03 wave completes when all enemies are gone","scenario_hash":"ad23ee6937591d7e80f1249d7c4b6dd4bc7d316a07a6df48972fcfb3d57db4b4","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-25T02:14:52.562319Z"},{"index":3,"name":"Waves and rearm 07 hud shows the current wave number","scenario_hash":"35329f9b009d85d5d25021b7a4f3c920f8ebcbc94043750ec0224a01c0f41a29","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-25T02:14:52.562319Z"},{"index":4,"name":"Waves and rearm 04 surviving batteries rearm to full after wave","scenario_hash":"2aff6ec3e7470fbf36bb9f5df4211978d671fc4392bb98d7d59981b8a255bbba","mutation_count":33,"result":{"Total":33,"Killed":33,"Survived":0,"Errors":0},"tested_at":"2026-07-25T02:14:52.562319Z"},{"index":5,"name":"Waves and rearm 05 destroyed batteries are restored after rearm","scenario_hash":"41ca46db477acd1504522cdfcfbf80a008ee99ae46668a7f703d2711a45c199d","mutation_count":27,"result":{"Total":27,"Killed":27,"Survived":0,"Errors":0},"tested_at":"2026-07-25T02:14:52.562319Z"}]}
# acceptance-mutation-manifest-end

# Waves and rearm 01 new game starts at wave one
# Waves and rearm 02 wave does not complete while enemies remain
# Waves and rearm 03 wave completes when all enemies are gone
# Waves and rearm 04 surviving batteries rearm to full after wave
# Waves and rearm 05 destroyed batteries are restored after rearm
# Waves and rearm 06 higher waves schedule more or faster enemies
# Waves and rearm 07 hud shows the current wave number
Feature: Waves and rearm

Scenario: Waves and rearm 01 new game starts at wave one
  Given a new game with width <width> and height <height>
  And the player starts the game
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
  And the player starts the game
  And the current wave has <remaining> scheduled enemies still active
  When time advances by 0.05 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave number is <wave>
  And there are <enemy_count> enemy missiles in flight
  And the last applied dt is <expected_applied_dt>
  And an enemy missile has progressed toward city 0
  And the first enemy missile progress matches last applied dt and path length
  And the wave is not complete

Examples:
  | width | height | remaining | wave | enemy_count | expected_applied_dt | expected_width | expected_height |
  | 800   | 600    | 1         | 1    | 1           | 0.05                | 800            | 600             |
  | 800   | 600    | 3         | 1    | 3           | 0.05                | 800            | 600             |
  | 1920  | 1080   | 2         | 1    | 2           | 0.05                | 1920           | 1080            |

Scenario: Waves and rearm 03 wave completes when all enemies are gone
  Given a new game with width <width> and height <height>
  And the player starts the game
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
  And the player starts the game
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
  And the player starts the game
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

Scenario: Waves and rearm 05 destroyed batteries are restored after rearm
  Given a new game with width <width> and height <height>
  And the player starts the game
  And the <battery> battery has been destroyed
  And the current wave has <remaining> scheduled enemies still active
  And there are <expected_remaining> enemy missiles in flight
  When time advances until all wave enemies are destroyed or have impacted
  And the next wave starts
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <battery> battery is not destroyed
  And each non-destroyed battery has <ammo> missiles
  When the player fires the <battery> battery
  Then there are <missile_count> defensive missiles in flight

Examples:
  | width | height | battery | remaining | expected_remaining | ammo | missile_count | expected_width | expected_height |
  | 800   | 600    | left    | 1         | 1                  | 10   | 1             | 800            | 600             |
  | 800   | 600    | center  | 1         | 1                  | 10   | 1             | 800            | 600             |
  | 800   | 600    | right   | 1         | 1                  | 10   | 1             | 800            | 600             |

Scenario: Waves and rearm 06 higher waves schedule more or faster enemies
  Given a new game with width <width> and height <height>
  And the player starts the game
  And wave <low_wave> enemy schedule metrics are recorded
  When the game is at wave <high_wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave <high_wave> is harder than wave <low_wave> by enemy count or enemy speed
  And wave <low_wave> has enemy count <low_count> and speed <low_speed>
  And wave <high_wave> has enemy count <high_count> and speed <high_speed>

Examples:
  | width | height | low_wave | high_wave | low_count | low_speed | high_count | high_speed | expected_width | expected_height |
  | 800   | 600    | 1        | 2         | 3         | 40.0      | 3          | 45.0       | 800            | 600             |
  | 800   | 600    | 1        | 3         | 3         | 40.0      | 3          | 50.0       | 800            | 600             |
  | 1920  | 1080   | 2        | 4         | 3         | 45.0      | 3          | 55.0       | 1920           | 1080            |

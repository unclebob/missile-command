# mutation-stamp: sha256=6778eabf0a7b8a8e841a2d06dd85c1510a3b96f715acc73701ffbacf31707909
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-24T19:34:37.254191Z","feature_name":"Fire batteries with keys","feature_path":"features/fire-batteries-keys.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:7937df03dc46795934f64a2c4aeec6397cf1df16642148dbc4e9642ea6132a49","scenarios":[{"index":4,"name":"Fire batteries keys 05 center missile is faster than side missiles","scenario_hash":"d8721160441d7471a3029e5f8fbd63c1df694b7e39ddfc84d9621fc18e43fc28","mutation_count":10,"result":{"Total":10,"Killed":10,"Survived":0,"Errors":0},"tested_at":"2026-07-24T16:47:18.051237Z"},{"index":0,"name":"Fire batteries keys 01 stocked battery launches toward crosshair","scenario_hash":"e856b6035bc9cab5fcea00831161711c8e7a7815dfa4c46ed8cc735e3b3ab96b","mutation_count":66,"result":{"Total":66,"Killed":66,"Survived":0,"Errors":0},"tested_at":"2026-07-24T16:46:58.832001Z"},{"index":1,"name":"Fire batteries keys 02 firing one battery leaves others full","scenario_hash":"d8363e1d70be7d53e8cab3f9c8f8704184c88a84ec7522a66e540861959c9a6d","mutation_count":9,"result":{"Total":9,"Killed":9,"Survived":0,"Errors":0},"tested_at":"2026-07-24T16:46:58.832001Z"},{"index":2,"name":"Fire batteries keys 03 empty battery does not fire","scenario_hash":"124f233046941b58b3511994d96f59c0a65c6393b167101da0fe062774ebf2e2","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T16:46:58.832001Z"},{"index":3,"name":"Fire batteries keys 04 destroyed battery does not fire","scenario_hash":"418d28eacaff74ff934734cc33e12460a534705090404b716ed9d53effe24047","mutation_count":9,"result":{"Total":9,"Killed":9,"Survived":0,"Errors":0},"tested_at":"2026-07-24T16:46:58.832001Z"}]}
# acceptance-mutation-manifest-end

# Fire batteries keys 01 stocked battery launches toward crosshair
# Fire batteries keys 02 firing one battery leaves others full
# Fire batteries keys 03 empty battery does not fire
# Fire batteries keys 04 destroyed battery does not fire
# Fire batteries keys 05 center missile is faster than side missiles
Feature: Fire batteries with keys

Scenario: Fire batteries keys 01 stocked battery launches toward crosshair
  Given a new game with width <width> and height <height>
  And the player starts the game
  And the player aims at <aim_x> <aim_y>
  When the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <battery> battery has <ammo> missiles
  And there are <missile_count> defensive missiles in flight
  And a defensive missile from the <battery> battery targets <expected_x> <expected_y>

Examples:
  | width | height | aim_x | aim_y | expected_width | expected_height | battery | ammo | missile_count | expected_x | expected_y |
  | 800   | 600    | 400   | 200   | 800            | 600             | left    | 9    | 1             | 400        | 200        |
  | 800   | 600    | 400   | 200   | 800            | 600             | center  | 9    | 1             | 400        | 200        |
  | 800   | 600    | 400   | 200   | 800            | 600             | right   | 9    | 1             | 400        | 200        |
  | 1920  | 1080   | 960   | 300   | 1920           | 1080            | left    | 9    | 1             | 960        | 300        |
  | 1920  | 1080   | 960   | 300   | 1920           | 1080            | center  | 9    | 1             | 960        | 300        |
  | 1920  | 1080   | 960   | 300   | 1920           | 1080            | right   | 9    | 1             | 960        | 300        |

Scenario: Fire batteries keys 02 firing one battery leaves others full
  Given a new game with width 800 and height 600
  And the player starts the game
  And the player aims at 400 200
  When the player fires the <battery> battery
  Then the <battery> battery has <ammo> missiles
  And every other battery has <full_ammo> missiles

Examples:
  | battery | ammo | full_ammo |
  | left    | 9    | 10        |
  | center  | 9    | 10        |
  | right   | 9    | 10        |

Scenario: Fire batteries keys 03 empty battery does not fire
  Given a new game with width 800 and height 600
  And the player starts the game
  And the player aims at 400 200
  And the <battery> battery ammo is set to <set_ammo>
  When the player fires the <battery> battery
  Then the <battery> battery has <remaining_ammo> missiles
  And there are <missile_count> defensive missiles in flight

Examples:
  | battery | set_ammo | remaining_ammo | missile_count |
  | left    | 0        | 0              | 0             |
  | center  | 0        | 0              | 0             |
  | right   | 0        | 0              | 0             |

Scenario: Fire batteries keys 04 destroyed battery does not fire
  Given a new game with width 800 and height 600
  And the player starts the game
  And the player aims at 400 200
  And the <battery> battery is destroyed
  When the player fires the <battery> battery
  Then there are <missile_count> defensive missiles in flight
  And the <battery> battery has <ammo> missiles

Examples:
  | battery | missile_count | ammo |
  | left    | 0             | 0    |
  | center  | 0             | 0    |
  | right   | 0             | 0    |

Scenario: Fire batteries keys 05 center missile is faster than side missiles
  Given a new game with width <width> and height <height>
  And the player starts the game
  And the player aims at 400 100
  When the player fires every battery once
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the center defensive missile is faster than each side defensive missile
  And there are <missile_count> defensive missiles in flight

Examples:
  | width | height | expected_width | expected_height | missile_count |
  | 800   | 600    | 800            | 600             | 3             |
  | 1920  | 1080   | 1920           | 1080            | 3             |

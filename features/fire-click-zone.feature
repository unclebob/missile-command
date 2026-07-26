# mutation-stamp: sha256=25d0dd5dd528f4f3443f203ca756df88660c0f8c8fa076fb59e58e6f80acb694
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-26T15:39:58.294899Z","feature_name":"Fire by click zone","feature_path":"features/fire-click-zone.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:f4d9a2a5a0ed190e611237bae4843bee49c25f479ffbf8a96941c565ae880439","scenarios":[{"index":0,"name":"Fire click zone 01 click fires battery for horizontal third","scenario_hash":"2f49ac3fc518fc658e3e9b47e29e52f3cd44c6fe01fb1a9a3292e5dc18bf4e36","mutation_count":132,"result":{"Total":132,"Killed":132,"Survived":0,"Errors":0},"tested_at":"2026-07-25T16:12:28.227363Z"},{"index":1,"name":"Fire click zone 02 empty batteries fall back along the zone order","scenario_hash":"cf14e07a8e235a115ec964ab1cb6a5fcabe5967d6f708412df4ea31d7fe6f016","mutation_count":66,"result":{"Total":66,"Killed":66,"Survived":0,"Errors":0},"tested_at":"2026-07-25T16:12:28.227363Z"},{"index":2,"name":"Fire click zone 03 destroyed batteries fall back along the zone order","scenario_hash":"ab011c41d8a725e1bf3d5aa3f2a7c9967822de6f5d1502cb678fbf281aa1246d","mutation_count":66,"result":{"Total":66,"Killed":66,"Survived":0,"Errors":0},"tested_at":"2026-07-25T16:12:28.227363Z"},{"index":3,"name":"Fire click zone 04 no battery can fire yields no missile","scenario_hash":"de943db6c91b6e62176636fa272f64a8e43400816fb1555b8b12b813674240a0","mutation_count":27,"result":{"Total":27,"Killed":27,"Survived":0,"Errors":0},"tested_at":"2026-07-25T16:12:28.227363Z"},{"index":7,"name":"Fire click zone 06 zone mapping uses width after resize","scenario_hash":"d37d4127e1db98ab64e5647e7e5f4a920897a516bfc5d1e0adedd2c179086089","mutation_count":40,"result":{"Total":40,"Killed":40,"Survived":0,"Errors":0},"tested_at":"2026-07-25T16:12:28.227363Z"}]}
# acceptance-mutation-manifest-end

# Fire click zone 01 click fires battery for horizontal third
# Fire click zone 02 empty batteries fall back along the zone order
# Fire click zone 03 destroyed batteries fall back along the zone order
# Fire click zone 04 no battery can fire yields no missile
# Fire click zone 05 key fire remains available with click fire
# Fire click zone 06 zone mapping uses width after resize
Feature: Fire by click zone

Scenario: Fire click zone 01 click fires battery for horizontal third
  Given a new game with width <width> and height <height>
  And the player starts the game
  When the player clicks at <x> <y>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the crosshair is at <expected_x> <expected_y>
  And a defensive missile from the <battery> battery targets <expected_x> <expected_y>
  And the <battery> battery has <ammo> missiles
  And there are <missile_count> defensive missiles in flight

Examples:
  | width | height | x    | y   | expected_width | expected_height | expected_x | expected_y | battery | ammo | missile_count |
  | 900   | 600    | 0    | 100 | 900            | 600             | 0          | 100        | left    | 9    | 1             |
  | 900   | 600    | 299  | 100 | 900            | 600             | 299        | 100        | left    | 9    | 1             |
  | 900   | 600    | 300  | 100 | 900            | 600             | 300        | 100        | center  | 9    | 1             |
  | 900   | 600    | 599  | 100 | 900            | 600             | 599        | 100        | center  | 9    | 1             |
  | 900   | 600    | 600  | 100 | 900            | 600             | 600        | 100        | right   | 9    | 1             |
  | 900   | 600    | 899  | 100 | 900            | 600             | 899        | 100        | right   | 9    | 1             |
  | 1920  | 1080   | 0    | 200 | 1920           | 1080            | 0          | 200        | left    | 9    | 1             |
  | 1920  | 1080   | 639  | 200 | 1920           | 1080            | 639        | 200        | left    | 9    | 1             |
  | 1920  | 1080   | 640  | 200 | 1920           | 1080            | 640        | 200        | center  | 9    | 1             |
  | 1920  | 1080   | 1279 | 200 | 1920           | 1080            | 1279       | 200        | center  | 9    | 1             |
  | 1920  | 1080   | 1280 | 200 | 1920           | 1080            | 1280       | 200        | right   | 9    | 1             |
  | 1920  | 1080   | 1500 | 200 | 1920           | 1080            | 1500       | 200        | right   | 9    | 1             |

Scenario: Fire click zone 02 empty batteries fall back along the zone order
  Given a new game with width <width> and height <height>
  And the player starts the game
  And the click must fall back to the <battery> battery because earlier batteries are empty
  When the player clicks at <x> <y>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the crosshair is at <expected_x> <expected_y>
  And a defensive missile from the <battery> battery targets <expected_x> <expected_y>
  And the <battery> battery has <ammo> missiles
  And there are <missile_count> defensive missiles in flight

Examples:
  | width | height | x   | y   | expected_width | expected_height | expected_x | expected_y | battery | ammo | missile_count |
  | 900   | 600    | 100 | 100 | 900            | 600             | 100        | 100        | center  | 9    | 1             |
  | 900   | 600    | 100 | 100 | 900            | 600             | 100        | 100        | right   | 9    | 1             |
  | 900   | 600    | 800 | 100 | 900            | 600             | 800        | 100        | center  | 9    | 1             |
  | 900   | 600    | 800 | 100 | 900            | 600             | 800        | 100        | left    | 9    | 1             |
  | 900   | 600    | 450 | 100 | 900            | 600             | 450        | 100        | left    | 9    | 1             |
  | 900   | 600    | 450 | 100 | 900            | 600             | 450        | 100        | right   | 9    | 1             |

Scenario: Fire click zone 03 destroyed batteries fall back along the zone order
  Given a new game with width <width> and height <height>
  And the player starts the game
  And the click must fall back to the <battery> battery because earlier batteries are destroyed
  When the player clicks at <x> <y>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the crosshair is at <expected_x> <expected_y>
  And a defensive missile from the <battery> battery targets <expected_x> <expected_y>
  And the <battery> battery has <ammo> missiles
  And there are <missile_count> defensive missiles in flight

Examples:
  | width | height | x   | y   | expected_width | expected_height | expected_x | expected_y | battery | ammo | missile_count |
  | 900   | 600    | 100 | 100 | 900            | 600             | 100        | 100        | center  | 9    | 1             |
  | 900   | 600    | 100 | 100 | 900            | 600             | 100        | 100        | right   | 9    | 1             |
  | 900   | 600    | 800 | 100 | 900            | 600             | 800        | 100        | center  | 9    | 1             |
  | 900   | 600    | 800 | 100 | 900            | 600             | 800        | 100        | left    | 9    | 1             |
  | 900   | 600    | 450 | 100 | 900            | 600             | 450        | 100        | left    | 9    | 1             |
  | 900   | 600    | 450 | 100 | 900            | 600             | 450        | 100        | right   | 9    | 1             |

Scenario: Fire click zone 04 no battery can fire yields no missile
  Given a new game with width <width> and height <height>
  And the player starts the game
  And no battery can fire
  When the player clicks at <x> <y>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <missile_count> defensive missiles in flight
  And the crosshair is at <expected_x> <expected_y>

Examples:
  | width | height | x   | y   | expected_width | expected_height | expected_x | expected_y | missile_count |
  | 900   | 600    | 100 | 100 | 900            | 600             | 100        | 100        | 0             |
  | 900   | 600    | 450 | 100 | 900            | 600             | 450        | 100        | 0             |
  | 900   | 600    | 800 | 100 | 900            | 600             | 800        | 100        | 0             |

Scenario: Fire click zone 05 key fire after click left then key right
  Given a new game with width 900 and height 600
  And the player starts the game
  When the player clicks at 100 150
  And the player aims at 800 120
  And the player fires the right battery
  Then there are 2 defensive missiles in flight
  And the right battery has 9 missiles
  And a defensive missile from the right battery targets 800 120

Scenario: Fire click zone 05b key fire after click right then key left
  Given a new game with width 900 and height 600
  And the player starts the game
  When the player clicks at 800 150
  And the player aims at 100 120
  And the player fires the left battery
  Then there are 2 defensive missiles in flight
  And the left battery has 9 missiles
  And a defensive missile from the left battery targets 100 120

Scenario: Fire click zone 05c key fire after click left then key center
  Given a new game with width 900 and height 600
  And the player starts the game
  When the player clicks at 100 150
  And the player aims at 450 120
  And the player fires the center battery
  Then there are 2 defensive missiles in flight
  And the center battery has 9 missiles
  And a defensive missile from the center battery targets 450 120

Scenario: Fire click zone 06 zone mapping uses width after resize
  Given a new game with width 900 and height 600
  And the player starts the game
  When the playfield is resized to width <new_width> and height <new_height>
  And the player clicks at <x> <y>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And a defensive missile from the <battery> battery targets <expected_x> <expected_y>
  And the <battery> battery has <ammo> missiles

Examples:
  | new_width | new_height | x    | y   | expected_width | expected_height | expected_x | expected_y | battery | ammo |
  | 1800      | 600        | 500  | 100 | 1800           | 600             | 500        | 100        | left    | 9    |
  | 1800      | 600        | 900  | 100 | 1800           | 600             | 900        | 100        | center  | 9    |
  | 1800      | 600        | 1500 | 100 | 1800           | 600             | 1500       | 100        | right   | 9    |
  | 900       | 600        | 450  | 100 | 900            | 600             | 450        | 100        | center  | 9    |

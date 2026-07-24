# mutation-stamp: sha256=34a7b8397146693b1183fdcbcd11e2c33faee70e4e2b5ffc4b3bb3809a3caed5
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-24T19:34:34.448528Z","feature_name":"Aim crosshair","feature_path":"features/aim-crosshair.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:7b4387ba8eb6cee137ec3f66341fb5339e58e2e8b39f827f5e595123a6d3d3f0","scenarios":[{"index":0,"name":"Aim crosshair 01 aim inside the playfield","scenario_hash":"88cb15a74cd3a1b075f9def185373273ea9c6bbc52efc26c7e524410d4bf9ad9","mutation_count":32,"result":{"Total":32,"Killed":32,"Survived":0,"Errors":0},"tested_at":"2026-07-24T16:46:57.630853Z"},{"index":4,"name":"Aim crosshair 05 aim does not change forces or score","scenario_hash":"f7721b2840c409d0841e5b506d892127b5b31456e8595edf62a7f76a35ae5b25","mutation_count":22,"result":{"Total":22,"Killed":22,"Survived":0,"Errors":0},"tested_at":"2026-07-24T16:46:57.630853Z"}]}
# acceptance-mutation-manifest-end

# Aim crosshair 01 aim inside the playfield
# Aim crosshair 02 aim outside clamps to the playfield
# Aim crosshair 03 aim does not change forces or score
Feature: Aim crosshair

Scenario: Aim crosshair 01 aim inside the playfield
  Given a new game with width <width> and height <height>
  And the player starts the game
  When the player aims at <x> <y>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the crosshair is at <expected_x> <expected_y>

Examples:
  | width | height | x    | y    | expected_width | expected_height | expected_x | expected_y |
  | 800   | 600    | 100  | 200  | 800            | 600             | 100        | 200        |
  | 800   | 600    | 400  | 300  | 800            | 600             | 400        | 300        |
  | 1920  | 1080   | 960  | 540  | 1920           | 1080            | 960        | 540        |
  | 1024  | 768    | 1    | 1    | 1024           | 768             | 1          | 1          |

Scenario: Aim crosshair 02 clamps left and right edges
  Given a new game with width 800 and height 600
  And the player starts the game
  When the player aims at -1 100
  Then the crosshair is at 0 100
  When the player aims at 800 100
  Then the crosshair is at 799 100

Scenario: Aim crosshair 03 clamps top and bottom edges
  Given a new game with width 800 and height 600
  And the player starts the game
  When the player aims at 100 -1
  Then the crosshair is at 100 0
  When the player aims at 100 600
  Then the crosshair is at 100 599

Scenario: Aim crosshair 04 clamps far outside a larger playfield
  Given a new game with width 1920 and height 1080
  And the player starts the game
  When the player aims at 1920 500
  Then the crosshair is at 1919 500
  When the player aims at 100 1080
  Then the crosshair is at 100 1079

Scenario: Aim crosshair 05 aim does not change forces or score
  Given a new game with width <width> and height <height>
  And the player starts the game
  When the player aims at <x> <y>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <city_count> living cities
  And each battery has <ammo> missiles
  And the score is <score>
  And the crosshair is at <expected_x> <expected_y>

Examples:
  | width | height | x   | y   | expected_width | expected_height | expected_x | expected_y | city_count | ammo | score |
  | 800   | 600    | 250 | 150 | 800            | 600             | 250        | 150        | 6          | 10   | 0     |
  | 1920  | 1080   | 10  | 20  | 1920           | 1080            | 10         | 20         | 6          | 10   | 0     |

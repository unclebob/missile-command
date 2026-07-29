# mutation-stamp: sha256=a3469f9bcf5a131cf7597b5172982cce9ab76539f45c8c04a9f66241bae62415
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-27T18:34:56.836686Z","feature_name":"Defensive missiles and fireballs","feature_path":"features/defensive-missiles-fireballs.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:3d9a7c583781a139acfb8a44a66586993fc2a67388a442804ed8ab4862e2f5bc","scenarios":[{"index":0,"name":"Defensive missiles fireballs 01 missile advances toward the aim point","scenario_hash":"d521c8f28388c27ac2956302bbdcc7652cd3c0455e9f53c3cc4b587542d0bf71","mutation_count":40,"result":{"Total":40,"Killed":40,"Survived":0,"Errors":0},"tested_at":"2026-07-25T16:12:23.916548Z"},{"index":1,"name":"Defensive missiles fireballs 02 missile becomes a fireball on arrival","scenario_hash":"e3f1bf7e062ab06f8aeae8ee9a4881171c55a2c88a9d0c1ad3f742d4aa290c37","mutation_count":44,"result":{"Total":44,"Killed":44,"Survived":0,"Errors":0},"tested_at":"2026-07-25T16:12:23.916548Z"},{"index":7,"name":"Defensive missiles fireballs 06 large time steps are clamped","scenario_hash":"02a4837ebc5c76f8c7fc470580e5b1e84861c52d4fc13474487c36bcc0785f76","mutation_count":30,"result":{"Total":30,"Killed":30,"Survived":0,"Errors":0},"tested_at":"2026-07-25T16:12:23.916548Z"}]}
# acceptance-mutation-manifest-end

# Defensive missiles fireballs 01 missile advances toward the aim point
# Defensive missiles fireballs 02 missile becomes a fireball on arrival
# Defensive missiles fireballs 03 fireball expands then contracts then disappears
# Defensive missiles fireballs 04 destroyable target inside fireball is destroyed
# Defensive missiles fireballs 05 destroyable target outside fireball survives
# Defensive missiles fireballs 06 large time steps are clamped
Feature: Defensive missiles and fireballs

Scenario: Defensive missiles fireballs 01 missile advances toward the aim point
  Given a new game with width <width> and height <height>
  And the player starts the game
  And the player aims at <aim_x> <aim_y>
  And the player fires the <battery> battery
  When time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <missile_count> defensive missiles in flight
  And a defensive missile from the <battery> battery has progressed toward <expected_x> <expected_y>

Examples:
  | width | height | aim_x | aim_y | expected_width | expected_height | expected_x | expected_y | battery | missile_count |
  | 800   | 600    | 400   | 100   | 800            | 600             | 400        | 100        | left    | 1             |
  | 800   | 600    | 400   | 100   | 800            | 600             | 400        | 100        | center  | 1             |
  | 800   | 600    | 400   | 100   | 800            | 600             | 400        | 100        | right   | 1             |
  | 1920  | 1080   | 960   | 200   | 1440           | 1080            | 960        | 200        | center  | 1             |

Scenario: Defensive missiles fireballs 02 missile becomes a fireball on arrival
  Given a new game with width <width> and height <height>
  And the player starts the game
  And the player aims at <aim_x> <aim_y>
  And the player fires the <battery> battery
  When time advances until defensive missiles arrive
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <missile_count> defensive missiles in flight
  And there are <fireball_count> fireballs
  And a fireball is centered at <expected_x> <expected_y>

Examples:
  | width | height | aim_x | aim_y | expected_width | expected_height | expected_x | expected_y | battery | missile_count | fireball_count |
  | 800   | 600    | 400   | 100   | 800            | 600             | 400        | 100        | left    | 0             | 1              |
  | 800   | 600    | 400   | 100   | 800            | 600             | 400        | 100        | center  | 0             | 1              |
  | 800   | 600    | 400   | 100   | 800            | 600             | 400        | 100        | right   | 0             | 1              |
  | 1920  | 1080   | 960   | 200   | 1440           | 1080            | 960        | 200        | center  | 0             | 1              |

Scenario: Defensive missiles fireballs 03 fireball expands then contracts then disappears
  Given a new game with width 800 and height 600
  And the player starts the game
  And the player aims at 400 100
  And the player fires the center battery
  When time advances until defensive missiles arrive
  Then there is an active fireball
  And the fireball start time is recorded
  When time advances until the fireball reaches max radius
  Then the fireball max time is at least the fireball start time
  And a fireball radius is greater than 35
  When time advances into the fireball shrink phase
  Then the fireball shrink time is at least the fireball max time
  And a fireball radius is less than the max fireball radius
  When time advances until fireballs expire
  Then there are 0 fireballs
  And the fireball end time is at least the fireball shrink time

Scenario: Defensive missiles fireballs 04 destroyable target inside fireball is destroyed
  Given a new game with width 800 and height 600
  And the player starts the game
  And the player aims at 400 200
  And the player fires the center battery
  And a destroyable target at 400 200
  When time advances until defensive missiles arrive
  And time advances until fireballs reach at least radius 1
  Then the destroyable target is destroyed
  And there are 1 fireballs

Scenario: Defensive missiles fireballs 04b destroyable near aim is destroyed
  Given a new game with width 800 and height 600
  And the player starts the game
  And the player aims at 400 200
  And the player fires the center battery
  And a destroyable target at 405 200
  When time advances until defensive missiles arrive
  And time advances until fireballs reach at least radius 10
  Then the destroyable target is destroyed
  And there are 1 fireballs

Scenario: Defensive missiles fireballs 05 destroyable target outside fireball survives
  Given a new game with width 800 and height 600
  And the player starts the game
  And the player aims at 400 200
  And the player fires the center battery
  And a destroyable target at 50 50
  When time advances until defensive missiles arrive
  And time advances until fireballs reach peak radius
  Then the destroyable target is not destroyed
  And there are 1 fireballs

Scenario: Defensive missiles fireballs 05b far target survives on the right
  Given a new game with width 800 and height 600
  And the player starts the game
  And the player aims at 400 200
  And the player fires the center battery
  And a destroyable target at 750 50
  When time advances until defensive missiles arrive
  And time advances until fireballs reach peak radius
  Then the destroyable target is not destroyed
  And there are 1 fireballs

Scenario: Defensive missiles fireballs 06 large time steps are clamped
  Given a new game with width <width> and height <height>
  And the player starts the game
  And the player aims at <aim_x> <aim_y>
  And the player fires the <battery> battery
  When time advances by 1.0 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the last applied time step is at most 0.05 seconds
  And there are <missile_count> defensive missiles in flight
  And a defensive missile from the <battery> battery has not reached <expected_x> <expected_y>

Examples:
  | width | height | aim_x | aim_y | expected_width | expected_height | expected_x | expected_y | battery | missile_count |
  | 800   | 600    | 400   | 100   | 800            | 600             | 400        | 100        | center  | 1             |
  | 800   | 600    | 400   | 100   | 800            | 600             | 400        | 100        | left    | 1             |
  | 1920  | 1080   | 960   | 200   | 1440           | 1080            | 960        | 200        | right   | 1             |

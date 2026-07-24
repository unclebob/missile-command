# mutation-stamp: sha256=e3f91c2805d34ca09a2f9d6556960d5b27bc23e3094ccab4f6e30b3ae5dc1c53
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-24T17:46:16.090612Z","feature_name":"Enemy missiles impacts","feature_path":"features/enemy-missiles-impacts.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:7906a43dc441fdda4be3819de63d9dac5f61b185d8b4fc083afd05fdcdd7ff56","scenarios":[{"index":1,"name":"Enemy missiles impacts 02 unintercepted enemy destroys a city","scenario_hash":"93f7e18f397f061be7f948934e42ee1ca9d74ba982bfe2d2d70e0100a167ed45","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-24T17:46:03.658859Z"},{"index":0,"name":"Enemy missiles impacts 01 enemy missile advances toward its target","scenario_hash":"2a0a7f4a12ce6272a22d4d5e5a4a81a3dea704b6eb3bb4a230f75abfafe177cb","mutation_count":21,"result":{"Total":21,"Killed":21,"Survived":0,"Errors":0},"tested_at":"2026-07-24T17:45:14.565982Z"},{"index":8,"name":"Enemy missiles impacts 07 cities are destroyed independently","scenario_hash":"44cda8ee769c7c9bdd25ac3b6afadeacec4109af938adbc291f8c39b32a1d8f7","mutation_count":21,"result":{"Total":21,"Killed":21,"Survived":0,"Errors":0},"tested_at":"2026-07-24T17:44:29.401813Z"}]}
# acceptance-mutation-manifest-end

# Enemy missiles impacts 01 enemy missile advances toward its target
# Enemy missiles impacts 02 unintercepted enemy destroys a city
# Enemy missiles impacts 03 unintercepted enemy destroys a battery
# Enemy missiles impacts 04 enemy is destroyed inside fireball radius
# Enemy missiles impacts 05 enemy outside fireball radius is not destroyed by it
# Enemy missiles impacts 06 destroyed battery cannot fire
# Enemy missiles impacts 07 cities are destroyed independently
Feature: Enemy missiles impacts

Scenario: Enemy missiles impacts 01 enemy missile advances toward its target
  Given a new game with width <width> and height <height>
  And an enemy missile targeting city <city_index>
  When time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <enemy_count> enemy missiles in flight
  And an enemy missile has progressed toward city <expected_city_index>

Examples:
  | width | height | city_index | expected_city_index | expected_width | expected_height | enemy_count |
  | 800   | 600    | 0          | 0                   | 800            | 600             | 1           |
  | 800   | 600    | 3          | 3                   | 800            | 600             | 1           |
  | 1920  | 1080   | 5          | 5                   | 1920           | 1080            | 1           |

Scenario: Enemy missiles impacts 02 unintercepted enemy destroys a city
  Given a new game with width <width> and height <height>
  And an enemy missile targeting city <city_index>
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And city <expected_city_index> is not living
  And there are <living_cities> living cities
  And there are <enemy_count> enemy missiles in flight

Examples:
  | width | height | city_index | expected_city_index | expected_width | expected_height | living_cities | enemy_count |
  | 800   | 600    | 0          | 0                   | 800            | 600             | 5             | 0           |
  | 800   | 600    | 2          | 2                   | 800            | 600             | 5             | 0           |
  | 1920  | 1080   | 5          | 5                   | 1920           | 1080            | 5             | 0           |

Scenario: Enemy missiles impacts 03 unintercepted enemy destroys a battery
  Given a new game with width 800 and height 600
  And an enemy missile targeting battery left
  When time advances until enemy missiles impact or are destroyed
  Then the left battery is destroyed
  And there are 0 enemy missiles in flight

Scenario: Enemy missiles impacts 03b unintercepted enemy destroys center battery
  Given a new game with width 800 and height 600
  And an enemy missile targeting battery center
  When time advances until enemy missiles impact or are destroyed
  Then the center battery is destroyed
  And there are 0 enemy missiles in flight

Scenario: Enemy missiles impacts 03c unintercepted enemy destroys right battery
  Given a new game with width 800 and height 600
  And an enemy missile targeting battery right
  When time advances until enemy missiles impact or are destroyed
  Then the right battery is destroyed
  And there are 0 enemy missiles in flight

Scenario: Enemy missiles impacts 04 enemy is destroyed inside fireball radius
  Given a new game with width 800 and height 600
  And an enemy missile targeting city 1
  And a fireball at 400 250 with radius 40
  And the enemy missile path passes within distance 40 of that fireball center
  When time advances until the enemy missile is inside the fireball radius or has impacted
  Then the enemy missile is destroyed by the fireball
  And there are 0 enemy missiles in flight
  And city 1 is living
  And there are 6 living cities

Scenario: Enemy missiles impacts 05 enemy outside fireball radius is not destroyed by it
  Given a new game with width 800 and height 600
  And an enemy missile targeting city 0
  And a fireball at 750 50 with radius 20
  And the enemy missile path stays farther than 20 from that fireball center
  When time advances until enemy missiles impact or are destroyed
  Then city 0 is not living
  And there are 5 living cities
  And there are 0 enemy missiles in flight

Scenario: Enemy missiles impacts 06 destroyed battery cannot fire
  Given a new game with width 800 and height 600
  And an enemy missile targeting battery left
  When time advances until enemy missiles impact or are destroyed
  And the player aims at 400 100
  And the player fires the left battery
  Then there are 0 defensive missiles in flight
  And the left battery is destroyed

Scenario: Enemy missiles impacts 07 cities are destroyed independently
  Given a new game with width <width> and height <height>
  And <spawn_count> enemy missiles each targeting a different living city
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <living_cities> living cities
  And there are <enemy_count> enemy missiles in flight

Examples:
  | width | height | spawn_count | expected_width | expected_height | living_cities | enemy_count |
  | 800   | 600    | 2           | 800            | 600             | 4             | 0           |
  | 800   | 600    | 3           | 800            | 600             | 3             | 0           |
  | 1920  | 1080   | 2           | 1920           | 1080            | 4             | 0           |

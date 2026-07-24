# Enemy missiles impacts 01 enemy missile advances toward its target
# Enemy missiles impacts 02 unintercepted enemy destroys a city
# Enemy missiles impacts 03 unintercepted enemy destroys a battery
# Enemy missiles impacts 04 enemy is destroyed inside fireball radius
# Enemy missiles impacts 05 enemy outside fireball radius is not destroyed by it
# Enemy missiles impacts 06 destroyed battery cannot fire
# Enemy missiles impacts 07 cities are destroyed independently
Feature: Enemy missiles impacts

Background:
  Given a new game with width <width> and height <height>

Scenario: Enemy missiles impacts 01 enemy missile advances toward its target
  Given an enemy missile targeting city <city_index>
  When time advances by <dt> seconds
  Then there are <enemy_count> enemy missiles in flight
  And an enemy missile has progressed toward city <city_index>

Examples:
  | width | height | city_index | dt  | enemy_count |
  | 800   | 600    | 0          | 0.1 | 1           |
  | 800   | 600    | 3          | 0.1 | 1           |
  | 1920  | 1080   | 5          | 0.05| 1           |

Scenario: Enemy missiles impacts 02 unintercepted enemy destroys a city
  Given an enemy missile targeting city <city_index>
  When time advances until enemy missiles impact or are destroyed
  Then city <city_index> is not living
  And there are <living_cities> living cities
  And there are <enemy_count> enemy missiles in flight

Examples:
  | width | height | city_index | living_cities | enemy_count |
  | 800   | 600    | 0          | 5             | 0           |
  | 800   | 600    | 2          | 5             | 0           |
  | 1920  | 1080   | 5          | 5             | 0           |

Scenario: Enemy missiles impacts 03 unintercepted enemy destroys a battery
  Given an enemy missile targeting battery <battery>
  When time advances until enemy missiles impact or are destroyed
  Then the <battery> battery is destroyed
  And there are <enemy_count> enemy missiles in flight

Examples:
  | width | height | battery | enemy_count |
  | 800   | 600    | left    | 0           |
  | 800   | 600    | center  | 0           |
  | 800   | 600    | right   | 0           |

Scenario: Enemy missiles impacts 04 enemy is destroyed inside fireball radius
  Given an enemy missile targeting city <city_index>
  And a fireball at <fireball_x> <fireball_y> with radius <radius>
  And the enemy missile path passes within distance <radius> of that fireball center
  When time advances until the enemy missile is inside the fireball radius or has impacted
  Then the enemy missile is destroyed by the fireball
  And there are <enemy_count> enemy missiles in flight
  And city <city_index> is living
  And there are <living_cities> living cities

Examples:
  | width | height | city_index | fireball_x | fireball_y | radius | enemy_count | living_cities |
  | 800   | 600    | 1          | 400        | 250        | 40     | 0           | 6             |
  | 800   | 600    | 4          | 500        | 200        | 50     | 0           | 6             |
  | 1920  | 1080   | 0          | 300        | 400        | 60     | 0           | 6             |

Scenario: Enemy missiles impacts 05 enemy outside fireball radius is not destroyed by it
  Given an enemy missile targeting city <city_index>
  And a fireball at <fireball_x> <fireball_y> with radius <radius>
  And the enemy missile path stays farther than <radius> from that fireball center
  When time advances until enemy missiles impact or are destroyed
  Then city <city_index> is not living
  And there are <living_cities> living cities
  And there are <enemy_count> enemy missiles in flight

Examples:
  | width | height | city_index | fireball_x | fireball_y | radius | living_cities | enemy_count |
  | 800   | 600    | 0          | 750        | 50         | 20     | 5             | 0           |
  | 800   | 600    | 2          | 50         | 50         | 15     | 5             | 0           |
  | 1920  | 1080   | 5          | 100        | 100        | 25     | 5             | 0           |

Scenario: Enemy missiles impacts 06 destroyed battery cannot fire
  Given an enemy missile targeting battery <battery>
  When time advances until enemy missiles impact or are destroyed
  And the player aims at <aim_x> <aim_y>
  And the player fires the <battery> battery
  Then there are <missile_count> defensive missiles in flight
  And the <battery> battery is destroyed

Examples:
  | width | height | battery | aim_x | aim_y | missile_count |
  | 800   | 600    | left    | 400   | 100   | 0             |
  | 800   | 600    | center  | 400   | 100   | 0             |
  | 800   | 600    | right   | 400   | 100   | 0             |

Scenario: Enemy missiles impacts 07 cities are destroyed independently
  Given <spawn_count> enemy missiles each targeting a different living city
  When time advances until enemy missiles impact or are destroyed
  Then there are <living_cities> living cities
  And there are <enemy_count> enemy missiles in flight

Examples:
  | width | height | spawn_count | living_cities | enemy_count |
  | 800   | 600    | 2           | 4             | 0           |
  | 800   | 600    | 3           | 3             | 0           |
  | 1920  | 1080   | 2           | 4             | 0           |

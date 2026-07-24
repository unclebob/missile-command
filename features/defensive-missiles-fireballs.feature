# Defensive missiles fireballs 01 missile advances toward the aim point
# Defensive missiles fireballs 02 missile becomes a fireball on arrival
# Defensive missiles fireballs 03 fireball expands then contracts then disappears
# Defensive missiles fireballs 04 destroyable target inside fireball is destroyed
# Defensive missiles fireballs 05 destroyable target outside fireball survives
# Defensive missiles fireballs 06 large time steps are clamped
Feature: Defensive missiles and fireballs

Background:
  Given a new game with width <width> and height <height>
  And the player aims at <aim_x> <aim_y>
  And the player fires the <battery> battery

Scenario: Defensive missiles fireballs 01 missile advances toward the aim point
  When time advances by <dt> seconds
  Then there are <missile_count> defensive missiles in flight
  And a defensive missile from the <battery> battery has progressed toward <aim_x> <aim_y>

Examples:
  | width | height | aim_x | aim_y | battery | dt  | missile_count |
  | 800   | 600    | 400   | 100   | left    | 0.1 | 1             |
  | 800   | 600    | 400   | 100   | center  | 0.1 | 1             |
  | 800   | 600    | 400   | 100   | right   | 0.1 | 1             |
  | 1920  | 1080   | 960   | 200   | center  | 0.05| 1             |

Scenario: Defensive missiles fireballs 02 missile becomes a fireball on arrival
  When time advances until defensive missiles arrive
  Then there are <missile_count> defensive missiles in flight
  And there are <fireball_count> fireballs
  And a fireball is centered at <aim_x> <aim_y>

Examples:
  | width | height | aim_x | aim_y | battery | missile_count | fireball_count |
  | 800   | 600    | 400   | 100   | left    | 0             | 1              |
  | 800   | 600    | 400   | 100   | center  | 0             | 1              |
  | 800   | 600    | 400   | 100   | right   | 0             | 1              |
  | 1920  | 1080   | 960   | 200   | center  | 0             | 1              |

Scenario: Defensive missiles fireballs 03 fireball expands then contracts then disappears
  When time advances until defensive missiles arrive
  Then there is an active fireball
  And the fireball start time is recorded
  When time advances until the fireball reaches max radius
  Then the fireball max time is at least the fireball start time
  And a fireball radius is greater than <min_radius>
  When time advances into the fireball shrink phase
  Then the fireball shrink time is at least the fireball max time
  And a fireball radius is less than the max fireball radius
  When time advances until fireballs expire
  Then there are <fireball_count> fireballs
  And the fireball end time is at least the fireball shrink time

Examples:
  | width | height | aim_x | aim_y | battery | min_radius | fireball_count |
  | 800   | 600    | 400   | 100   | center  | 1          | 0              |
  | 1920  | 1080   | 960   | 200   | left    | 1          | 0              |

Scenario: Defensive missiles fireballs 04 destroyable target inside fireball is destroyed
  Given a destroyable target at <target_x> <target_y>
  When time advances until defensive missiles arrive
  And time advances until fireballs reach at least radius <min_blast_radius>
  Then the destroyable target is destroyed
  And there are <fireball_count> fireballs

Examples:
  | width | height | aim_x | aim_y | battery | target_x | target_y | min_blast_radius | fireball_count |
  | 800   | 600    | 400   | 200   | center  | 400      | 200      | 1                | 1              |
  | 800   | 600    | 400   | 200   | center  | 405      | 200      | 10               | 1              |
  | 1920  | 1080   | 960   | 300   | left    | 960      | 300      | 1                | 1              |

Scenario: Defensive missiles fireballs 05 destroyable target outside fireball survives
  Given a destroyable target at <target_x> <target_y>
  When time advances until defensive missiles arrive
  And time advances until fireballs reach peak radius
  Then the destroyable target is not destroyed
  And there are <fireball_count> fireballs

Examples:
  | width | height | aim_x | aim_y | battery | target_x | target_y | fireball_count |
  | 800   | 600    | 400   | 200   | center  | 50       | 50       | 1              |
  | 800   | 600    | 400   | 200   | center  | 750      | 50       | 1              |
  | 1920  | 1080   | 960   | 300   | right   | 100      | 100      | 1              |

Scenario: Defensive missiles fireballs 06 large time steps are clamped
  When time advances by <dt> seconds
  Then the last applied time step is at most <max_dt> seconds
  And there are <missile_count> defensive missiles in flight
  And a defensive missile from the <battery> battery has not reached <aim_x> <aim_y>

Examples:
  | width | height | aim_x | aim_y | battery | dt  | max_dt | missile_count |
  | 800   | 600    | 400   | 100   | center  | 1.0 | 0.05   | 1             |
  | 800   | 600    | 400   | 100   | left    | 5.0 | 0.05   | 1             |
  | 1920  | 1080   | 960   | 200   | right   | 2.0 | 0.05   | 1             |

# Fire batteries keys 01 stocked battery launches toward crosshair
# Fire batteries keys 02 firing one battery leaves others full
# Fire batteries keys 03 empty battery does not fire
# Fire batteries keys 04 destroyed battery does not fire
# Fire batteries keys 05 center missile is faster than side missiles
Feature: Fire batteries with keys

Background:
  Given a new game with width <width> and height <height>
  And the player aims at <aim_x> <aim_y>

Scenario: Fire batteries keys 01 stocked battery launches toward crosshair
  When the player fires the <battery> battery
  Then the <battery> battery has <ammo> missiles
  And there are <missile_count> defensive missiles in flight
  And a defensive missile from the <battery> battery targets <aim_x> <aim_y>

Examples:
  | width | height | aim_x | aim_y | battery | ammo | missile_count |
  | 800   | 600    | 400   | 200   | left    | 9    | 1             |
  | 800   | 600    | 400   | 200   | center  | 9    | 1             |
  | 800   | 600    | 400   | 200   | right   | 9    | 1             |
  | 1920  | 1080   | 960   | 300   | left    | 9    | 1             |
  | 1920  | 1080   | 960   | 300   | center  | 9    | 1             |
  | 1920  | 1080   | 960   | 300   | right   | 9    | 1             |

Scenario: Fire batteries keys 02 firing one battery leaves others full
  When the player fires the <battery> battery
  Then the <battery> battery has <ammo> missiles
  And every other battery has <full_ammo> missiles

Examples:
  | width | height | aim_x | aim_y | battery | ammo | full_ammo |
  | 800   | 600    | 400   | 200   | left    | 9    | 10        |
  | 800   | 600    | 400   | 200   | center  | 9    | 10        |
  | 800   | 600    | 400   | 200   | right   | 9    | 10        |

Scenario: Fire batteries keys 03 empty battery does not fire
  Given the <battery> battery ammo is set to <ammo>
  When the player fires the <battery> battery
  Then the <battery> battery has <ammo> missiles
  And there are <missile_count> defensive missiles in flight

Examples:
  | width | height | aim_x | aim_y | battery | ammo | missile_count |
  | 800   | 600    | 400   | 200   | left    | 0    | 0             |
  | 800   | 600    | 400   | 200   | center  | 0    | 0             |
  | 800   | 600    | 400   | 200   | right   | 0    | 0             |

Scenario: Fire batteries keys 04 destroyed battery does not fire
  Given the <battery> battery is destroyed
  When the player fires the <battery> battery
  Then there are <missile_count> defensive missiles in flight
  And the <battery> battery has <ammo> missiles

Examples:
  | width | height | aim_x | aim_y | battery | missile_count | ammo |
  | 800   | 600    | 400   | 200   | left    | 0             | 10   |
  | 800   | 600    | 400   | 200   | center  | 0             | 10   |
  | 800   | 600    | 400   | 200   | right   | 0             | 10   |

Scenario: Fire batteries keys 05 center missile is faster than side missiles
  When the player fires every battery once
  Then the center defensive missile is faster than each side defensive missile
  And there are <missile_count> defensive missiles in flight

Examples:
  | width | height | aim_x | aim_y | missile_count |
  | 800   | 600    | 400   | 100   | 3             |
  | 1920  | 1080   | 960   | 200   | 3             |

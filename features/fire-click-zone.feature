# Fire click zone 01 click fires battery for horizontal third
# Fire click zone 02 empty batteries fall back along the zone order
# Fire click zone 03 destroyed batteries fall back along the zone order
# Fire click zone 04 no battery can fire yields no missile
# Fire click zone 05 key fire remains available with click fire
# Fire click zone 06 zone mapping uses width after resize
Feature: Fire by click zone

Background:
  Given a new game with width <width> and height <height>

Scenario: Fire click zone 01 click fires battery for horizontal third
  When the player clicks at <x> <y>
  Then the crosshair is at <expected_x> <expected_y>
  And a defensive missile from the <battery> battery targets <expected_x> <expected_y>
  And the <battery> battery has <ammo> missiles
  And there are <missile_count> defensive missiles in flight

Examples:
  | width | height | x    | y   | expected_x | expected_y | battery | ammo | missile_count |
  | 900   | 600    | 0    | 100 | 0          | 100        | left    | 9    | 1             |
  | 900   | 600    | 299  | 100 | 299        | 100        | left    | 9    | 1             |
  | 900   | 600    | 300  | 100 | 300        | 100        | center  | 9    | 1             |
  | 900   | 600    | 599  | 100 | 599        | 100        | center  | 9    | 1             |
  | 900   | 600    | 600  | 100 | 600        | 100        | right   | 9    | 1             |
  | 900   | 600    | 899  | 100 | 899        | 100        | right   | 9    | 1             |
  | 1920  | 1080   | 0    | 200 | 0          | 200        | left    | 9    | 1             |
  | 1920  | 1080   | 639  | 200 | 639        | 200        | left    | 9    | 1             |
  | 1920  | 1080   | 640  | 200 | 640        | 200        | center  | 9    | 1             |
  | 1920  | 1080   | 1279 | 200 | 1279       | 200        | center  | 9    | 1             |
  | 1920  | 1080   | 1280 | 200 | 1280       | 200        | right   | 9    | 1             |
  | 1920  | 1080   | 1919 | 200 | 1919       | 200        | right   | 9    | 1             |

Scenario: Fire click zone 02 empty batteries fall back along the zone order
  Given the click must fall back to the <battery> battery because earlier batteries are empty
  When the player clicks at <x> <y>
  Then the crosshair is at <expected_x> <expected_y>
  And a defensive missile from the <battery> battery targets <expected_x> <expected_y>
  And the <battery> battery has <ammo> missiles
  And there are <missile_count> defensive missiles in flight

Examples:
  | width | height | x   | y   | expected_x | expected_y | battery | ammo | missile_count |
  | 900   | 600    | 100 | 100 | 100        | 100        | center  | 9    | 1             |
  | 900   | 600    | 100 | 100 | 100        | 100        | right   | 9    | 1             |
  | 900   | 600    | 800 | 100 | 800        | 100        | center  | 9    | 1             |
  | 900   | 600    | 800 | 100 | 800        | 100        | left    | 9    | 1             |
  | 900   | 600    | 450 | 100 | 450        | 100        | left    | 9    | 1             |
  | 900   | 600    | 450 | 100 | 450        | 100        | right   | 9    | 1             |

Scenario: Fire click zone 03 destroyed batteries fall back along the zone order
  Given the click must fall back to the <battery> battery because earlier batteries are destroyed
  When the player clicks at <x> <y>
  Then the crosshair is at <expected_x> <expected_y>
  And a defensive missile from the <battery> battery targets <expected_x> <expected_y>
  And the <battery> battery has <ammo> missiles
  And there are <missile_count> defensive missiles in flight

Examples:
  | width | height | x   | y   | expected_x | expected_y | battery | ammo | missile_count |
  | 900   | 600    | 100 | 100 | 100        | 100        | center  | 9    | 1             |
  | 900   | 600    | 100 | 100 | 100        | 100        | right   | 9    | 1             |
  | 900   | 600    | 800 | 100 | 800        | 100        | center  | 9    | 1             |
  | 900   | 600    | 800 | 100 | 800        | 100        | left    | 9    | 1             |
  | 900   | 600    | 450 | 100 | 450        | 100        | left    | 9    | 1             |
  | 900   | 600    | 450 | 100 | 450        | 100        | right   | 9    | 1             |

Scenario: Fire click zone 04 no battery can fire yields no missile
  Given no battery can fire
  When the player clicks at <x> <y>
  Then there are <missile_count> defensive missiles in flight
  And the crosshair is at <expected_x> <expected_y>

Examples:
  | width | height | x   | y   | expected_x | expected_y | missile_count |
  | 900   | 600    | 100 | 100 | 100        | 100        | 0             |
  | 900   | 600    | 450 | 100 | 450        | 100        | 0             |
  | 900   | 600    | 800 | 100 | 800        | 100        | 0             |

Scenario: Fire click zone 05 key fire remains available with click fire
  When the player clicks at <x> <y>
  And the player aims at <expected_x> <expected_y>
  And the player fires the <battery> battery
  Then there are <missile_count> defensive missiles in flight
  And the <battery> battery has <ammo> missiles
  And a defensive missile from the <battery> battery targets <expected_x> <expected_y>

Examples:
  | width | height | x   | y   | expected_x | expected_y | battery | missile_count | ammo |
  | 900   | 600    | 100 | 150 | 200        | 120        | right   | 2             | 9    |
  | 900   | 600    | 800 | 150 | 100        | 120        | left    | 2             | 9    |
  | 900   | 600    | 100 | 150 | 450        | 120        | center  | 2             | 9    |

Scenario: Fire click zone 06 zone mapping uses width after resize
  When the playfield is resized to width <new_width> and height <new_height>
  And the player clicks at <x> <y>
  Then a defensive missile from the <battery> battery targets <expected_x> <expected_y>
  And the <battery> battery has <ammo> missiles

Examples:
  | width | height | new_width | new_height | x    | y   | expected_x | expected_y | battery | ammo |
  | 900   | 600    | 1800      | 600        | 500  | 100 | 500        | 100        | left    | 9    |
  | 900   | 600    | 1800      | 600        | 900  | 100 | 900        | 100        | center  | 9    |
  | 900   | 600    | 1800      | 600        | 1500 | 100 | 1500       | 100        | right   | 9    |
  | 1800  | 600    | 900       | 600        | 450  | 100 | 450        | 100        | center  | 9    |

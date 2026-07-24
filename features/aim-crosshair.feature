# Aim crosshair 01 aim inside the playfield
# Aim crosshair 02 aim outside clamps to the playfield
# Aim crosshair 03 aim does not change forces or score
Feature: Aim crosshair

Background:
  Given a new game with width <width> and height <height>

Scenario: Aim crosshair 01 aim inside the playfield
  When the player aims at <x> <y>
  Then the crosshair is at <expected_x> <expected_y>

Examples:
  | width | height | x    | y    | expected_x | expected_y |
  | 800   | 600    | 100  | 200  | 100        | 200        |
  | 800   | 600    | 400  | 300  | 400        | 300        |
  | 1920  | 1080   | 960  | 540  | 960        | 540        |
  | 1024  | 768    | 0    | 0    | 0          | 0          |

Scenario: Aim crosshair 02 aim outside clamps to the playfield
  When the player aims at <x> <y>
  Then the crosshair is at <expected_x> <expected_y>

Examples:
  | width | height | x     | y     | expected_x | expected_y |
  | 800   | 600    | -10   | 100   | 0          | 100        |
  | 800   | 600    | 900   | 100   | 799        | 100        |
  | 800   | 600    | 100   | -5    | 100        | 0          |
  | 800   | 600    | 100   | 700   | 100        | 599        |
  | 800   | 600    | -1    | -1    | 0          | 0          |
  | 800   | 600    | 9999  | 9999  | 799        | 599        |
  | 1920  | 1080   | 2000  | 500   | 1919       | 500        |
  | 1920  | 1080   | 100   | 2000  | 100        | 1079       |

Scenario: Aim crosshair 03 aim does not change forces or score
  When the player aims at <x> <y>
  Then there are <city_count> living cities
  And each battery has <ammo> missiles
  And the score is <score>
  And the crosshair is at <expected_x> <expected_y>

Examples:
  | width | height | x   | y   | expected_x | expected_y | city_count | ammo | score |
  | 800   | 600    | 250 | 150 | 250        | 150        | 6          | 10   | 0     |
  | 1920  | 1080   | 10  | 20  | 10         | 20         | 6          | 10   | 0     |

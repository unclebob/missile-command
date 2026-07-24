# Enemy missile angles 01 angled enemy advances toward its target
# Enemy missile angles 02 unintercepted angled enemy destroys a city
# Enemy missile angles 03 unintercepted angled enemy destroys a battery
# Enemy missile angles 04 wave enemies enter from varied sky origins
Feature: Enemy missile angles

Background:
  Given a new game with width <width> and height <height>

Scenario: Enemy missile angles 01 angled enemy advances toward its target
  And an enemy missile from <origin_x> 0 targeting city <city_index>
  When time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 1 enemy missiles in flight
  And the first enemy missile origin is <origin_x> 0
  And the first enemy missile origin x differs from its target x
  And an enemy missile has progressed toward city <city_index>
  And the first enemy missile has moved toward its target on both axes

Examples:
  | width | height | origin_x | city_index | expected_width | expected_height |
  | 800   | 600    | 50       | 0          | 800            | 600             |
  | 800   | 600    | 750      | 0          | 800            | 600             |
  | 800   | 600    | 100      | 5          | 800            | 600             |
  | 1920  | 1080   | 200      | 3          | 1920           | 1080            |

Scenario: Enemy missile angles 02 unintercepted angled enemy destroys a city
  And an enemy missile from <origin_x> 0 targeting city <city_index>
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And city <city_index> is not living
  And there are <living_cities> living cities
  And there are 0 enemy missiles in flight

Examples:
  | width | height | origin_x | city_index | living_cities | expected_width | expected_height |
  | 800   | 600    | 50       | 0          | 5             | 800            | 600             |
  | 800   | 600    | 700      | 2          | 5             | 800            | 600             |
  | 1920  | 1080   | 100      | 5          | 5             | 1920           | 1080            |

Scenario: Enemy missile angles 03 unintercepted angled enemy destroys a battery
  And an enemy missile from <origin_x> 0 targeting battery <battery>
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <battery> battery is destroyed
  And there are 0 enemy missiles in flight

Examples:
  | width | height | origin_x | battery | expected_width | expected_height |
  | 800   | 600    | 200      | left    | 800            | 600             |
  | 800   | 600    | 100      | center  | 800            | 600             |
  | 800   | 600    | 400      | right   | 800            | 600             |
  | 1920  | 1080   | 50       | left    | 1920           | 1080            |

Scenario: Enemy missile angles 04 wave enemies enter from varied sky origins
  And the current wave has <remaining> scheduled enemies still active
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <remaining> enemy missiles in flight
  And every enemy missile origin y is 0
  And every enemy missile origin x is within the playfield
  And the enemy missiles use more than one distinct origin x
  And at least one enemy missile origin x differs from its target x

Examples:
  | width | height | remaining | expected_width | expected_height |
  | 800   | 600    | 3         | 800            | 600             |
  | 800   | 600    | 4         | 800            | 600             |
  | 1920  | 1080   | 3         | 1920           | 1080            |

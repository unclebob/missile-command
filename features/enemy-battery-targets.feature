# Enemy battery targets 01 wave enemies choose among cities and batteries
# Enemy battery targets 02 an unintercepted wave enemy destroys its battery target
# Enemy battery targets 03 a full target sweep includes every battery and city
# Enemy battery targets 04 destroyed batteries are not chosen as wave targets
Feature: Enemy battery targets

Background:
  Given a new game with width <width> and height <height>

Scenario: Enemy battery targets 01 wave enemies choose among cities and batteries
  And the current wave has <remaining> scheduled enemies still active
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <remaining> enemy missiles in flight
  And at least one enemy missile targets a city
  And at least one enemy missile targets a battery

Examples:
  | width | height | remaining | expected_width | expected_height |
  | 800   | 600    | 9         | 800            | 600             |
  | 800   | 600    | 12        | 800            | 600             |
  | 1920  | 1080   | 9         | 1920           | 1080            |

Scenario: Enemy battery targets 02 an unintercepted wave enemy destroys its battery target
  And a wave enemy missile targeting battery <battery>
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <battery> battery is destroyed
  And there are 0 enemy missiles in flight
  And there are <living_cities> living cities

Examples:
  | width | height | battery | living_cities | expected_width | expected_height |
  | 800   | 600    | left    | 6             | 800            | 600             |
  | 800   | 600    | center  | 6             | 800            | 600             |
  | 800   | 600    | right   | 6             | 800            | 600             |
  | 1920  | 1080   | left    | 6             | 1920           | 1080            |

Scenario: Enemy battery targets 03 a full target sweep includes every battery and city
  And the current wave has <remaining> scheduled enemies still active
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <remaining> enemy missiles in flight
  And enemy missile targets include every living city
  And enemy missile targets include every non-destroyed battery

Examples:
  | width | height | remaining | expected_width | expected_height |
  | 800   | 600    | 9         | 800            | 600             |
  | 1920  | 1080   | 9         | 1920           | 1080            |

Scenario: Enemy battery targets 04 destroyed batteries are not chosen as wave targets
  And the <battery> battery has been destroyed
  And the current wave has <remaining> scheduled enemies still active
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <remaining> enemy missiles in flight
  And no enemy missile targets battery <battery>
  And every enemy missile targets a living city or a non-destroyed battery

Examples:
  | width | height | battery | remaining | expected_width | expected_height |
  | 800   | 600    | left    | 8         | 800            | 600             |
  | 800   | 600    | center  | 8         | 800            | 600             |
  | 800   | 600    | right   | 8         | 800            | 600             |
  | 1920  | 1080   | left    | 8         | 1920           | 1080            |

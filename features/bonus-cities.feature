# Bonus cities 01 new game has empty bonus city reserve
# Bonus cities 02 crossing the score threshold awards a reserve city
# Bonus cities 03 each crossed threshold awards one reserve city
# Bonus cities 04 earning a bonus does not restore cities mid-wave
# Bonus cities 05 multiple threshold awards stay in reserve until wave end
# Bonus cities 06 remaining reserve restores after wave resolution
# Bonus cities 07 bonus city earned event is recorded
Feature: Bonus cities

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game
  And the bonus city threshold is <threshold>

Scenario: Bonus cities 01 new game has empty bonus city reserve
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <score>
  And the bonus city reserve is <reserve>
  And there are <living_cities> living cities

Examples:
  | width | height | threshold | score | reserve | living_cities | expected_width | expected_height |
  | 800   | 600    | 10000     | 0     | 0       | 6             | 800            | 600             |
  | 1920  | 1080   | 10000     | 0     | 0       | 6             | 1920           | 1080            |

Scenario: Bonus cities 02 crossing the score threshold awards a reserve city
  When the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <score>
  And the bonus city reserve is <reserve>
  And there are <living_cities> living cities

Examples:
  | width | height | threshold | score | reserve | living_cities | expected_width | expected_height |
  | 800   | 600    | 10000     | 9999  | 0       | 6             | 800            | 600             |
  | 800   | 600    | 10000     | 10000 | 1       | 6             | 800            | 600             |
  | 800   | 600    | 10000     | 10001 | 1       | 6             | 800            | 600             |
  | 1920  | 1080   | 5000      | 5000  | 1       | 6             | 1920           | 1080            |

Scenario: Bonus cities 03 each crossed threshold awards one reserve city
  When the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <score>
  And the bonus city reserve is <reserve>
  And there are <living_cities> living cities

Examples:
  | width | height | threshold | score | reserve | living_cities | expected_width | expected_height |
  | 800   | 600    | 10000     | 20000 | 2       | 6             | 800            | 600             |
  | 800   | 600    | 10000     | 29999 | 2       | 6             | 800            | 600             |
  | 800   | 600    | 10000     | 30000 | 3       | 6             | 800            | 600             |
  | 1920  | 1080   | 10000     | 40000 | 4       | 6             | 1920           | 1080            |

Scenario: Bonus cities 04 earning a bonus does not restore cities mid-wave
  And city <destroyed_city> has been destroyed
  And there are <living_cities> living cities
  When the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <score>
  And there are <living_cities> living cities
  And city <destroyed_city> is not living
  And the bonus city reserve is <reserve>

Examples:
  | width | height | threshold | destroyed_city | living_cities | score | reserve | expected_width | expected_height |
  | 800   | 600    | 10000     | 0              | 5             | 10000 | 1       | 800            | 600             |
  | 800   | 600    | 10000     | 3              | 5             | 10000 | 1       | 800            | 600             |
  | 1920  | 1080   | 10000     | 5              | 5             | 10000 | 1       | 1920           | 1080            |

Scenario: Bonus cities 05 multiple threshold awards stay in reserve until wave end
  And city 0 has been destroyed
  And city 1 has been destroyed
  And there are 4 living cities
  When the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <living_cities> living cities
  And the bonus city reserve is <reserve>

Examples:
  | width | height | threshold | score | living_cities | reserve | expected_width | expected_height |
  | 800   | 600    | 10000     | 10000 | 4             | 1       | 800            | 600             |
  | 800   | 600    | 10000     | 20000 | 4             | 2       | 800            | 600             |
  | 800   | 600    | 10000     | 30000 | 4             | 3       | 800            | 600             |
  | 1920  | 1080   | 10000     | 30000 | 4             | 3       | 1920           | 1080            |

Scenario: Bonus cities 06 remaining reserve restores after wave resolution
  And city 0 has been destroyed
  And city 1 has been destroyed
  And the bonus city reserve is set to <starting_reserve>
  And there are 4 living cities
  When bonus cities from reserve are applied after wave resolution
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <living_cities> living cities
  And the bonus city reserve is <reserve>

Examples:
  | width | height | threshold | starting_reserve | living_cities | reserve | expected_width | expected_height |
  | 800   | 600    | 10000     | 1                | 5             | 0       | 800            | 600             |
  | 800   | 600    | 10000     | 2                | 6             | 0       | 800            | 600             |
  | 800   | 600    | 10000     | 3                | 6             | 1       | 800            | 600             |
  | 1920  | 1080   | 10000     | 2                | 6             | 0       | 1920           | 1080            |

Scenario: Bonus cities 07 bonus city earned event is recorded
  When the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the number of bonus city earned events is <event_count>
  And the bonus city reserve is <reserve>

Examples:
  | width | height | threshold | score | event_count | reserve | expected_width | expected_height |
  | 800   | 600    | 10000     | 9999  | 0           | 0       | 800            | 600             |
  | 800   | 600    | 10000     | 10000 | 1           | 1       | 800            | 600             |
  | 800   | 600    | 10000     | 30000 | 3           | 3       | 800            | 600             |
  | 1920  | 1080   | 10000     | 20000 | 2           | 2       | 1920           | 1080            |

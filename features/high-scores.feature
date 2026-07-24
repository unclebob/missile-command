# High scores 01 a non qualifying score does not open initials entry
# High scores 02 a qualifying score opens initials entry after THE END
# High scores 03 submitting initials inserts the score in ranked order
# High scores 04 the table is capped at the maximum entries
# High scores 05 initials are three characters from the allowed set
# High scores 06 high scores can be viewed from the title screen
# High scores 07 after entry the screen returns to title
Feature: High scores

Background:
  Given a new game with width <width> and height <height>
  And the high score table capacity is <capacity>

Scenario: High scores 01 a non qualifying score does not open initials entry
  And a high score entry initials AAA with score 1000
  And a high score entry initials BBB with score 900
  And a high score entry initials CCC with score 800
  And the score becomes <score>
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title
  And the screen is not high-score-entry

Examples:
  | width | height | capacity | score | expected_width | expected_height |
  | 800   | 600    | 3        | 700   | 800            | 600             |
  | 800   | 600    | 3        | 0     | 800            | 600             |
  | 1920  | 1080   | 3        | 799   | 1920           | 1080            |

Scenario: High scores 02 a qualifying score opens initials entry after THE END
  And a high score entry initials AAA with score 1000
  And a high score entry initials BBB with score 900
  And the score becomes <score>
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is high-score-entry
  And the pending high score is <score>

Examples:
  | width | height | capacity | score | expected_width | expected_height |
  | 800   | 600    | 10       | 500   | 800            | 600             |
  | 800   | 600    | 3        | 950   | 800            | 600             |
  | 800   | 600    | 3        | 1000  | 800            | 600             |
  | 1920  | 1080   | 10       | 1     | 1920           | 1080            |

Scenario: High scores 03 submitting initials inserts the score in ranked order
  And a high score entry initials AAA with score 1000
  And a high score entry initials CCC with score 500
  And the score becomes <score>
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  And the player enters high score initials <initials>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the high score table is ordered by score descending
  And the high score at rank <rank> has initials <initials> and score <score>
  And the high score table has <entry_count> entries

Examples:
  | width | height | capacity | score | initials | rank | entry_count | expected_width | expected_height |
  | 800   | 600    | 10       | 750   | BOB      | 2    | 3           | 800            | 600             |
  | 800   | 600    | 10       | 1200  | ACE      | 1    | 3           | 800            | 600             |
  | 1920  | 1080   | 10       | 600   | ZED      | 2    | 3           | 1920           | 1080            |

Scenario: High scores 04 the table is capped at the maximum entries
  And a high score entry initials AAA with score 1000
  And a high score entry initials BBB with score 900
  And a high score entry initials CCC with score 800
  And the score becomes <score>
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  And the player enters high score initials <initials>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the high score table has <capacity> entries
  And the high score table lowest score is <lowest>
  And the high score table does not include score <dropped>

Examples:
  | width | height | capacity | score | initials | lowest | dropped | expected_width | expected_height |
  | 800   | 600    | 3        | 850   | NEW      | 850    | 800     | 800            | 600             |
  | 800   | 600    | 3        | 950   | TOP      | 900    | 800     | 800            | 600             |
  | 1920  | 1080   | 3        | 801   | LOW      | 801    | 800     | 1920           | 1080            |

Scenario: High scores 05 initials are three characters from the allowed set
  And the score becomes <score>
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  And the player enters high score initials <initials>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the submitted high score initials are <normalized>
  And the submitted high score initials length is <length>

Examples:
  | width | height | capacity | score | initials | normalized | length | expected_width | expected_height |
  | 800   | 600    | 10       | 100   | ABC      | ABC        | 3      | 800            | 600             |
  | 800   | 600    | 10       | 100   | xyz      | XYZ        | 3      | 800            | 600             |
  | 800   | 600    | 10       | 100   | A1B      | A1B        | 3      | 800            | 600             |
  | 1920  | 1080   | 10       | 100   | ZZ9      | ZZ9        | 3      | 1920           | 1080            |

Scenario: High scores 06 high scores can be viewed from the title screen
  And a high score entry initials AAA with score 1000
  And a high score entry initials BBB with score 500
  When the player opens high scores from the title
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is high-scores
  And the high score at rank 1 has initials AAA and score 1000
  And the high score at rank 2 has initials BBB and score 500

Examples:
  | width | height | capacity | expected_width | expected_height |
  | 800   | 600    | 10       | 800            | 600             |
  | 1920  | 1080   | 10       | 1920           | 1080            |

Scenario: High scores 07 after entry the screen returns to title
  And the score becomes <score>
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  And the player enters high score initials <initials>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title

Examples:
  | width | height | capacity | score | initials | expected_width | expected_height |
  | 800   | 600    | 10       | 100   | AAA      | 800            | 600             |
  | 800   | 600    | 10       | 2500  | BOB      | 800            | 600             |
  | 1920  | 1080   | 10       | 50    | ZZZ      | 1920           | 1080            |

# MIRV warheads 01 a MIRV is a single enemy before it splits
# MIRV warheads 02 a MIRV splits into multiple warheads at the split progress
# MIRV warheads 03 child warheads proceed toward independent targets
# MIRV warheads 04 destroying a MIRV before split prevents children
# MIRV warheads 05 an unintercepted child warhead destroys its target city
# MIRV warheads 06 a fireball destroys a child warhead
# MIRV warheads 07 later waves schedule MIRV capable enemies
Feature: MIRV warheads

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: MIRV warheads 01 a MIRV is a single enemy before it splits
  And a MIRV enemy missile targeting city <city_index> that splits into <child_count> warheads at progress <split_progress>
  When time advances by 0.05 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <enemy_count> enemy missiles in flight
  And there is 1 MIRV parent in flight
  And the first enemy missile progress is less than <split_progress>

Examples:
  | width | height | city_index | child_count | split_progress | enemy_count | expected_width | expected_height |
  | 800   | 600    | 0          | 3           | 0.5            | 1           | 800            | 600             |
  | 800   | 600    | 2          | 2           | 0.4            | 1           | 800            | 600             |
  | 1920  | 1080   | 5          | 3           | 0.5            | 1           | 1920           | 1080            |

Scenario: MIRV warheads 02 a MIRV splits into multiple warheads at the split progress
  And a MIRV enemy missile targeting city <city_index> that splits into <child_count> warheads at progress <split_progress>
  When time advances until the MIRV has split or all enemies are gone
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <child_count> enemy missiles in flight
  And there are 0 MIRV parents in flight
  And every in-flight enemy is a MIRV child warhead

Examples:
  | width | height | city_index | child_count | split_progress | expected_width | expected_height |
  | 800   | 600    | 0          | 3           | 0.5            | 800            | 600             |
  | 800   | 600    | 1          | 2           | 0.45           | 800            | 600             |
  | 800   | 600    | 3          | 4           | 0.5            | 800            | 600             |
  | 1920  | 1080   | 5          | 3           | 0.5            | 1920           | 1080            |

Scenario: MIRV warheads 03 child warheads proceed toward independent targets
  And a MIRV enemy missile targeting city <city_index> that splits into <child_count> warheads at progress <split_progress>
  When time advances until the MIRV has split or all enemies are gone
  And time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <child_count> enemy missiles in flight
  And the MIRV child warheads target more than one distinct target
  And every MIRV child warhead has progressed toward its target

Examples:
  | width | height | city_index | child_count | split_progress | expected_width | expected_height |
  | 800   | 600    | 0          | 3           | 0.5            | 800            | 600             |
  | 800   | 600    | 2          | 3           | 0.5            | 800            | 600             |
  | 1920  | 1080   | 1          | 3           | 0.5            | 1920           | 1080            |

Scenario: MIRV warheads 04 destroying a MIRV before split prevents children
  And a MIRV enemy missile targeting city <city_index> that splits into <child_count> warheads at progress <split_progress>
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the enemy missile path passes within distance <radius> of that fireball center
  When time advances until the enemy missile is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the enemy missile is destroyed by the fireball
  And there are 0 enemy missiles in flight
  And there are 0 MIRV parents in flight
  And there are <living_cities> living cities

Examples:
  | width | height | city_index | child_count | split_progress | fb_x | fb_y | radius | living_cities | expected_width | expected_height |
  | 800   | 600    | 1          | 3           | 0.5            | 400  | 100  | 40     | 6             | 800            | 600             |
  | 800   | 600    | 2          | 3           | 0.6            | 400  | 80   | 50     | 6             | 800            | 600             |
  | 1920  | 1080   | 3          | 2           | 0.5            | 960  | 150  | 50     | 6             | 1920           | 1080            |

Scenario: MIRV warheads 05 an unintercepted child warhead destroys its target city
  And a MIRV enemy missile targeting city <city_index> that splits into <child_count> warheads at progress <split_progress>
  When time advances until the MIRV has split or all enemies are gone
  And time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 0 enemy missiles in flight
  And there are <living_cities> living cities

Examples:
  | width | height | city_index | child_count | split_progress | living_cities | expected_width | expected_height |
  | 800   | 600    | 0          | 2           | 0.5            | 4             | 800            | 600             |
  | 800   | 600    | 1          | 3           | 0.5            | 3             | 800            | 600             |
  | 1920  | 1080   | 0          | 2           | 0.5            | 4             | 1920           | 1080            |

Scenario: MIRV warheads 06 a fireball destroys a child warhead
  And a MIRV enemy missile targeting city <city_index> that splits into <child_count> warheads at progress <split_progress>
  When time advances until the MIRV has split or all enemies are gone
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the first MIRV child warhead path passes within distance <radius> of that fireball center
  When time advances until the first MIRV child is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the first MIRV child warhead is destroyed by the fireball
  And there are <remaining_enemies> enemy missiles in flight

Examples:
  | width | height | city_index | child_count | split_progress | fb_x | fb_y | radius | remaining_enemies | expected_width | expected_height |
  | 800   | 600    | 0          | 3           | 0.5            | 400  | 350  | 40     | 2                 | 800            | 600             |
  | 800   | 600    | 1          | 2           | 0.5            | 400  | 350  | 50     | 1                 | 800            | 600             |
  | 1920  | 1080   | 2          | 3           | 0.5            | 960  | 600  | 50     | 2                 | 1920           | 1080            |

Scenario: MIRV warheads 07 later waves schedule MIRV capable enemies
  When the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave <wave> MIRV schedule count is <mirv_count>

Examples:
  | width | height | wave | mirv_count | expected_width | expected_height |
  | 800   | 600    | 1    | 0          | 800            | 600             |
  | 800   | 600    | 2    | 0          | 800            | 600             |
  | 800   | 600    | 4    | 1          | 800            | 600             |
  | 800   | 600    | 6    | 2          | 800            | 600             |
  | 1920  | 1080   | 3    | 0          | 1920           | 1080            |
  | 1920  | 1080   | 5    | 1          | 1920           | 1080            |

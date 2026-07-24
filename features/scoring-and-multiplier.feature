# Scoring and multiplier 01 new game starts with zero score and one times multiplier
# Scoring and multiplier 02 multiplier follows the wave schedule
# Scoring and multiplier 03 destroying an enemy missile awards points times multiplier
# Scoring and multiplier 04 wave end awards unused missiles and cities times multiplier
# Scoring and multiplier 05 score never decreases
# Scoring and multiplier 06 multiplier does not exceed the maximum
Feature: Scoring and multiplier

Background:
  Given a new game with width <width> and height <height>

Scenario: Scoring and multiplier 01 new game starts with zero score and one times multiplier
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <score>
  And the multiplier is <multiplier>
  And the wave number is <wave>

Examples:
  | width | height | score | multiplier | wave | expected_width | expected_height |
  | 800   | 600    | 0     | 1          | 1    | 800            | 600             |
  | 1920  | 1080   | 0     | 1          | 1    | 1920           | 1080            |

Scenario: Scoring and multiplier 02 multiplier follows the wave schedule
  When the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave number is <wave>
  And the multiplier is <multiplier>

Examples:
  | width | height | wave | multiplier | expected_width | expected_height |
  | 800   | 600    | 1    | 1          | 800            | 600             |
  | 800   | 600    | 2    | 1          | 800            | 600             |
  | 800   | 600    | 3    | 2          | 800            | 600             |
  | 800   | 600    | 4    | 2          | 800            | 600             |
  | 800   | 600    | 5    | 3          | 800            | 600             |
  | 800   | 600    | 11   | 6          | 800            | 600             |
  | 1920  | 1080   | 6    | 3          | 1920           | 1080            |
  | 1920  | 1080   | 12   | 6          | 1920           | 1080            |

Scenario: Scoring and multiplier 03 destroying an enemy missile awards points times multiplier
  When the game is at wave <wave>
  And the current wave has 2 scheduled enemies still active
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the enemy missile path passes within distance <radius> of that fireball center
  When time advances until the enemy missile is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the enemy missile is destroyed by the fireball
  And there are 1 enemy missiles in flight
  And the wave is not complete
  And the multiplier is <multiplier>
  And the score is <score>

Examples:
  | width | height | wave | multiplier | fb_x | fb_y | radius | score | expected_width | expected_height |
  | 800   | 600    | 1    | 1          | 400  | 250  | 40     | 25    | 800            | 600             |
  | 800   | 600    | 3    | 2          | 400  | 250  | 40     | 50    | 800            | 600             |
  | 800   | 600    | 5    | 3          | 400  | 250  | 40     | 75    | 800            | 600             |
  | 1920  | 1080   | 1    | 1          | 960  | 400  | 50     | 25    | 1920           | 1080            |

Scenario: Scoring and multiplier 04 wave end awards unused missiles and cities times multiplier
  When the game is at wave <wave>
  And every non-destroyed battery has <ammo> missiles
  And the current wave has 1 scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave is complete
  And there are <living_cities> living cities
  And the multiplier is <multiplier>
  And the score is <score>

Examples:
  | width | height | wave | multiplier | ammo | living_cities | score | expected_width | expected_height |
  | 800   | 600    | 1    | 1          | 10   | 5             | 650   | 800            | 600             |
  | 800   | 600    | 1    | 1          | 0    | 5             | 500   | 800            | 600             |
  | 800   | 600    | 1    | 1          | 5    | 5             | 575   | 800            | 600             |
  | 800   | 600    | 3    | 2          | 10   | 5             | 1300  | 800            | 600             |
  | 1920  | 1080   | 1    | 1          | 10   | 5             | 650   | 1920           | 1080            |

Scenario: Scoring and multiplier 05 score never decreases
  When the game is at wave <wave>
  And the current wave has 2 scheduled enemies still active
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the enemy missile path passes within distance <radius> of that fireball center
  When time advances until the enemy missile is inside the fireball radius or has impacted
  Then the score is <score_after_kill>
  When the player aims at <aim_x> <aim_y>
  Then the score is <score_after_aim>
  When time advances until all wave enemies are destroyed or have impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <score_after_wave>

Examples:
  | width | height | wave | fb_x | fb_y | radius | aim_x | aim_y | score_after_kill | score_after_aim | score_after_wave | expected_width | expected_height |
  | 800   | 600    | 1    | 400  | 250  | 40     | 100   | 100   | 25               | 25              | 675              | 800            | 600             |
  | 800   | 600    | 3    | 400  | 250  | 40     | 200   | 150   | 50               | 50              | 1350             | 800            | 600             |
  | 1920  | 1080   | 1    | 960  | 400  | 50     | 50    | 50    | 25               | 25              | 675              | 1920           | 1080            |

Scenario: Scoring and multiplier 06 multiplier does not exceed the maximum
  When the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the multiplier is <multiplier>

Examples:
  | width | height | wave | multiplier | expected_width | expected_height |
  | 800   | 600    | 11   | 6          | 800            | 600             |
  | 800   | 600    | 13   | 6          | 800            | 600             |
  | 800   | 600    | 20   | 6          | 800            | 600             |
  | 1920  | 1080   | 15   | 6          | 1920           | 1080            |

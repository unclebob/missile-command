# Bombers and satellites 01 a flyer moves across the playfield
# Bombers and satellites 02 a flyer drops enemy missiles during its pass
# Bombers and satellites 03 a fireball destroys a flyer
# Bombers and satellites 04 destroying a flyer awards one hundred times multiplier
# Bombers and satellites 05 destroying a flyer stops further drops
# Bombers and satellites 06 an unintercepted dropped missile destroys a city
# Bombers and satellites 07 later waves schedule bombers and satellites
Feature: Bombers and satellites

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: Bombers and satellites 01 a flyer moves across the playfield
  And a <flyer_kind> flyer from <start_x> <start_y> toward <end_x> <end_y> at speed <speed>
  When time advances by 0.2 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there is 1 <flyer_kind> flyer in flight
  And the <flyer_kind> flyer has progressed along its path
  And the <flyer_kind> flyer y is <start_y>

Examples:
  | width | height | flyer_kind | start_x | start_y | end_x | end_y | speed | expected_width | expected_height |
  | 800   | 600    | bomber     | 0       | 80      | 800   | 80    | 100   | 800            | 600             |
  | 800   | 600    | satellite  | 800     | 50      | 0     | 50    | 120   | 800            | 600             |
  | 1920  | 1080   | bomber     | 0       | 120     | 1920  | 120   | 150   | 1920           | 1080            |
  | 1920  | 1080   | satellite  | 0       | 90      | 1920  | 90    | 140   | 1920           | 1080            |

Scenario: Bombers and satellites 02 a flyer drops enemy missiles during its pass
  And a <flyer_kind> flyer from <start_x> <start_y> toward <end_x> <end_y> at speed <speed>
  And the flyer drops <drop_count> enemy missiles toward living cities at path progress <drop_progress>
  When time advances until the flyer has passed drop progress <drop_progress>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there is 1 <flyer_kind> flyer in flight
  And there are <drop_count> enemy missiles in flight
  And every dropped enemy missile originates at the flyer position

Examples:
  | width | height | flyer_kind | start_x | start_y | end_x | end_y | speed | drop_count | drop_progress | expected_width | expected_height |
  | 800   | 600    | bomber     | 0       | 80      | 800   | 80    | 100   | 1          | 0.4           | 800            | 600             |
  | 800   | 600    | bomber     | 0       | 80      | 800   | 80    | 100   | 2          | 0.5           | 800            | 600             |
  | 800   | 600    | satellite  | 800     | 50      | 0     | 50    | 120   | 1          | 0.35          | 800            | 600             |
  | 1920  | 1080   | satellite  | 0       | 90      | 1920  | 90    | 140   | 2          | 0.45          | 1920           | 1080            |

Scenario: Bombers and satellites 03 a fireball destroys a flyer
  And a <flyer_kind> flyer from <start_x> <start_y> toward <end_x> <end_y> at speed <speed>
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the flyer path passes within distance <radius> of that fireball center
  When time advances until the flyer is inside the fireball radius or has left the playfield
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <flyer_kind> flyer is destroyed by the fireball
  And there are 0 <flyer_kind> flyers in flight

Examples:
  | width | height | flyer_kind | start_x | start_y | end_x | end_y | speed | fb_x | fb_y | radius | expected_width | expected_height |
  | 800   | 600    | bomber     | 0       | 80      | 800   | 80    | 100   | 400  | 80   | 40     | 800            | 600             |
  | 800   | 600    | satellite  | 0       | 50      | 800   | 50    | 120   | 400  | 50   | 40     | 800            | 600             |
  | 1920  | 1080   | bomber     | 0       | 120     | 1920  | 120   | 150   | 960  | 120  | 50     | 1920           | 1080            |

Scenario: Bombers and satellites 04 destroying a flyer awards one hundred times multiplier
  When the game is at wave <wave>
  And a <flyer_kind> flyer from <start_x> <start_y> toward <end_x> <end_y> at speed <speed>
  And an enemy missile targeting city <other_city>
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the flyer path passes within distance <radius> of that fireball center
  When time advances until the flyer is inside the fireball radius or has left the playfield
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <flyer_kind> flyer is destroyed by the fireball
  And there is 1 enemy missile in flight
  And the wave is not complete
  And the multiplier is <multiplier>
  And the score is <score>

Examples:
  | width | height | wave | multiplier | flyer_kind | start_x | start_y | end_x | end_y | speed | other_city | fb_x | fb_y | radius | score | expected_width | expected_height |
  | 800   | 600    | 1    | 1          | bomber     | 0       | 80      | 800   | 80    | 100   | 0          | 400  | 80   | 40     | 100   | 800            | 600             |
  | 800   | 600    | 1    | 1          | satellite  | 0       | 50      | 800   | 50    | 120   | 0          | 400  | 50   | 40     | 100   | 800            | 600             |
  | 800   | 600    | 3    | 2          | bomber     | 0       | 80      | 800   | 80    | 100   | 1          | 400  | 80   | 40     | 200   | 800            | 600             |
  | 1920  | 1080   | 5    | 3          | satellite  | 0       | 90      | 1920  | 90    | 140   | 2          | 960  | 90   | 50     | 300   | 1920           | 1080            |

Scenario: Bombers and satellites 05 destroying a flyer stops further drops
  And a <flyer_kind> flyer from <start_x> <start_y> toward <end_x> <end_y> at speed <speed>
  And the flyer drops <drop_count> enemy missiles toward living cities at path progress <drop_progress>
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the flyer path passes within distance <radius> of that fireball center
  When time advances until the flyer is inside the fireball radius or has left the playfield
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <flyer_kind> flyer is destroyed by the fireball
  And there are 0 <flyer_kind> flyers in flight
  And there are <enemy_count> enemy missiles in flight

Examples:
  | width | height | flyer_kind | start_x | start_y | end_x | end_y | speed | drop_count | drop_progress | fb_x | fb_y | radius | enemy_count | expected_width | expected_height |
  | 800   | 600    | bomber     | 0       | 80      | 800   | 80    | 100   | 2          | 0.6           | 100  | 80   | 40     | 0           | 800            | 600             |
  | 800   | 600    | satellite  | 0       | 50      | 800   | 50    | 120   | 2          | 0.7           | 80   | 50   | 40     | 0           | 800            | 600             |
  | 1920  | 1080   | bomber     | 0       | 120     | 1920  | 120   | 150   | 3          | 0.5           | 200  | 120  | 50     | 0           | 1920           | 1080            |

Scenario: Bombers and satellites 06 an unintercepted dropped missile destroys a city
  And a <flyer_kind> flyer from <start_x> <start_y> toward <end_x> <end_y> at speed <speed>
  And the flyer drops 1 enemy missile targeting city <city_index> at path progress <drop_progress>
  When time advances until the flyer has passed drop progress <drop_progress>
  And time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And city <city_index> is not living
  And there are <living_cities> living cities
  And there are 0 enemy missiles in flight

Examples:
  | width | height | flyer_kind | start_x | start_y | end_x | end_y | speed | city_index | drop_progress | living_cities | expected_width | expected_height |
  | 800   | 600    | bomber     | 0       | 80      | 800   | 80    | 100   | 0          | 0.3           | 5             | 800            | 600             |
  | 800   | 600    | satellite  | 0       | 50      | 800   | 50    | 120   | 2          | 0.35          | 5             | 800            | 600             |
  | 1920  | 1080   | bomber     | 0       | 120     | 1920  | 120   | 150   | 5          | 0.4           | 5             | 1920           | 1080            |

Scenario: Bombers and satellites 07 later waves schedule bombers and satellites
  When the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave <wave> bomber schedule count is <bomber_count>
  And wave <wave> satellite schedule count is <satellite_count>

Examples:
  | width | height | wave | bomber_count | satellite_count | expected_width | expected_height |
  | 800   | 600    | 1    | 0            | 0               | 800            | 600             |
  | 800   | 600    | 3    | 0            | 0               | 800            | 600             |
  | 800   | 600    | 4    | 1            | 0               | 800            | 600             |
  | 800   | 600    | 5    | 1            | 1               | 800            | 600             |
  | 1920  | 1080   | 3    | 0            | 0               | 1920           | 1080            |
  | 1920  | 1080   | 5    | 1            | 1               | 1920           | 1080            |

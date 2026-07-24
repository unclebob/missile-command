# Smart bombs 01 a smart bomb advances toward its target
# Smart bombs 02 a well centered fireball destroys a smart bomb
# Smart bombs 03 an edge of blast fireball is evaded once
# Smart bombs 04 after evasion the smart bomb remains a threat
# Smart bombs 05 a second well centered fireball destroys an escaped smart bomb
# Smart bombs 06 an unintercepted smart bomb destroys its target city
# Smart bombs 07 destroying a smart bomb awards one hundred twenty five times multiplier
# Smart bombs 08 later waves schedule smart bombs
Feature: Smart bombs

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: Smart bombs 01 a smart bomb advances toward its target
  And a smart bomb targeting city <city_index>
  When time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <enemy_count> enemy missiles in flight
  And there is 1 smart bomb in flight
  And a smart bomb has progressed toward city <city_index>

Examples:
  | width | height | city_index | enemy_count | expected_width | expected_height |
  | 800   | 600    | 0          | 1           | 800            | 600             |
  | 800   | 600    | 3          | 1           | 800            | 600             |
  | 1920  | 1080   | 5          | 1           | 1920           | 1080            |

Scenario: Smart bombs 02 a well centered fireball destroys a smart bomb
  And a smart bomb targeting city <city_index>
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the smart bomb path is centered in that fireball within distance <center_limit>
  When time advances until the smart bomb is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the smart bomb is destroyed by the fireball
  And there are 0 enemy missiles in flight
  And there are <living_cities> living cities

Examples:
  | width | height | city_index | fb_x | fb_y | radius | center_limit | living_cities | expected_width | expected_height |
  | 800   | 600    | 1          | 400  | 250  | 40     | 15           | 6             | 800            | 600             |
  | 800   | 600    | 2          | 400  | 200  | 50     | 20           | 6             | 800            | 600             |
  | 1920  | 1080   | 3          | 960  | 400  | 50     | 20           | 6             | 1920           | 1080            |

Scenario: Smart bombs 03 an edge of blast fireball is evaded once
  And a smart bomb targeting city <city_index>
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the smart bomb path is only in the edge band of that fireball between <edge_inner> and <radius>
  When time advances until the smart bomb would enter the fireball edge band or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the smart bomb has evaded the fireball
  And there is 1 smart bomb in flight
  And there are <living_cities> living cities

Examples:
  | width | height | city_index | fb_x | fb_y | radius | edge_inner | living_cities | expected_width | expected_height |
  | 800   | 600    | 1          | 400  | 250  | 40     | 25         | 6             | 800            | 600             |
  | 800   | 600    | 2          | 450  | 220  | 50     | 30         | 6             | 800            | 600             |
  | 1920  | 1080   | 4          | 960  | 400  | 50     | 30         | 6             | 1920           | 1080            |

Scenario: Smart bombs 04 after evasion the smart bomb remains a threat
  And a smart bomb targeting city <city_index>
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the smart bomb path is only in the edge band of that fireball between <edge_inner> and <radius>
  When time advances until the smart bomb would enter the fireball edge band or has impacted
  And time advances by 0.2 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there is 1 smart bomb in flight
  And a smart bomb has progressed toward city <city_index>
  And the smart bomb is not destroyed by the fireball

Examples:
  | width | height | city_index | fb_x | fb_y | radius | edge_inner | expected_width | expected_height |
  | 800   | 600    | 1          | 400  | 250  | 40     | 25         | 800            | 600             |
  | 800   | 600    | 0          | 350  | 200  | 45     | 28         | 800            | 600             |
  | 1920  | 1080   | 5          | 900  | 400  | 50     | 30         | 1920           | 1080            |

Scenario: Smart bombs 05 a second well centered fireball destroys an escaped smart bomb
  And a smart bomb targeting city <city_index>
  And a fireball at <fb_x1> <fb_y1> with radius <radius>
  And the smart bomb path is only in the edge band of that fireball between <edge_inner> and <radius>
  When time advances until the smart bomb would enter the fireball edge band or has impacted
  Then the smart bomb has evaded the fireball
  And a fireball at <fb_x2> <fb_y2> with radius <radius>
  And the smart bomb path is centered in that fireball within distance <center_limit>
  When time advances until the smart bomb is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the smart bomb is destroyed by the fireball
  And there are 0 enemy missiles in flight

Examples:
  | width | height | city_index | fb_x1 | fb_y1 | fb_x2 | fb_y2 | radius | edge_inner | center_limit | expected_width | expected_height |
  | 800   | 600    | 1          | 400   | 250   | 400   | 350   | 40     | 25         | 15           | 800            | 600             |
  | 800   | 600    | 2          | 420   | 220   | 400   | 320   | 50     | 30         | 20           | 800            | 600             |
  | 1920  | 1080   | 3          | 960   | 400   | 960   | 550   | 50     | 30         | 20           | 1920           | 1080            |

Scenario: Smart bombs 06 an unintercepted smart bomb destroys its target city
  And a smart bomb targeting city <city_index>
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And city <city_index> is not living
  And there are <living_cities> living cities
  And there are 0 enemy missiles in flight

Examples:
  | width | height | city_index | living_cities | expected_width | expected_height |
  | 800   | 600    | 0          | 5             | 800            | 600             |
  | 800   | 600    | 2          | 5             | 800            | 600             |
  | 1920  | 1080   | 5          | 5             | 1920           | 1080            |

Scenario: Smart bombs 07 destroying a smart bomb awards one hundred twenty five times multiplier
  When the game is at wave <wave>
  And a smart bomb targeting city <city_index>
  And an enemy missile targeting city <other_city>
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the smart bomb path is centered in that fireball within distance <center_limit>
  When time advances until the smart bomb is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the smart bomb is destroyed by the fireball
  And there is 1 enemy missile in flight
  And the wave is not complete
  And the multiplier is <multiplier>
  And the score is <score>

Examples:
  | width | height | wave | multiplier | city_index | other_city | fb_x | fb_y | radius | center_limit | score | expected_width | expected_height |
  | 800   | 600    | 1    | 1          | 1          | 0          | 400  | 250  | 40     | 15           | 125   | 800            | 600             |
  | 800   | 600    | 3    | 2          | 1          | 0          | 400  | 250  | 40     | 15           | 250   | 800            | 600             |
  | 800   | 600    | 5    | 3          | 2          | 0          | 400  | 200  | 50     | 20           | 375   | 800            | 600             |
  | 1920  | 1080   | 1    | 1          | 3          | 5          | 960  | 400  | 50     | 20           | 125   | 1920           | 1080            |

Scenario: Smart bombs 08 later waves schedule smart bombs
  When the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave <wave> smart bomb schedule count is <smart_count>

Examples:
  | width | height | wave | smart_count | expected_width | expected_height |
  | 800   | 600    | 1    | 0           | 800            | 600             |
  | 800   | 600    | 4    | 0           | 800            | 600             |
  | 800   | 600    | 7    | 1           | 800            | 600             |
  | 800   | 600    | 9    | 2           | 800            | 600             |
  | 1920  | 1080   | 6    | 0           | 1920           | 1080            |
  | 1920  | 1080   | 8    | 1           | 1920           | 1080            |

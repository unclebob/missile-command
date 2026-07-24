# Title screen 01 a new session starts on the title screen
# Title screen 02 the title screen shows the game name
# Title screen 03 the title screen shows a start affordance
# Title screen 04 start enters playing with a fresh layout
# Title screen 05 start resets score and wave for a new run
# Title screen 06 start uses the current playfield dimensions
# Title screen 07 fire does not launch on the title screen
# Title screen 08 confirm from THE END without high score returns to title
Feature: Title screen

Background:
  Given a new game with width <width> and height <height>

Scenario: Title screen 01 a new session starts on the title screen
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

Scenario: Title screen 02 the title screen shows the game name
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title
  And the title game name is <game_name>

Examples:
  | width | height | game_name       | expected_width | expected_height |
  | 800   | 600    | Missile Command | 800            | 600             |
  | 1920  | 1080   | Missile Command | 1920           | 1080            |

Scenario: Title screen 03 the title screen shows a start affordance
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title
  And the title shows a start affordance

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Title screen 04 start enters playing with a fresh layout
  When the player starts the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And there are <city_count> living cities
  And there are <battery_count> non-destroyed batteries named left center and right
  And each battery has <ammo> missiles

Examples:
  | width | height | city_count | battery_count | ammo | expected_width | expected_height |
  | 800   | 600    | 6          | 3             | 10   | 800            | 600             |
  | 1920  | 1080   | 6          | 3             | 10   | 1920           | 1080            |
  | 1024  | 768    | 6          | 3             | 10   | 1024           | 768             |

Scenario: Title screen 05 start resets score and wave for a new run
  When the score becomes <prior_score>
  And the game is at wave <prior_wave>
  And the player starts the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And the score is <score>
  And the wave number is <wave>
  And the multiplier is <multiplier>

Examples:
  | width | height | prior_score | prior_wave | score | wave | multiplier | expected_width | expected_height |
  | 800   | 600    | 2500        | 5          | 0     | 1    | 1          | 800            | 600             |
  | 800   | 600    | 99999       | 12         | 0     | 1    | 1          | 800            | 600             |
  | 1920  | 1080   | 100         | 3          | 0     | 1    | 1          | 1920           | 1080            |

Scenario: Title screen 06 start uses the current playfield dimensions
  When the player starts the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And the horizontal span of the cities is greater than half of width <expected_width>

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1600  | 600    | 1600           | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Title screen 07 fire does not launch on the title screen
  When the player aims at <aim_x> <aim_y>
  And the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title
  And there are 0 defensive missiles in flight

Examples:
  | width | height | aim_x | aim_y | battery | expected_width | expected_height |
  | 800   | 600    | 400   | 200   | left    | 800            | 600             |
  | 800   | 600    | 400   | 200   | center  | 800            | 600             |
  | 800   | 600    | 400   | 200   | right   | 800            | 600             |
  | 1920  | 1080   | 960   | 400   | center  | 1920           | 1080            |

Scenario: Title screen 08 confirm from THE END without high score returns to title
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  Then the game is at THE END
  When the player confirms the end screen
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

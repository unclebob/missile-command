# THE END 01 a new game is not at THE END
# THE END 02 living cities prevent THE END
# THE END 03 zero living cities and zero reserve enter THE END
# THE END 04 reserve cities prevent THE END when all living cities are gone
# THE END 05 THE END uses the THE END message not Game Over
# THE END 06 final score remains available at THE END
# THE END 07 fire commands do not launch after THE END
# THE END 08 THE END presents a centered screen filling fireball
# THE END 09 THE END letters fill the final fireball expanse
# THE END 10 THE END letters are only visible inside the fireball
Feature: THE END

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: THE END 01 a new game is not at THE END
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <living_cities> living cities
  And the bonus city reserve is <reserve>
  And the game is not at THE END

Examples:
  | width | height | living_cities | reserve | expected_width | expected_height |
  | 800   | 600    | 6             | 0       | 800            | 600             |
  | 1920  | 1080   | 6             | 0       | 1920           | 1080            |

Scenario: THE END 02 living cities prevent THE END
  And city 0 has been destroyed
  And city 1 has been destroyed
  And there are <living_cities> living cities
  When game over conditions are evaluated
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the game is not at THE END

Examples:
  | width | height | living_cities | expected_width | expected_height |
  | 800   | 600    | 4             | 800            | 600             |
  | 1024  | 768    | 4             | 1024           | 768             |
  | 1920  | 1080   | 4             | 1920           | 1080            |

Scenario: THE END 03 zero living cities and zero reserve enter THE END
  And all cities have been destroyed
  And the bonus city reserve is set to <reserve>
  And there are 0 living cities
  When game over conditions are evaluated
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the game is at THE END
  And there are 0 living cities
  And the bonus city reserve is <reserve>

Examples:
  | width | height | reserve | expected_width | expected_height |
  | 800   | 600    | 0       | 800            | 600             |
  | 1920  | 1080   | 0       | 1920           | 1080            |
  | 1024  | 768    | 0       | 1024           | 768             |

Scenario: THE END 04 reserve cities prevent THE END when all living cities are gone
  And all cities have been destroyed
  And the bonus city reserve is set to <starting_reserve>
  And there are 0 living cities
  When game over conditions are evaluated
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the game is not at THE END
  And there are <living_cities> living cities
  And the bonus city reserve is <reserve>

Examples:
  | width | height | starting_reserve | living_cities | reserve | expected_width | expected_height |
  | 800   | 600    | 1                | 1             | 0       | 800            | 600             |
  | 800   | 600    | 2                | 2             | 0       | 800            | 600             |
  | 800   | 600    | 3                | 3             | 0       | 800            | 600             |
  | 1920  | 1080   | 1                | 1             | 0       | 1920           | 1080            |

Scenario: THE END 05 THE END uses the THE END message not Game Over
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the game is at THE END
  And the end message is <end_message>
  And the end message is not <wrong_message>

Examples:
  | width | height | end_message | wrong_message | expected_width | expected_height |
  | 800   | 600    | THE END     | Game Over     | 800            | 600             |
  | 1920  | 1080   | THE END     | Game Over     | 1920           | 1080            |
  | 1024  | 768    | THE END     | Game Over     | 1024           | 768             |

Scenario: THE END 06 final score remains available at THE END
  And the score becomes <score>
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the game is at THE END
  And the score is <score>
  And the final score is <score>

Examples:
  | width | height | score | expected_width | expected_height |
  | 800   | 600    | 0     | 800            | 600             |
  | 800   | 600    | 2500  | 800            | 600             |
  | 800   | 600    | 12500 | 800            | 600             |
  | 1920  | 1080   | 999   | 1920           | 1080            |

Scenario: THE END 07 fire commands do not launch after THE END
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  Then the game is at THE END
  When the player aims at <aim_x> <aim_y>
  And the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 0 defensive missiles in flight
  And the game is at THE END

Examples:
  | width | height | aim_x | aim_y | battery | expected_width | expected_height |
  | 800   | 600    | 400   | 200   | left    | 800            | 600             |
  | 800   | 600    | 400   | 200   | center  | 800            | 600             |
  | 800   | 600    | 400   | 200   | right   | 800            | 600             |
  | 1920  | 1080   | 960   | 400   | center  | 1920           | 1080            |

Scenario: THE END 08 THE END presents a centered screen filling fireball
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  Then the game is at THE END
  And the end fireball is centered at the playfield center
  When time advances until the end fireball reaches max radius
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the end fireball radius fills the playfield
  When time advances into the end fireball shrink phase
  Then the end fireball radius is less than its max radius

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

Scenario: THE END 09 THE END letters fill the final fireball expanse
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And time advances until the end fireball reaches max radius
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the end message is THE END
  And the end message glyph bounds fill the end fireball max expanse
  And the end message is centered at the playfield center

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

Scenario: THE END 10 THE END letters are only visible inside the fireball
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  When time advances until the end fireball radius is <partial_radius>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the end message visibility is clipped to the end fireball disk
  And the end message is not visible outside the end fireball
  And more of the end message is revealed as the end fireball radius grows

Examples:
  | width | height | partial_radius | expected_width | expected_height |
  | 800   | 600    | 50             | 800            | 600             |
  | 800   | 600    | 120            | 800            | 600             |
  | 1920  | 1080   | 200            | 1920           | 1080            |

# mutation-stamp: sha256=e0b2d4b16a96187ac4ff9000bb71ac593f8c6f4da99e4bbcfaafe3352c408737
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-25T16:09:56.731212Z","feature_name":"THE END","feature_path":"features/the-end.feature","background_hash":"2ad6c53e758cb94bd90a7e42d0ca6dc51bfdcaa4e3368f8e8c551a4cb55a221c","implementation_hash":"sha256:2b9f703d4cd2633cd2078fbb7e1296ba4415dd5247de8adff2b0130b604a1e33","scenarios":[{"index":3,"name":"THE END 04 reserve cities prevent THE END when all living cities are gone","scenario_hash":"43adfc994d85f0fc6c757c4e3349df546429c8e978e21d2aed3a2c306cd8de90","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:09:12.781039Z"},{"index":0,"name":"THE END 01 a new game is not at THE END","scenario_hash":"8e134b71d645e9ace9ed2ae2fc27fe76b5a1356cc27e43dabc36a67d88460361","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:39.492733Z"},{"index":1,"name":"THE END 02 living cities prevent THE END","scenario_hash":"9c26be37cc9f2f6917b51fda1215345ecd3d5710b905170aa23a371047a66281","mutation_count":15,"result":{"Total":15,"Killed":15,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:39.492733Z"},{"index":2,"name":"THE END 03 zero living cities and zero reserve enter THE END","scenario_hash":"3dd689ce6742efc00d3ce39190c8a7ef9b9d96c84b335ee82f72f88f1138f555","mutation_count":15,"result":{"Total":15,"Killed":15,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:39.492733Z"},{"index":4,"name":"THE END 05 THE END uses the THE END message not Game Over","scenario_hash":"3572bf42cff91762de79be4bc066a5ac0d25525a27d9a4aeca1a123d787e38c8","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:39.492733Z"},{"index":5,"name":"THE END 06 final score remains available at THE END","scenario_hash":"73c0e51ea73db65b9f171a2da4e7b4ab4874aceca27c4dcea04578b3b7375308","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:39.492733Z"},{"index":6,"name":"THE END 07 fire commands do not launch after THE END","scenario_hash":"18b65723853dd17c15e3b3adf5a07ad2a0ae9a822e7d3bbd9850158e9632c79d","mutation_count":15,"result":{"Total":15,"Killed":15,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:39.492733Z"},{"index":7,"name":"THE END 08 THE END presents a centered screen filling fireball","scenario_hash":"83cc35169ce6778d06b4d069e8c009a3d69b42324690d47688b81c5e46751218","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:39.492733Z"},{"index":8,"name":"THE END 09 THE END letters fill the final fireball expanse","scenario_hash":"349e9cba825b48ab32ff0711db3e81250a4847719c5441dffdbc1a8214e99731","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:39.492733Z"},{"index":9,"name":"THE END 10 THE END letters are only visible inside the fireball","scenario_hash":"6b59e159e410b913bf9b13f6bbe1b67287a744f64f2761ef8c68b783621c86d1","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:39.492733Z"}]}
# acceptance-mutation-manifest-end

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
  And there are 0 living cities
  And the bonus city reserve is <expected_reserve>

Examples:
  | width | height | starting_reserve | expected_reserve | expected_width | expected_height |
  | 800   | 600    | 1                | 1                | 800            | 600             |
  | 800   | 600    | 2                | 2                | 800            | 600             |
  | 800   | 600    | 3                | 3                | 800            | 600             |
  | 1920  | 1080   | 1                | 1                | 1920           | 1080            |

Scenario: THE END 05 THE END uses the THE END message not Game Over
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the game is at THE END
  And the end message is THE END

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: THE END 06 final score remains available at THE END
  And the score becomes <score>
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the game is at THE END
  And the score is <expected_score>
  And the final score is <expected_score>

Examples:
  | width | height | score | expected_score | expected_width | expected_height |
  | 800   | 600    | 0     | 0              | 800            | 600             |
  | 800   | 600    | 2500  | 2500           | 800            | 600             |
  | 800   | 600    | 12500 | 12500          | 800            | 600             |
  | 1920  | 1080   | 999   | 999            | 1920           | 1080            |

Scenario: THE END 07 fire commands do not launch after THE END
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  Then the game is at THE END
  When the player aims at 400 200
  And the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 0 defensive missiles in flight
  And the game is at THE END

Examples:
  | width | height | battery | expected_width | expected_height |
  | 800   | 600    | left    | 800            | 600             |
  | 800   | 600    | center  | 800            | 600             |
  | 800   | 600    | right   | 800            | 600             |

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
  When time advances until the end fireball radius is 50
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the end message visibility is clipped to the end fireball disk
  And the end message is not visible outside the end fireball
  And more of the end message is revealed as the end fireball radius grows

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

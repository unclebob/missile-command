# HUD 01 playing HUD shows score wave multiplier ammo cities and reserve
# HUD 02 HUD score matches core after a scoring event
# HUD 03 HUD ammo matches batteries after fire
# HUD 04 HUD wave and multiplier match core
# HUD 05 HUD living cities and bonus reserve match core
# HUD 06 paused screen still exposes the HUD
# HUD 07 title screen is not required to show the full HUD
Feature: HUD

Background:
  Given a new game with width <width> and height <height>

Scenario: HUD 01 playing HUD shows score wave multiplier ammo cities and reserve
  When the player starts the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And the hud shows score <score>
  And the hud shows wave <wave>
  And the hud shows multiplier <multiplier>
  And the hud shows left ammo <left_ammo>
  And the hud shows center ammo <center_ammo>
  And the hud shows right ammo <right_ammo>
  And the hud shows living cities <living_cities>
  And the hud shows bonus cities <bonus_cities>

Examples:
  | width | height | score | wave | multiplier | left_ammo | center_ammo | right_ammo | living_cities | bonus_cities | expected_width | expected_height |
  | 800   | 600    | 0     | 1    | 1          | 10        | 10          | 10         | 6             | 0            | 800            | 600             |
  | 1920  | 1080   | 0     | 1    | 1          | 10        | 10          | 10         | 6             | 0            | 1920           | 1080            |
  | 1024  | 768    | 0     | 1    | 1          | 10        | 10          | 10         | 6             | 0            | 1024           | 768             |

Scenario: HUD 02 HUD score matches core after a scoring event
  When the player starts the game
  And the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <score>
  And the hud shows score <score>

Examples:
  | width | height | score | expected_width | expected_height |
  | 800   | 600    | 25    | 800            | 600             |
  | 800   | 600    | 2500  | 800            | 600             |
  | 800   | 600    | 12500 | 800            | 600             |
  | 1920  | 1080   | 100   | 1920           | 1080            |

Scenario: HUD 03 HUD ammo matches batteries after fire
  When the player starts the game
  And the player aims at <aim_x> <aim_y>
  And the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And each non-destroyed battery has matching hud ammo
  And the hud shows <battery> ammo <ammo>

Examples:
  | width | height | aim_x | aim_y | battery | ammo | expected_width | expected_height |
  | 800   | 600    | 400   | 200   | left    | 9    | 800            | 600             |
  | 800   | 600    | 400   | 200   | center  | 9    | 800            | 600             |
  | 800   | 600    | 400   | 200   | right   | 9    | 800            | 600             |
  | 1920  | 1080   | 960   | 400   | center  | 9    | 1920           | 1080            |

Scenario: HUD 04 HUD wave and multiplier match core
  When the player starts the game
  And the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave number is <wave>
  And the multiplier is <multiplier>
  And the hud shows wave <wave>
  And the hud shows multiplier <multiplier>

Examples:
  | width | height | wave | multiplier | expected_width | expected_height |
  | 800   | 600    | 1    | 1          | 800            | 600             |
  | 800   | 600    | 3    | 2          | 800            | 600             |
  | 800   | 600    | 5    | 3          | 800            | 600             |
  | 1920  | 1080   | 11   | 6          | 1920           | 1080            |

Scenario: HUD 05 HUD living cities and bonus reserve match core
  When the player starts the game
  And city <destroyed_city> has been destroyed
  And the bonus city reserve is set to <bonus_cities>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <living_cities> living cities
  And the bonus city reserve is <bonus_cities>
  And the hud shows living cities <living_cities>
  And the hud shows bonus cities <bonus_cities>

Examples:
  | width | height | destroyed_city | living_cities | bonus_cities | expected_width | expected_height |
  | 800   | 600    | 0              | 5             | 0            | 800            | 600             |
  | 800   | 600    | 0              | 5             | 2            | 800            | 600             |
  | 800   | 600    | 3              | 5             | 1            | 800            | 600             |
  | 1920  | 1080   | 5              | 5             | 3            | 1920           | 1080            |

Scenario: HUD 06 paused screen still exposes the HUD
  When the player starts the game
  And the score becomes <score>
  And the player pauses the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is paused
  And the hud shows score <score>
  And the hud shows wave <wave>
  And the hud shows multiplier <multiplier>
  And the hud shows living cities <living_cities>
  And the hud shows bonus cities <bonus_cities>

Examples:
  | width | height | score | wave | multiplier | living_cities | bonus_cities | expected_width | expected_height |
  | 800   | 600    | 500   | 1    | 1          | 6             | 0            | 800            | 600             |
  | 800   | 600    | 0     | 1    | 1          | 6             | 0            | 800            | 600             |
  | 1920  | 1080   | 1000  | 1    | 1          | 6             | 0            | 1920           | 1080            |

Scenario: HUD 07 title screen is not required to show the full HUD
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title
  And the full playing hud is not required

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

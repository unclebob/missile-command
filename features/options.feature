# Options 01 options are reachable from the title screen
# Options 02 defaults are unmuted arcade difficulty and default fire keys
# Options 03 mute can be toggled and is stored
# Options 04 remapped fire keys launch the matching battery
# Options 05 difficulty scales wave enemy count and speed
# Options 06 difficulty change applies on the next new game
# Options 07 leaving options returns to the title screen
Feature: Options

Background:
  Given a new game with width <width> and height <height>

Scenario: Options 01 options are reachable from the title screen
  Then the screen is title
  When the player opens options from the title
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is options

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

Scenario: Options 02 defaults are unmuted arcade difficulty and default fire keys
  When the player opens options from the title
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And mute is <mute>
  And the difficulty is <difficulty>
  And the fire key for left includes <left_key>
  And the fire key for center includes <center_key>
  And the fire key for right includes <right_key>
  And the pause key includes <pause_key>

Examples:
  | width | height | mute  | difficulty | left_key | center_key | right_key | pause_key | expected_width | expected_height |
  | 800   | 600    | false | arcade     | z        | x          | c         | p         | 800            | 600             |
  | 1920  | 1080   | false | arcade     | z        | x          | c         | p         | 1920           | 1080            |

Scenario: Options 03 mute can be toggled and is stored
  When the player opens options from the title
  And the player sets mute to <mute>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And mute is <mute>
  When the player leaves options
  And the player opens options from the title
  Then mute is <mute>

Examples:
  | width | height | mute  | expected_width | expected_height |
  | 800   | 600    | true  | 800            | 600             |
  | 800   | 600    | false | 800            | 600             |
  | 1920  | 1080   | true  | 1920           | 1080            |

Scenario: Options 04 remapped fire keys launch the matching battery
  When the player opens options from the title
  And the player binds fire <battery> to key <key>
  And the player leaves options
  And the player starts the game
  And the player aims at <aim_x> <aim_y>
  And the player presses key <key>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 1 defensive missiles in flight
  And a defensive missile from the <battery> battery targets <aim_x> <aim_y>
  And the <battery> battery has <ammo> missiles

Examples:
  | width | height | battery | key | aim_x | aim_y | ammo | expected_width | expected_height |
  | 800   | 600    | left    | q   | 400   | 200   | 9    | 800            | 600             |
  | 800   | 600    | center  | w   | 400   | 200   | 9    | 800            | 600             |
  | 800   | 600    | right   | e   | 400   | 200   | 9    | 800            | 600             |
  | 1920  | 1080   | left    | a   | 960   | 400   | 9    | 1920           | 1080            |

Scenario: Options 05 difficulty scales wave enemy count and speed
  When the player opens options from the title
  And the player sets difficulty to <difficulty>
  And the player leaves options
  And the player starts the game
  And the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the difficulty is <difficulty>
  And wave <wave> enemy count is <enemy_count>
  And wave <wave> enemy speed is <enemy_speed>

Examples:
  | width | height | difficulty | wave | enemy_count | enemy_speed | expected_width | expected_height |
  | 800   | 600    | arcade     | 1    | 3           | 50.0        | 800            | 600             |
  | 800   | 600    | normal     | 1    | 2           | 42.5        | 800            | 600             |
  | 800   | 600    | easy       | 1    | 2           | 35.0        | 800            | 600             |
  | 800   | 600    | arcade     | 3    | 5           | 75.0        | 800            | 600             |
  | 800   | 600    | easy       | 3    | 3           | 52.5        | 800            | 600             |
  | 1920  | 1080   | normal     | 2    | 3           | 53.125      | 1920           | 1080            |

Scenario: Options 06 difficulty change is stored and applies on start
  When the player opens options from the title
  And the player sets difficulty to <difficulty>
  And the player leaves options
  When the player opens options from the title
  Then the difficulty is <difficulty>
  When the player leaves options
  And the player starts the game
  And the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave <wave> enemy count is <enemy_count>
  And wave <wave> enemy speed is <enemy_speed>

Examples:
  | width | height | difficulty | wave | enemy_count | enemy_speed | expected_width | expected_height |
  | 800   | 600    | easy       | 1    | 2           | 35.0        | 800            | 600             |
  | 800   | 600    | normal     | 1    | 2           | 42.5        | 800            | 600             |
  | 1920  | 1080   | easy       | 1    | 2           | 35.0        | 1920           | 1080            |

Scenario: Options 07 leaving options returns to the title screen
  When the player opens options from the title
  Then the screen is options
  When the player leaves options
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

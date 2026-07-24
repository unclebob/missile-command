# Pause 01 pause from playing enters the paused screen
# Pause 02 while paused simulation does not advance
# Pause 03 while paused fire does not launch
# Pause 04 resume returns to playing
# Pause 05 resume continues entities from prior state
# Pause 06 pause is ignored on the title screen
Feature: Pause

Background:
  Given a new game with width <width> and height <height>
  When the player starts the game

Scenario: Pause 01 pause from playing enters the paused screen
  Then the screen is playing
  When the player pauses the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is paused

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

Scenario: Pause 02 while paused simulation does not advance
  And an enemy missile targeting city <city_index>
  When time advances by <play_dt> seconds
  And the first enemy missile progress is recorded
  When the player pauses the game
  And time advances by <paused_dt> seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is paused
  And the first enemy missile progress equals the recorded progress

Examples:
  | width | height | city_index | play_dt | paused_dt | expected_width | expected_height |
  | 800   | 600    | 0          | 0.1     | 0.5       | 800            | 600             |
  | 800   | 600    | 3          | 0.1     | 1.0       | 800            | 600             |
  | 1920  | 1080   | 0          | 0.1     | 0.5       | 1920           | 1080            |

Scenario: Pause 03 while paused fire does not launch
  When the player pauses the game
  And the player aims at <aim_x> <aim_y>
  And the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is paused
  And there are 0 defensive missiles in flight
  And each non-destroyed battery has <ammo> missiles

Examples:
  | width | height | aim_x | aim_y | battery | ammo | expected_width | expected_height |
  | 800   | 600    | 400   | 200   | left    | 10   | 800            | 600             |
  | 800   | 600    | 400   | 200   | center  | 10   | 800            | 600             |
  | 800   | 600    | 400   | 200   | right   | 10   | 800            | 600             |
  | 1920  | 1080   | 960   | 400   | center  | 10   | 1920           | 1080            |

Scenario: Pause 04 resume returns to playing
  When the player pauses the game
  Then the screen is paused
  When the player resumes the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

Scenario: Pause 05 resume continues entities from prior state
  And an enemy missile targeting city <city_index>
  When time advances by <play_dt> seconds
  And the first enemy missile progress is recorded
  When the player pauses the game
  And time advances by <paused_dt> seconds
  And the player resumes the game
  And time advances by <play_dt> seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And the first enemy missile progress is greater than the recorded progress

Examples:
  | width | height | city_index | play_dt | paused_dt | expected_width | expected_height |
  | 800   | 600    | 0          | 0.1     | 0.5       | 800            | 600             |
  | 800   | 600    | 2          | 0.1     | 0.5       | 800            | 600             |
  | 1920  | 1080   | 0          | 0.1     | 0.5       | 1920           | 1080            |

Scenario: Pause 06 pause is ignored on the title screen
  Given a new game with width <width> and height <height>
  Then the screen is title
  When the player pauses the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

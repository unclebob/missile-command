# Desktop host 01 the project documents a desktop launch command
# Desktop host 02 starting play uses the requested playfield size
# Desktop host 03 resize reflows layout without a fixed magnified buffer
# Desktop host 04 mouse aim and key fire are available while playing
# Desktop host 05 click zone fire is available while playing
# Desktop host 06 pause and resume work on the desktop host
# Desktop host 07 high scores and options persist across desktop restarts
# Desktop host 08 pure core stays free of host dependencies
Feature: Desktop host

Background:
  Given a new game with width <width> and height <height>

Scenario: Desktop host 01 the project documents a desktop launch command
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the documented desktop launch command is <command>

Examples:
  | width | height | command | expected_width | expected_height |
  | 800   | 600    | bb play | 800            | 600             |
  | 1920  | 1080   | bb play | 1920           | 1080            |

Scenario: Desktop host 02 starting play uses the requested playfield size
  When the player starts the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And there are 6 living cities
  And there are 3 non-destroyed batteries named left center and right

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1280  | 720    | 1280           | 720             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Desktop host 03 resize reflows layout without a fixed magnified buffer
  When the player starts the game
  And the playfield is resized to width <new_width> and height <new_height>
  Then the playfield width is <new_width>
  And the playfield height is <new_height>
  And city x positions increase with city index
  And every city y is in the ground band for height <new_height>
  And the horizontal span of the cities is greater than half of width <new_width>

Examples:
  | width | height | new_width | new_height |
  | 800   | 600    | 1280      | 720        |
  | 800   | 600    | 1920      | 1080       |
  | 1024  | 768    | 800       | 600        |

Scenario: Desktop host 04 mouse aim and key fire are available while playing
  When the player starts the game
  And the player aims at <aim_x> <aim_y>
  And the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the crosshair is at <aim_x> <aim_y>
  And there are 1 defensive missiles in flight
  And a defensive missile from the <battery> battery targets <aim_x> <aim_y>

Examples:
  | width | height | aim_x | aim_y | battery | expected_width | expected_height |
  | 800   | 600    | 400   | 200   | left    | 800            | 600             |
  | 800   | 600    | 400   | 200   | center  | 800            | 600             |
  | 800   | 600    | 400   | 200   | right   | 800            | 600             |
  | 1920  | 1080   | 960   | 400   | center  | 1920           | 1080            |

Scenario: Desktop host 05 click zone fire is available while playing
  When the player starts the game
  And the player aims at <aim_x> <aim_y>
  And the player clicks at <aim_x> <aim_y>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 1 defensive missiles in flight
  And a defensive missile from the <battery> battery targets <aim_x> <aim_y>

Examples:
  | width | height | aim_x | aim_y | battery | expected_width | expected_height |
  | 800   | 600    | 100   | 200   | left    | 800            | 600             |
  | 800   | 600    | 400   | 200   | center  | 800            | 600             |
  | 800   | 600    | 700   | 200   | right   | 800            | 600             |
  | 1920  | 1080   | 100   | 400   | left    | 1920           | 1080            |

Scenario: Desktop host 06 pause and resume work on the desktop host
  When the player starts the game
  And the player pauses the game
  Then the screen is paused
  When the player resumes the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Desktop host 07 high scores and options persist across desktop restarts
  When the player opens options from the title
  And the player sets mute to true
  And the player sets difficulty to easy
  And the player leaves options
  And the desktop host options and high scores are persisted
  And the desktop host is restarted with width <width> and height <height>
  When the player opens options from the title
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And mute is true
  And the difficulty is easy

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Desktop host 08 pure core stays free of host dependencies
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the architecture check passes for pure core isolation

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

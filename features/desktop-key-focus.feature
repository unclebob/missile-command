# Desktop key focus 01 the project documents a no-keyfocus launch flag
# Desktop key focus 02 no-keyfocus QA launch keeps the previous app focused and ignores desktop input
Feature: Desktop key focus

Scenario: Desktop key focus 01 the project documents a no-keyfocus launch flag
  Given a new game with width <width> and height <height>
  Then the documented desktop launch command is <command>
  And the documented desktop no-keyfocus flag is <flag>

Examples:
  | width | height | command | flag          |
  | 800   | 600    | bb play | --no-keyfocus |

Scenario: Desktop key focus 02 no-keyfocus QA launch keeps the previous app focused and ignores desktop input
  Given a desktop app named <previous_app> has keyboard focus
  When the desktop host is launched in QA mode with <flag>
  Then a playable game window is visible
  And keyboard focus remains on <previous_app>
  And real desktop keyboard and mouse input is ignored by the game
  And scripted QA events drive the game

Examples:
  | previous_app | flag          |
  | Terminal     | --no-keyfocus |

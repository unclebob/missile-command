# mutation-stamp: sha256=ac17dcf828674f90a915104c85f3ec0293c27beca2f552ad517f3585b7912588
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-26T15:39:54.496468Z","feature_name":"Desktop key focus","feature_path":"features/desktop-key-focus.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:9f440c4b22c37774e531db1f0b839338c7defb95e400fc227af076de6c401227","scenarios":[{"index":0,"name":"Desktop key focus 01 the project documents a no-keyfocus launch flag","scenario_hash":"37a3b584a1403787787b2304b05e387486a26e40a184f1a35be15507fb01e712","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:27.145774Z"},{"index":1,"name":"Desktop key focus 02 no-keyfocus QA launch keeps the previous app focused and ignores desktop input","scenario_hash":"6153870e90792a2b2121bbcbc5653fc6a00dd31ecf1628a21b8270bfb1ba2707","mutation_count":3,"result":{"Total":3,"Killed":3,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:27.145774Z"}]}
# acceptance-mutation-manifest-end

# Desktop key focus 01 the project documents a no-keyfocus launch flag
# Desktop key focus 02 no-keyfocus QA launch keeps the previous app focused and ignores desktop input
Feature: Desktop key focus

Scenario: Desktop key focus 01 the project documents a no-keyfocus launch flag
  Then the documented desktop launch command is <command>
  And the documented desktop no-keyfocus flag is <flag>

Examples:
  | command | flag          |
  | bb play | --no-keyfocus |

Scenario: Desktop key focus 02 no-keyfocus QA launch keeps the previous app focused and ignores desktop input
  Given a desktop app named <previous_app> has keyboard focus
  When the desktop host is launched in QA mode with <flag>
  Then a playable game window is visible
  And keyboard focus remains on <expected_app>
  And real desktop keyboard and mouse input is ignored by the game
  And scripted QA events drive the game

Examples:
  | previous_app | expected_app | flag          |
  | Terminal     | Terminal     | --no-keyfocus |

# mutation-stamp: sha256=077a24413cbc9ee52ee4a007bade555a5911a7bbc323d0f9192967ea4f64c9b9
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-26T15:05:58.568201Z","feature_name":"Project foundation","feature_path":"features/project-foundation.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:532de3c772129d878195c607e27951b7e3668e8d63953f15ebd727aae44fa216","scenarios":[{"index":0,"name":"Project foundation 01 new game records playfield size","scenario_hash":"8352c470f3f3cdfd24f37ce9568609f7a2fd9871f3e0867a2e817b46255fec8b","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:58.568201Z"}]}
# acceptance-mutation-manifest-end

# Project foundation 01 new game records playfield size
Feature: Project foundation

Scenario: Project foundation 01 new game records playfield size
  Given a new game with width <width> and height <height>
  And the player starts the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

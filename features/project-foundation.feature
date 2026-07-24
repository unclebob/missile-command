# mutation-stamp: sha256=642aedc01954dc5a7c0778e6b5e4c2438886142ade67de4d6cd03c91fd994040
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-24T14:46:00.288672Z","feature_name":"Project foundation","feature_path":"features/project-foundation.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:532de3c772129d878195c607e27951b7e3668e8d63953f15ebd727aae44fa216","scenarios":[{"index":0,"name":"Project foundation 01 new game records playfield size","scenario_hash":"7e200e6b6cbc53f9e5b9e2919c11c8b7770b9b10e0955a4d940edabaaba8187b","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T14:46:00.288672Z"}]}
# acceptance-mutation-manifest-end

# Project foundation 01 new game records playfield size
Feature: Project foundation

Scenario: Project foundation 01 new game records playfield size
  Given a new game with width <width> and height <height>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

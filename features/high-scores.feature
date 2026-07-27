# mutation-stamp: sha256=78bcf2c81664d07185f568ff36308291c72063a1e1dd9a29661e4549542f7397
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-27T18:35:04.462172Z","feature_name":"High scores","feature_path":"features/high-scores.feature","background_hash":"8e63f035c8dab0c09e62ed95cd3dfb2f8ecc23b566cec139f18d3206495fbae2","implementation_hash":"sha256:4c0a2db375170e22cfea26bd20e72112e89c29ce4220d9cab68e77be515da37a","scenarios":[{"index":1,"name":"High scores 08 Escape does not close the high score table","scenario_hash":"2201e8094da7d2e483c3ba0de6937f54e4d1b553a452c7b8c87895e07e781f6b","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-27T18:35:04.462172Z"},{"index":2,"name":"High scores 02 a qualifying score opens initials entry after THE END","scenario_hash":"e3911a3650c67780dad646919cea2860598a72859a927bda00d93b121fc1e540","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-27T18:35:04.462172Z"},{"index":3,"name":"High scores 03 submitting initials inserts the score in ranked order","scenario_hash":"3b741e4536b57f4b06d327b2c8bc3642a0926117190a2baa5023440bc819edb0","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-27T18:35:04.462172Z"},{"index":4,"name":"High scores 04 the table is capped at the maximum entries","scenario_hash":"e014c2bd59c0512e0d11379fbed1ec5917be885a77cc1ceba34ca025fe09f277","mutation_count":30,"result":{"Total":30,"Killed":30,"Survived":0,"Errors":0},"tested_at":"2026-07-27T18:35:04.462172Z"},{"index":5,"name":"High scores 05 initials are three characters from the allowed set","scenario_hash":"7076326968206f698657eea856ea2283c8b79d53009a2522a67cc3fd628990e3","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-27T18:35:04.462172Z"},{"index":6,"name":"High scores 06 high scores can be viewed from the title screen","scenario_hash":"a542bd4e557758671b2de92121454a30a146f4bd1bb4da8f7deb08a3478c29a1","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-27T18:35:04.462172Z"},{"index":7,"name":"High scores 07 after entry the screen returns to title","scenario_hash":"c9782b6f8d6cbcf870c94df3b3c57e7f1518ae737147ba32fb05ab387fb2a1fc","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-27T18:35:04.462172Z"},{"index":0,"name":"High scores 01 a non qualifying score does not open initials entry","scenario_hash":"c059f2cfb8e41db37389d2a981d0f9873268ff47d39ef2837dbc3ae20817c5e8","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:13:12.007661Z"}]}
# acceptance-mutation-manifest-end

# High scores 01 a non qualifying score does not open initials entry
# High scores 02 a qualifying score opens initials entry after THE END
# High scores 03 submitting initials inserts the score in ranked order
# High scores 04 the table is capped at the maximum entries
# High scores 05 initials are three characters from the allowed set
# High scores 06 high scores can be viewed from the title screen
# High scores 07 after entry the screen returns to title
# High scores 08 Escape does not close the high score table
Feature: High scores

Background:
  Given a new game with width <width> and height <height>

Scenario: High scores 01 a non qualifying score does not open initials entry
  And the high score table capacity is 3
  And a high score entry initials AAA with score 1000
  And a high score entry initials BBB with score 900
  And a high score entry initials CCC with score 800
  And the score becomes 700
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title
  And the screen is not high-score-entry

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: High scores 08 Escape does not close the high score table
  And the high score table capacity is 10
  And a high score entry initials AAA with score 1000
  When the player opens high scores from the title
  And the player presses Escape while viewing high scores
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is high-scores

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: High scores 02 a qualifying score opens initials entry after THE END
  And the high score table capacity is 3
  And a high score entry initials AAA with score 1000
  And a high score entry initials BBB with score 900
  And the score becomes <score>
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is high-score-entry
  And the pending high score is <expected_score>

Examples:
  | width | height | score | expected_score | expected_width | expected_height |
  | 800   | 600    | 500   | 500            | 800            | 600             |
  | 800   | 600    | 950   | 950            | 800            | 600             |
  | 800   | 600    | 1000  | 1000           | 800            | 600             |
  | 1920  | 1080   | 1     | 1              | 1920           | 1080            |

Scenario: High scores 03 submitting initials inserts the score in ranked order
  And the high score table capacity is 10
  And a high score entry initials AAA with score 1000
  And a high score entry initials CCC with score 500
  And the score becomes <score>
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  And the player enters high score initials BOB
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the high score table is ordered by score descending
  And the high score at rank <rank> has initials BOB and score <expected_score>
  And the high score table has <entry_count> entries

Examples:
  | width | height | score | expected_score | rank | entry_count | expected_width | expected_height |
  | 800   | 600    | 750   | 750            | 2    | 3           | 800            | 600             |
  | 800   | 600    | 1200  | 1200           | 1    | 3           | 800            | 600             |
  | 1920  | 1080   | 600   | 600            | 2    | 3           | 1920           | 1080            |

Scenario: High scores 04 the table is capped at the maximum entries
  And the high score table capacity is <capacity>
  And a high score entry initials AAA with score 1000
  And a high score entry initials BBB with score 900
  And a high score entry initials CCC with score 800
  And the score becomes <score>
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  And the player enters high score initials NEW
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the high score table has <expected_capacity> entries
  And the high score table lowest score is <lowest>
  And the high score at rank <rank> has initials NEW and score <expected_score>
  And the high score table does not include score 800

Examples:
  | width | height | capacity | expected_capacity | score | expected_score | lowest | rank | expected_width | expected_height |
  | 800   | 600    | 3        | 3                 | 850   | 850            | 850    | 3    | 800            | 600             |
  | 800   | 600    | 3        | 3                 | 950   | 950            | 900    | 2    | 800            | 600             |
  | 1920  | 1080   | 3        | 3                 | 801   | 801            | 801    | 3    | 1920           | 1080            |

Scenario: High scores 05 initials are three characters from the allowed set
  And the high score table capacity is 10
  And the score becomes 100
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  And the player enters high score initials a1b!!
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the submitted high score initials are A1B
  And the submitted high score initials length is 3

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: High scores 06 high scores can be viewed from the title screen
  And the high score table capacity is 10
  And a high score entry initials AAA with score 1000
  And a high score entry initials BBB with score 500
  When the player opens high scores from the title
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is high-scores
  And the high score at rank 1 has initials AAA and score 1000
  And the high score at rank 2 has initials BBB and score 500

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: High scores 07 after entry the screen returns to title
  And the high score table capacity is 10
  And the score becomes 100
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  And the player confirms the end screen
  And the player enters high score initials AAA
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

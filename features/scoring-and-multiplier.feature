# mutation-stamp: sha256=f2e488b0b09e5cc8c27f37eeb0488bf3846c9f3460f722b980b3abd6707e57ef
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-26T15:26:29.012616Z","feature_name":"Scoring and multiplier","feature_path":"features/scoring-and-multiplier.feature","background_hash":"2ad6c53e758cb94bd90a7e42d0ca6dc51bfdcaa4e3368f8e8c551a4cb55a221c","implementation_hash":"sha256:bb54e0efa1a80de50ecd8d34ec1ffc677b4a9f9a6557d720671a4aa4e58ba2cc","scenarios":[{"index":0,"name":"Scoring and multiplier 01 new game starts with zero score and one times multiplier","scenario_hash":"74f8c6edbb79146fdf33ba99ac29563c16d5bd51b1faa80c2d86c25de78e517e","mutation_count":14,"result":{"Total":14,"Killed":14,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:06:01.480438Z"},{"index":1,"name":"Scoring and multiplier 02 multiplier follows the wave schedule","scenario_hash":"4d2d2b54888b25362e2a407b10e6c93e6f1aaba1ea3c6b303a2f3f2683ce1b04","mutation_count":28,"result":{"Total":28,"Killed":28,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:06:01.480438Z"},{"index":2,"name":"Scoring and multiplier 03 destroying an enemy missile awards points times multiplier","scenario_hash":"ccc1f77903e08d5485dd2c4d99c99401a495a9f665d9bf2819d55e1eac2e8486","mutation_count":21,"result":{"Total":21,"Killed":21,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:06:01.480438Z"},{"index":3,"name":"Scoring and multiplier 04 wave end awards unused missiles and cities times multiplier","scenario_hash":"d8b644155c52f9c534c976918d1577d30622d9883aa34ccc22d2c5013700fb22","mutation_count":45,"result":{"Total":45,"Killed":45,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:06:01.480438Z"},{"index":4,"name":"Scoring and multiplier 05 score never decreases","scenario_hash":"77ab5c37c8feae2a6cfafb4522cd4bab4d7c611ec9adb310dcc3c2dc6b91a8c0","mutation_count":16,"result":{"Total":16,"Killed":16,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:06:01.480438Z"},{"index":5,"name":"Scoring and multiplier 06 multiplier does not exceed the maximum","scenario_hash":"2767cf6961472f8ac843b0323ddc94380f2c456e49470406b317e66edf1ff5b1","mutation_count":14,"result":{"Total":14,"Killed":14,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:06:01.480438Z"}]}
# acceptance-mutation-manifest-end

# Scoring and multiplier 01 new game starts with zero score and one times multiplier
# Scoring and multiplier 02 multiplier follows the wave schedule
# Scoring and multiplier 03 destroying an enemy missile awards points times multiplier
# Scoring and multiplier 04 wave end awards unused missiles and cities times multiplier
# Scoring and multiplier 05 score never decreases
# Scoring and multiplier 06 multiplier does not exceed the maximum
Feature: Scoring and multiplier

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: Scoring and multiplier 01 new game starts with zero score and one times multiplier
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <score>
  And the multiplier is <multiplier>
  And the wave number is <wave>

Examples:
  | width | height | score | multiplier | wave | expected_width | expected_height |
  | 800   | 600    | 0     | 1          | 1    | 800            | 600             |
  | 1920  | 1080   | 0     | 1          | 1    | 1920           | 1080            |

Scenario: Scoring and multiplier 02 multiplier follows the wave schedule
  When the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave number is <expected_wave>
  And the multiplier is <multiplier>

Examples:
  | width | height | wave | expected_wave | multiplier | expected_width | expected_height |
  | 800   | 600    | 1    | 1             | 1          | 800            | 600             |
  | 800   | 600    | 3    | 3             | 2          | 800            | 600             |
  | 800   | 600    | 5    | 5             | 3          | 800            | 600             |
  | 800   | 600    | 11   | 11            | 6          | 800            | 600             |

Scenario: Scoring and multiplier 03 destroying an enemy missile awards points times multiplier
  When the game is at wave <wave>
  And the current wave has 2 scheduled enemies still active
  And a fireball at 400 250 with radius 40
  And the enemy missile path passes within distance 40 of that fireball center
  When time advances until the enemy missile is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the enemy missile is destroyed by the fireball
  And there are 1 enemy missiles in flight
  And the wave is not complete
  And the multiplier is <multiplier>
  And the score is <score>

Examples:
  | width | height | wave | multiplier | score | expected_width | expected_height |
  | 800   | 600    | 1    | 1          | 25    | 800            | 600             |
  | 800   | 600    | 3    | 2          | 50    | 800            | 600             |
  | 800   | 600    | 5    | 3          | 75    | 800            | 600             |

Scenario: Scoring and multiplier 04 wave end awards unused missiles and cities times multiplier
  When the game is at wave <wave>
  And every non-destroyed battery has <ammo> missiles
  And the current wave has 1 scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave is complete
  And there are <living_cities> living cities
  And the multiplier is <multiplier>
  And the score is <score>

Examples:
  | width | height | wave | multiplier | ammo | living_cities | score | expected_width | expected_height |
  | 800   | 600    | 1    | 1          | 10   | 5             | 650   | 800            | 600             |
  | 800   | 600    | 1    | 1          | 0    | 5             | 500   | 800            | 600             |
  | 800   | 600    | 1    | 1          | 5    | 5             | 575   | 800            | 600             |
  | 800   | 600    | 3    | 2          | 10   | 5             | 1300  | 800            | 600             |
  | 1920  | 1080   | 1    | 1          | 10   | 5             | 650   | 1920           | 1080            |

Scenario: Scoring and multiplier 05 score never decreases
  When the game is at wave <wave>
  And the current wave has 2 scheduled enemies still active
  And a fireball at 400 250 with radius 40
  And the enemy missile path passes within distance 40 of that fireball center
  When time advances until the enemy missile is inside the fireball radius or has impacted
  Then the score is <score_after_kill>
  When the player aims at 100 100
  Then the score is <score_after_aim>
  When time advances until all wave enemies are destroyed or have impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <score_after_wave>

Examples:
  | width | height | wave | score_after_kill | score_after_aim | score_after_wave | expected_width | expected_height |
  | 800   | 600    | 1    | 25               | 25              | 675              | 800            | 600             |
  | 800   | 600    | 3    | 50               | 50              | 1350             | 800            | 600             |

Scenario: Scoring and multiplier 06 multiplier does not exceed the maximum
  When the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave number is <expected_wave>
  And the multiplier is <multiplier>

Examples:
  | width | height | wave | expected_wave | multiplier | expected_width | expected_height |
  | 800   | 600    | 11   | 11            | 6          | 800            | 600             |
  | 800   | 600    | 20   | 20            | 6          | 800            | 600             |

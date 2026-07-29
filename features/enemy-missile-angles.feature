# mutation-stamp: sha256=f321e140964d8e98c246c47a1afe8348656ac2906f413d14a643e67369c28510
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-27T18:34:59.741658Z","feature_name":"Enemy missile angles","feature_path":"features/enemy-missile-angles.feature","background_hash":"2ad6c53e758cb94bd90a7e42d0ca6dc51bfdcaa4e3368f8e8c551a4cb55a221c","implementation_hash":"sha256:570b6ddc58a5a9720b9e973ddcf67be2f9b6ebb0b78cf87d7e61f7018a5c7169","scenarios":[{"index":0,"name":"Enemy missile angles 01 angled enemy advances toward its target","scenario_hash":"6d6c5797015abb0f23e23cc2a0d1294d3104d06026bb503f6ec99e8c5fd15c3f","mutation_count":28,"result":{"Total":28,"Killed":28,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:46.070707Z"},{"index":1,"name":"Enemy missile angles 02 unintercepted angled enemy destroys a city","scenario_hash":"8f58d9e448af519c897e978ad2f5447b757cbd4e48b0dc920cb60dc4ed888b2c","mutation_count":27,"result":{"Total":27,"Killed":27,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:46.070707Z"},{"index":2,"name":"Enemy missile angles 03 unintercepted angled enemy destroys a battery","scenario_hash":"4d805eaed6a0c1e191bfa294ddef8dc18b32362f13d0a5cef5660b0325ca8442","mutation_count":28,"result":{"Total":28,"Killed":28,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:46.070707Z"},{"index":3,"name":"Enemy missile angles 04 wave enemies enter from varied sky origins","scenario_hash":"7bf2c98fc0ca0a1f81fe0d1fa65375fa286a64fe20f36451b8197845002e246e","mutation_count":18,"result":{"Total":18,"Killed":18,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:46.070707Z"}]}
# acceptance-mutation-manifest-end

# Enemy missile angles 01 angled enemy advances toward its target
# Enemy missile angles 02 unintercepted angled enemy destroys a city
# Enemy missile angles 03 unintercepted angled enemy destroys a battery
# Enemy missile angles 04 wave enemies enter from varied sky origins
Feature: Enemy missile angles

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: Enemy missile angles 01 angled enemy advances toward its target
  And an enemy missile from <origin_x> 0 targeting city <city_index>
  When time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 1 enemy missiles in flight
  And the first enemy missile origin is <expected_origin_x> 0
  And the first enemy missile origin x differs from its target x
  And an enemy missile has progressed toward city <city_index>
  And the first enemy missile has moved toward its target on both axes

Examples:
  | width | height | origin_x | expected_origin_x | city_index | expected_width | expected_height |
  | 800   | 600    | 50       | 50                | 0          | 800            | 600             |
  | 800   | 600    | 750      | 750               | 0          | 800            | 600             |
  | 800   | 600    | 100      | 100               | 5          | 800            | 600             |
  | 1920  | 1080   | 200      | 200               | 3          | 1440           | 1080            |

Scenario: Enemy missile angles 02 unintercepted angled enemy destroys a city
  And an enemy missile from <origin_x> 0 targeting city <city_index>
  Then the first enemy missile origin is <expected_origin_x> 0
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And city <expected_city_index> is not living
  And there are <living_cities> living cities
  And there are 0 enemy missiles in flight

Examples:
  | width | height | origin_x | expected_origin_x | city_index | expected_city_index | living_cities | expected_width | expected_height |
  | 800   | 600    | 50       | 50                | 0          | 0                   | 5             | 800            | 600             |
  | 800   | 600    | 700      | 700               | 2          | 2                   | 5             | 800            | 600             |
  | 1920  | 1080   | 100      | 100               | 5          | 5                   | 5             | 1440           | 1080            |

Scenario: Enemy missile angles 03 unintercepted angled enemy destroys a battery
  And an enemy missile from <origin_x> 0 targeting battery <battery>
  Then the first enemy missile origin is <expected_origin_x> 0
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <battery> battery is destroyed
  And there are 0 enemy missiles in flight

Examples:
  | width | height | origin_x | expected_origin_x | battery | expected_width | expected_height |
  | 800   | 600    | 200      | 200               | left    | 800            | 600             |
  | 800   | 600    | 100      | 100               | center  | 800            | 600             |
  | 800   | 600    | 400      | 400               | right   | 800            | 600             |
  | 1920  | 1080   | 50       | 50                | left    | 1440           | 1080            |

Scenario: Enemy missile angles 04 wave enemies enter from varied sky origins
  And the current wave has <remaining> scheduled enemies still active
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <expected_remaining> enemy missiles in flight
  And every enemy missile origin y is 0
  And every enemy missile origin x is within the playfield
  And the enemy missiles use more than one distinct origin x
  And at least one enemy missile origin x differs from its target x

Examples:
  | width | height | remaining | expected_remaining | expected_width | expected_height |
  | 800   | 600    | 3         | 3                  | 800            | 600             |
  | 800   | 600    | 4         | 4                  | 800            | 600             |
  | 1920  | 1080   | 3         | 3                  | 1440           | 1080            |

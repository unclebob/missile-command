# mutation-stamp: sha256=cc4e2ac8ce7daff7980c71159c1ad7c521baa19122cd83b4b20bd629f385c373
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-25T16:09:55.787045Z","feature_name":"Bonus cities","feature_path":"features/bonus-cities.feature","background_hash":"2ad6c53e758cb94bd90a7e42d0ca6dc51bfdcaa4e3368f8e8c551a4cb55a221c","implementation_hash":"sha256:0baade5845e27004e3ccd66c32e443e46ebee130f3b01bbd60c35934e609469f","scenarios":[{"index":0,"name":"Bonus cities 01 new game has empty bonus city reserve","scenario_hash":"b39b4da8da43f62f623e7cc1665255e127d1e224e34bbe17fdb06ec89f8d0dce","mutation_count":14,"result":{"Total":14,"Killed":14,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:09:10.551173Z"},{"index":1,"name":"Bonus cities 02 crossing the score threshold awards a reserve city","scenario_hash":"24e7687782eaf876cc44bbf4d94a77cfac4c6c66054986f58d9b7061a37de8e7","mutation_count":32,"result":{"Total":32,"Killed":32,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:09:10.551173Z"},{"index":2,"name":"Bonus cities 02b alternate threshold awards at five thousand","scenario_hash":"2cffeb1ad755f475492efebc081673255bf506875b135cb7f9030c909c2c1c43","mutation_count":7,"result":{"Total":7,"Killed":7,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:09:10.551173Z"},{"index":3,"name":"Bonus cities 03 each crossed threshold awards one reserve city","scenario_hash":"6b1180249a0ce08ab08000d8d06c7094f1e49307a7af42cbd855ee417afa2a91","mutation_count":32,"result":{"Total":32,"Killed":32,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:09:10.551173Z"},{"index":4,"name":"Bonus cities 04 earning a bonus does not restore cities mid-wave","scenario_hash":"9c868f2d9d37b47dc77772ce8cda9c7b5c9af2b4b69823ab0fbf307da9cafdc8","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:09:10.551173Z"},{"index":5,"name":"Bonus cities 04b mid-wave bonus leaves other destroyed cities unrestored","scenario_hash":"976beaa6d32457eb8b51882856153bd5d60ba03c6926002202f9907de4b836ce","mutation_count":5,"result":{"Total":5,"Killed":5,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:09:10.551173Z"},{"index":6,"name":"Bonus cities 04c mid-wave bonus leaves last city unrestored","scenario_hash":"f75fdd8dc5e594084d09d89e5514a250807fd47ea8db04facf8bd8ee0696ae84","mutation_count":5,"result":{"Total":5,"Killed":5,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:09:10.551173Z"},{"index":7,"name":"Bonus cities 05 multiple threshold awards stay in reserve until wave end","scenario_hash":"7186a1f2e5df87a261ad444caa9c40ba60fb5e2b63a557b50b7df746317c8fcf","mutation_count":28,"result":{"Total":28,"Killed":28,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:09:10.551173Z"},{"index":8,"name":"Bonus cities 06 remaining reserve restores after wave resolution","scenario_hash":"511c5ac459522243e6b62d9277033ce1c8b4202a57a1848cc168da11550e78ac","mutation_count":28,"result":{"Total":28,"Killed":28,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:09:10.551173Z"},{"index":9,"name":"Bonus cities 07 bonus city earned event is recorded","scenario_hash":"a223c88fccddce27f9e52cab89474cf152254150cfe032fcec57b9c9fe79f310","mutation_count":32,"result":{"Total":32,"Killed":32,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:09:10.551173Z"}]}
# acceptance-mutation-manifest-end

# Bonus cities 01 new game has empty bonus city reserve
# Bonus cities 02 crossing the score threshold awards a reserve city
# Bonus cities 03 each crossed threshold awards one reserve city
# Bonus cities 04 earning a bonus does not restore cities mid-wave
# Bonus cities 05 multiple threshold awards stay in reserve until wave end
# Bonus cities 06 remaining reserve restores after wave resolution
# Bonus cities 07 bonus city earned event is recorded
Feature: Bonus cities

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: Bonus cities 01 new game has empty bonus city reserve
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <score>
  And the bonus city reserve is <reserve>
  And there are <living_cities> living cities

Examples:
  | width | height | score | reserve | living_cities | expected_width | expected_height |
  | 800   | 600    | 0     | 0       | 6             | 800            | 600             |
  | 1920  | 1080   | 0     | 0       | 6             | 1920           | 1080            |

Scenario: Bonus cities 02 crossing the score threshold awards a reserve city
  And the bonus city threshold is 10000
  When the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <expected_score>
  And the bonus city reserve is <reserve>
  And there are <living_cities> living cities

Examples:
  | width | height | score | expected_score | reserve | living_cities | expected_width | expected_height |
  | 800   | 600    | 9999  | 9999           | 0       | 6             | 800            | 600             |
  | 800   | 600    | 10000 | 10000          | 1       | 6             | 800            | 600             |
  | 800   | 600    | 10001 | 10001          | 1       | 6             | 800            | 600             |
  | 1920  | 1080   | 10000 | 10000          | 1       | 6             | 1920           | 1080            |

Scenario: Bonus cities 02b alternate threshold awards at five thousand
  And the bonus city threshold is 5000
  When the score becomes 5000
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <expected_score>
  And the bonus city reserve is <reserve>
  And there are <living_cities> living cities

Examples:
  | width | height | expected_score | reserve | living_cities | expected_width | expected_height |
  | 1920  | 1080   | 5000           | 1       | 6             | 1920           | 1080            |

Scenario: Bonus cities 03 each crossed threshold awards one reserve city
  And the bonus city threshold is 10000
  When the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <expected_score>
  And the bonus city reserve is <reserve>
  And there are <living_cities> living cities

Examples:
  | width | height | score | expected_score | reserve | living_cities | expected_width | expected_height |
  | 800   | 600    | 20000 | 20000          | 2       | 6             | 800            | 600             |
  | 800   | 600    | 29999 | 29999          | 2       | 6             | 800            | 600             |
  | 800   | 600    | 30000 | 30000          | 3       | 6             | 800            | 600             |
  | 1920  | 1080   | 40000 | 40000          | 4       | 6             | 1920           | 1080            |

Scenario: Bonus cities 04 earning a bonus does not restore cities mid-wave
  And the bonus city threshold is 10000
  And city 0 has been destroyed
  And there are 5 living cities
  When the score becomes 10000
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <expected_score>
  And there are 5 living cities
  And city 0 is not living
  And the bonus city reserve is <reserve>

Examples:
  | width | height | expected_score | reserve | expected_width | expected_height |
  | 800   | 600    | 10000          | 1       | 800            | 600             |
  | 1920  | 1080   | 10000          | 1       | 1920           | 1080            |

Scenario: Bonus cities 04b mid-wave bonus leaves other destroyed cities unrestored
  And the bonus city threshold is 10000
  And city 3 has been destroyed
  And there are 5 living cities
  When the score becomes 10000
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 5 living cities
  And city 3 is not living
  And the bonus city reserve is <reserve>

Examples:
  | width | height | reserve | expected_width | expected_height |
  | 800   | 600    | 1       | 800            | 600             |

Scenario: Bonus cities 04c mid-wave bonus leaves last city unrestored
  And the bonus city threshold is 10000
  And city 5 has been destroyed
  And there are 5 living cities
  When the score becomes 10000
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 5 living cities
  And city 5 is not living
  And the bonus city reserve is <reserve>

Examples:
  | width | height | reserve | expected_width | expected_height |
  | 1920  | 1080   | 1       | 1920           | 1080            |

Scenario: Bonus cities 05 multiple threshold awards stay in reserve until wave end
  And the bonus city threshold is 10000
  And city 0 has been destroyed
  And city 1 has been destroyed
  And there are 4 living cities
  When the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 4 living cities
  And the score is <expected_score>
  And the bonus city reserve is <reserve>

Examples:
  | width | height | score | expected_score | reserve | expected_width | expected_height |
  | 800   | 600    | 10000 | 10000          | 1       | 800            | 600             |
  | 800   | 600    | 20000 | 20000          | 2       | 800            | 600             |
  | 800   | 600    | 30000 | 30000          | 3       | 800            | 600             |
  | 1920  | 1080   | 30000 | 30000          | 3       | 1920           | 1080            |

Scenario: Bonus cities 06 remaining reserve restores after wave resolution
  And city 0 has been destroyed
  And city 1 has been destroyed
  And the bonus city reserve is set to <starting_reserve>
  And there are 4 living cities
  When bonus cities from reserve are applied after wave resolution
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <living_cities> living cities
  And the bonus city reserve is <expected_reserve>

Examples:
  | width | height | starting_reserve | living_cities | expected_reserve | expected_width | expected_height |
  | 800   | 600    | 1                | 5             | 0                | 800            | 600             |
  | 800   | 600    | 2                | 6             | 0                | 800            | 600             |
  | 800   | 600    | 3                | 6             | 1                | 800            | 600             |
  | 1920  | 1080   | 2                | 6             | 0                | 1920           | 1080            |

Scenario: Bonus cities 07 bonus city earned event is recorded
  And the bonus city threshold is 10000
  When the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <expected_score>
  And the number of bonus city earned events is <event_count>
  And the bonus city reserve is <reserve>

Examples:
  | width | height | score | expected_score | event_count | reserve | expected_width | expected_height |
  | 800   | 600    | 9999  | 9999           | 0           | 0       | 800            | 600             |
  | 800   | 600    | 10000 | 10000          | 1           | 1       | 800            | 600             |
  | 800   | 600    | 30000 | 30000          | 3           | 3       | 800            | 600             |
  | 1920  | 1080   | 20000 | 20000          | 2           | 2       | 1920           | 1080            |

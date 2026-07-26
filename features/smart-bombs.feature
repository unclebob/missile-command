# mutation-stamp: sha256=7a8dbf623774b2d27f32c015e8aec6975aed944e9ff8c519ec21ade170c309d4
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-26T15:36:27.123459Z","feature_name":"Smart bombs","feature_path":"features/smart-bombs.feature","background_hash":"2ad6c53e758cb94bd90a7e42d0ca6dc51bfdcaa4e3368f8e8c551a4cb55a221c","implementation_hash":"sha256:c3c8d8e67ca3fa08f40eaaa5ecf64c3b7d02769213aebe83e00f472ae438d6db","scenarios":[{"index":0,"name":"Smart bombs 01 a smart bomb advances toward its target","scenario_hash":"ecb29949897336e6c8e88a4b5cc1e7b394a8af295530ac2c57702b23c28bbea7","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"},{"index":1,"name":"Smart bombs 01b advances toward city three","scenario_hash":"52cba8d2974ffe2ce2b42b115f71c0d9b2aa0c54d7f36650f7dc3e3702f5063e","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"},{"index":2,"name":"Smart bombs 02 a well centered fireball destroys a smart bomb","scenario_hash":"d824ed2126f75237d05f0b75c7cb033e73d6d951186134dcb76e5e2c8955f51e","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"},{"index":3,"name":"Smart bombs 03 an edge of blast fireball is evaded once","scenario_hash":"c317f4773fd751f9d1e8d3a9ac3260e2e1928bc07e29b4e193314983db054d53","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"},{"index":4,"name":"Smart bombs 04 after evasion the smart bomb remains a threat","scenario_hash":"e834b8d2f682d249271e2d27b457251c85df4f34a3f9fb0989b3b7b647422e8e","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"},{"index":5,"name":"Smart bombs 05 a second well centered fireball destroys an escaped smart bomb","scenario_hash":"7369b801649cf4a53e7770c65bbee0114170f40dddfe06ccc786c7bf6e6428e6","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"},{"index":6,"name":"Smart bombs 06 an unintercepted smart bomb destroys its target city","scenario_hash":"1eaf3862e4b9a02bcdc7a05f8b4a27a296a3a24d7c80d1334f5ef8bc577bdeda","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"},{"index":7,"name":"Smart bombs 07 destroying a smart bomb awards one hundred twenty five times multiplier","scenario_hash":"7a7039bad0d13945fba2df3fa86b641cf92e8584c1a369cdc0eaefd3413006bd","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"},{"index":8,"name":"Smart bombs 07b smart bomb kill awards double at wave three","scenario_hash":"033734622587b8c941675a45f3a0f6d26b061708ee887342a58ae9b552393120","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"},{"index":9,"name":"Smart bombs 08 wave one schedules no smart bombs","scenario_hash":"abdd3a60233f3756d1b4b7196ec45bfd1ba476b887b88b608d437c6ae60bfacc","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"},{"index":10,"name":"Smart bombs 08b wave four schedules one smart bomb","scenario_hash":"88ad1ff0fd54d6f07acd63b8ec1142bf0056b3a30cd7daef1d0fc4118f0b25e8","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"},{"index":11,"name":"Smart bombs 08c wave six schedules two smart bombs","scenario_hash":"10a750d0d650420427e57cc76108eabf1ab409c61c8a2e7020d16dba30024144","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:17.953614Z"}]}
# acceptance-mutation-manifest-end

# Smart bombs 01 a smart bomb advances toward its target
# Smart bombs 02 a well centered fireball destroys a smart bomb
# Smart bombs 03 an edge of blast fireball is evaded once
# Smart bombs 04 after evasion the smart bomb remains a threat
# Smart bombs 05 a second well centered fireball destroys an escaped smart bomb
# Smart bombs 06 an unintercepted smart bomb destroys its target city
# Smart bombs 07 destroying a smart bomb awards one hundred twenty five times multiplier
# Smart bombs 08 later waves schedule smart bombs
Feature: Smart bombs

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: Smart bombs 01 a smart bomb advances toward its target
  And a smart bomb targeting city 0
  When time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 1 enemy missiles in flight
  And there is 1 smart bomb in flight
  And a smart bomb has progressed toward city 0

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Smart bombs 01b advances toward city three
  And a smart bomb targeting city 3
  When time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there is 1 smart bomb in flight
  And a smart bomb has progressed toward city 3

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Smart bombs 02 a well centered fireball destroys a smart bomb
  And a smart bomb targeting city 1
  And a fireball at 400 250 with radius 40
  And the smart bomb path is centered in that fireball within distance 15
  When time advances until the smart bomb is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the smart bomb is destroyed by the fireball
  And there are 0 enemy missiles in flight
  And there are 6 living cities

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Smart bombs 03 an edge of blast fireball is evaded once
  And a smart bomb targeting city 1
  And a fireball at 400 250 with radius 40
  And the smart bomb path is only in the edge band of that fireball between 25 and 40
  When time advances until the smart bomb would enter the fireball edge band or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the smart bomb has evaded the fireball
  And there is 1 smart bomb in flight
  And there are 6 living cities

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Smart bombs 04 after evasion the smart bomb remains a threat
  And a smart bomb targeting city 1
  And a fireball at 400 250 with radius 40
  And the smart bomb path is only in the edge band of that fireball between 25 and 40
  When time advances until the smart bomb would enter the fireball edge band or has impacted
  And time advances by 0.2 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there is 1 smart bomb in flight
  And a smart bomb has progressed toward city 1
  And the smart bomb is not destroyed by the fireball

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Smart bombs 05 a second well centered fireball destroys an escaped smart bomb
  And a smart bomb targeting city 1
  And a fireball at 400 250 with radius 40
  And the smart bomb path is only in the edge band of that fireball between 25 and 40
  When time advances until the smart bomb would enter the fireball edge band or has impacted
  Then the smart bomb has evaded the fireball
  And a fireball at 400 350 with radius 40
  And the smart bomb path is centered in that fireball within distance 15
  When time advances until the smart bomb is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the smart bomb is destroyed by the fireball
  And there are 0 enemy missiles in flight

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Smart bombs 06 an unintercepted smart bomb destroys its target city
  And a smart bomb targeting city 0
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And city 0 is not living
  And there are 5 living cities
  And there are 0 enemy missiles in flight

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Smart bombs 07 destroying a smart bomb awards one hundred twenty five times multiplier
  When the game is at wave 1
  And a smart bomb targeting city 1
  And an enemy missile targeting city 0
  And a fireball at 400 250 with radius 40
  And the smart bomb path is centered in that fireball within distance 15
  When time advances until the smart bomb is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the smart bomb is destroyed by the fireball
  And there is 1 enemy missile in flight
  And the wave is not complete
  And the multiplier is 1
  And the score is 125

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Smart bombs 07b smart bomb kill awards double at wave three
  When the game is at wave 3
  And a smart bomb targeting city 1
  And an enemy missile targeting city 0
  And a fireball at 400 250 with radius 40
  And the smart bomb path is centered in that fireball within distance 15
  When time advances until the smart bomb is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the smart bomb is destroyed by the fireball
  And the multiplier is 2
  And the score is 250

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Smart bombs 08 wave one schedules no smart bombs
  When the game is at wave 1
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave 1 smart bomb schedule count is 0

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Smart bombs 08b wave four schedules one smart bomb
  When the game is at wave 4
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave 4 smart bomb schedule count is 1

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Smart bombs 08c wave six schedules two smart bombs
  When the game is at wave 6
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave 6 smart bomb schedule count is 2

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

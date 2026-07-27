# mutation-stamp: sha256=f0f81b9be6cc27829d47f08eb20fc0123a537e017c8fefb8f734d36514ba057d
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-27T18:35:06.015066Z","feature_name":"MIRV warheads","feature_path":"features/mirv-warheads.feature","background_hash":"2ad6c53e758cb94bd90a7e42d0ca6dc51bfdcaa4e3368f8e8c551a4cb55a221c","implementation_hash":"sha256:3d26faffec4f8d613e790406c1926b54cde6e7f9ef4617ee1e0978236d5888b3","scenarios":[{"index":0,"name":"MIRV warheads 01 a MIRV is a single enemy before it splits","scenario_hash":"27e9c621f55c18229d99082f5a7402cce1ff79196f93a066da0182794ff107b1","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T17:06:29.900451Z"},{"index":1,"name":"MIRV warheads 01b MIRV parent before split on large playfield","scenario_hash":"ec9fd98cc5d8dc09b986a87960fc4f5da8859fec2348ac3f8a0b01c00a3d8dd3","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T17:06:29.900451Z"},{"index":2,"name":"MIRV warheads 02 a MIRV splits into three warheads","scenario_hash":"76b1c4de557d01aebf523107cdfe386eddaa18ea4a852ce68c8b9fe8e1b5a0d2","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T17:06:29.900451Z"},{"index":3,"name":"MIRV warheads 02b a MIRV splits into two warheads","scenario_hash":"6b7f153359a5ea741b97c3d0b521ba8f8b8cc0e1027aa95cbd6d92ea8077b1d7","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T17:06:29.900451Z"},{"index":4,"name":"MIRV warheads 03 child warheads proceed toward independent targets","scenario_hash":"ece627b1fe5b0e430f27d85d5ca2412e9e9832bd5afd43346621b038394bcb73","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T17:06:29.900451Z"},{"index":5,"name":"MIRV warheads 04 destroying a MIRV before split prevents children","scenario_hash":"4eae0a004231c28cda151fc2a587920de2472e6223859e575900dbde809e742a","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T17:06:29.900451Z"},{"index":6,"name":"MIRV warheads 05 an unintercepted child warhead destroys its target city","scenario_hash":"be6952751792026ca2aa4c5cc4f2a5324ac144a1ae911c725b17c3fe3cad5b83","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T17:06:29.900451Z"},{"index":7,"name":"MIRV warheads 06 a fireball destroys a child warhead","scenario_hash":"9c4ea21c2c3670c221f81454609467503e5043e1eea8be9ad51291f49b113e5b","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T17:06:29.900451Z"},{"index":8,"name":"MIRV warheads 07 wave one schedules no MIRVs","scenario_hash":"74a804932917cf086f52c98a30220fabf41805c8b1be004b15cdccb53f44fff5","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T17:06:29.900451Z"},{"index":9,"name":"MIRV warheads 07b wave four schedules one MIRV","scenario_hash":"6f798b134778a4818318d1d51d40e3f3c617115291a9d50ed9b5f854763cf85b","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T17:06:29.900451Z"},{"index":10,"name":"MIRV warheads 07c wave six schedules two MIRVs","scenario_hash":"887e3cdfa65f8e9ad1e4a54588050d1f57a32757f1eeddb98f9fea54094e7c71","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T17:06:29.900451Z"}]}
# acceptance-mutation-manifest-end

# MIRV warheads 01 a MIRV is a single enemy before it splits
# MIRV warheads 02 a MIRV splits into multiple warheads at the split progress
# MIRV warheads 03 child warheads proceed toward independent targets
# MIRV warheads 04 destroying a MIRV before split prevents children
# MIRV warheads 05 an unintercepted child warhead destroys its target city
# MIRV warheads 06 a fireball destroys a child warhead
# MIRV warheads 07 later waves schedule MIRV capable enemies
Feature: MIRV warheads

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: MIRV warheads 01 a MIRV is a single enemy before it splits
  And a MIRV enemy missile targeting city 0 that splits into 3 warheads at progress 0.5
  When time advances by 0.05 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 1 enemy missiles in flight
  And there is 1 MIRV parent in flight
  And the first enemy missile progress is less than 0.5

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: MIRV warheads 01b MIRV parent before split on large playfield
  And a MIRV enemy missile targeting city 5 that splits into 3 warheads at progress 0.5
  When time advances by 0.05 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there is 1 MIRV parent in flight
  And the first enemy missile progress is less than 0.5

Examples:
  | width | height | expected_width | expected_height |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: MIRV warheads 02 a MIRV splits into three warheads
  And a MIRV enemy missile targeting city 0 that splits into 3 warheads at progress 0.5
  When time advances until the MIRV has split or all enemies are gone
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 3 enemy missiles in flight
  And there are 0 MIRV parents in flight
  And every in-flight enemy is a MIRV child warhead

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: MIRV warheads 02b a MIRV splits into two warheads
  And a MIRV enemy missile targeting city 1 that splits into 2 warheads at progress 0.45
  When time advances until the MIRV has split or all enemies are gone
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 2 enemy missiles in flight
  And every in-flight enemy is a MIRV child warhead

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: MIRV warheads 03 child warheads proceed toward independent targets
  And a MIRV enemy missile targeting city 0 that splits into 3 warheads at progress 0.5
  When time advances until the MIRV has split or all enemies are gone
  And time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 3 enemy missiles in flight
  And the MIRV child warheads target more than one distinct target
  And every MIRV child warhead has progressed toward its target

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: MIRV warheads 04 destroying a MIRV before split prevents children
  And a MIRV enemy missile targeting city 1 that splits into 3 warheads at progress 0.5
  And a fireball at 400 100 with radius 40
  And the enemy missile path passes within distance 40 of that fireball center
  When time advances until the enemy missile is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the enemy missile is destroyed by the fireball
  And there are 0 enemy missiles in flight
  And there are 0 MIRV parents in flight
  And there are 6 living cities

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: MIRV warheads 05 an unintercepted child warhead destroys its target city
  And a MIRV enemy missile targeting city 0 that splits into 2 warheads at progress 0.5
  When time advances until the MIRV has split or all enemies are gone
  And time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 0 enemy missiles in flight
  And there are 4 living cities

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: MIRV warheads 06 a fireball destroys a child warhead
  And a MIRV enemy missile targeting city 0 that splits into 3 warheads at progress 0.5
  When time advances until the MIRV has split or all enemies are gone
  And a fireball at 400 350 with radius 40
  And the first MIRV child warhead path passes within distance 40 of that fireball center
  When time advances until the first MIRV child is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the first MIRV child warhead is destroyed by the fireball
  And there are 2 enemy missiles in flight

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: MIRV warheads 07 wave one schedules no MIRVs
  When the game is at wave 1
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave 1 MIRV schedule count is 0

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: MIRV warheads 07b wave four schedules one MIRV
  When the game is at wave 4
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave 4 MIRV schedule count is 1

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: MIRV warheads 07c wave six schedules two MIRVs
  When the game is at wave 6
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave 6 MIRV schedule count is 2

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

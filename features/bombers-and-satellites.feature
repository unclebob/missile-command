# mutation-stamp: sha256=44b2c517aa063527410002a3d275aab600400fecd96a2ac291b4f757af5482ec
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-27T18:34:54.425611Z","feature_name":"Bombers and satellites","feature_path":"features/bombers-and-satellites.feature","background_hash":"2ad6c53e758cb94bd90a7e42d0ca6dc51bfdcaa4e3368f8e8c551a4cb55a221c","implementation_hash":"sha256:f0bcd573abc2d2d59eceda81c9c09da22d773cc4c7c7cf84cabe6cd5dbd92ed9","scenarios":[{"index":0,"name":"Bombers and satellites 01 bomber moves across the playfield","scenario_hash":"749e3250e5ded1e0081709ac572f5a9309015b6e8465d4227f230e40bda2324b","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":1,"name":"Bombers and satellites 01b satellite moves across the playfield","scenario_hash":"6f21355b6fbb7809f281cceb249845bb69f57fb0717d2a0cc5a8ca6c6aafcf13","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":2,"name":"Bombers and satellites 01c bomber moves on a large playfield","scenario_hash":"e84bf7f96b37a2ff8d6a455394710439cb7f53d70dcb294ec4e9c83c46af42b8","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":3,"name":"Bombers and satellites 02 a flyer drops enemy missiles during its pass","scenario_hash":"850d37760954549b2731a5b0222885256a0f9ad9fe623efcbe5da282a1dad8cb","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":4,"name":"Bombers and satellites 02b satellite drops one missile","scenario_hash":"447648819772ac47fdfeb508d7c3296e710854de04f36b333ca72d3b4c61a604","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":5,"name":"Bombers and satellites 03 a fireball destroys a bomber","scenario_hash":"ac2f955a18b5654c302f0e946a63661757d5aa91ffbacb67afc8fb8066e78910","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":6,"name":"Bombers and satellites 03b a fireball destroys a satellite","scenario_hash":"fa1429a9e454e3fa1fd492fd52687a184e24afe584ec5357ede6be515257519e","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":7,"name":"Bombers and satellites 04 destroying a flyer awards one hundred times multiplier","scenario_hash":"0a572ad53b12df3ddcba60577538d56b1a21fdca10577938736e4d36b4dedb70","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":8,"name":"Bombers and satellites 04b flyer kill awards double at wave three","scenario_hash":"9aa9d022d65868a5da13b3ea9dc16152cf2afc70bb3a1494b1fe77c325612503","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":9,"name":"Bombers and satellites 05 destroying a flyer stops further drops","scenario_hash":"2c277acedd1b6a6bade565af69987b1a10267febdc00f9029fecc21963318fdd","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":10,"name":"Bombers and satellites 06 an unintercepted dropped missile destroys a city","scenario_hash":"5b7075dba388563e02202a51f253d8456baf5e69428c34cfbcde2b8ac513b9fe","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":11,"name":"Bombers and satellites 07 wave one schedules no flyers","scenario_hash":"d7920818b085fcc18624f93070c38139ef4d22306844ddf1be61995931ace9ec","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":12,"name":"Bombers and satellites 07b wave four schedules one bomber","scenario_hash":"e9f3a46df288b0316b8bc1d26dce741058735965c2d8161b3c010c1fb860fddf","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"},{"index":13,"name":"Bombers and satellites 07c wave five schedules bomber and satellite","scenario_hash":"e5e620cfd1e03b2cff4fe5f2b43a87727bbd1e1b42d38f877ca90a1ca4753fbe","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:17:15.823411Z"}]}
# acceptance-mutation-manifest-end

# Bombers and satellites 01 a flyer moves across the playfield
# Bombers and satellites 02 a flyer drops enemy missiles during its pass
# Bombers and satellites 03 a fireball destroys a flyer
# Bombers and satellites 04 destroying a flyer awards one hundred times multiplier
# Bombers and satellites 05 destroying a flyer stops further drops
# Bombers and satellites 06 an unintercepted dropped missile destroys a city
# Bombers and satellites 07 later waves schedule bombers and satellites
Feature: Bombers and satellites

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: Bombers and satellites 01 bomber moves across the playfield
  And a bomber flyer from 0 80 toward 800 80 at speed 100
  When time advances by 0.2 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there is 1 bomber flyer in flight
  And the bomber flyer has progressed along its path
  And the bomber flyer y is 80

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 01b satellite moves across the playfield
  And a satellite flyer from 800 50 toward 0 50 at speed 120
  When time advances by 0.2 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there is 1 satellite flyer in flight
  And the satellite flyer has progressed along its path
  And the satellite flyer y is 50

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 01c bomber moves on a large playfield
  And a bomber flyer from 0 120 toward 1920 120 at speed 150
  When time advances by 0.2 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there is 1 bomber flyer in flight
  And the bomber flyer has progressed along its path
  And the bomber flyer y is 120

Examples:
  | width | height | expected_width | expected_height |
  | 1920  | 1080   | 1440           | 1080            |

Scenario: Bombers and satellites 02 a flyer drops enemy missiles during its pass
  And a bomber flyer from 0 80 toward 800 80 at speed 100
  And the flyer drops 2 enemy missiles toward living cities at path progress 0.5
  When time advances until the flyer has passed drop progress 0.5
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there is 1 bomber flyer in flight
  And there are 2 enemy missiles in flight
  And every dropped enemy missile originates at the flyer position

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 02b satellite drops one missile
  And a satellite flyer from 800 50 toward 0 50 at speed 120
  And the flyer drops 1 enemy missiles toward living cities at path progress 0.35
  When time advances until the flyer has passed drop progress 0.35
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there is 1 satellite flyer in flight
  And there are 1 enemy missiles in flight
  And every dropped enemy missile originates at the flyer position

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 03 a fireball destroys a bomber
  And a bomber flyer from 0 80 toward 800 80 at speed 100
  And a fireball at 400 80 with radius 40
  And the flyer path passes within distance 40 of that fireball center
  When time advances until the flyer is inside the fireball radius or has left the playfield
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the bomber flyer is destroyed by the fireball
  And there are 0 bomber flyers in flight

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 03b a fireball destroys a satellite
  And a satellite flyer from 0 50 toward 800 50 at speed 120
  And a fireball at 400 50 with radius 40
  And the flyer path passes within distance 40 of that fireball center
  When time advances until the flyer is inside the fireball radius or has left the playfield
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the satellite flyer is destroyed by the fireball
  And there are 0 satellite flyers in flight

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 04 destroying a flyer awards one hundred times multiplier
  When the game is at wave 1
  And a bomber flyer from 0 80 toward 800 80 at speed 100
  And an enemy missile targeting city 0
  And a fireball at 400 80 with radius 40
  And the flyer path passes within distance 40 of that fireball center
  When time advances until the flyer is inside the fireball radius or has left the playfield
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the bomber flyer is destroyed by the fireball
  And there is 1 enemy missile in flight
  And the wave is not complete
  And the multiplier is 1
  And the score is 100

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 04b flyer kill awards double at wave three
  When the game is at wave 3
  And a bomber flyer from 0 80 toward 800 80 at speed 100
  And an enemy missile targeting city 1
  And a fireball at 400 80 with radius 40
  And the flyer path passes within distance 40 of that fireball center
  When time advances until the flyer is inside the fireball radius or has left the playfield
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the bomber flyer is destroyed by the fireball
  And the multiplier is 2
  And the score is 200

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 05 destroying a flyer stops further drops
  And a bomber flyer from 0 80 toward 800 80 at speed 100
  And the flyer drops 2 enemy missiles toward living cities at path progress 0.6
  And a fireball at 100 80 with radius 40
  And the flyer path passes within distance 40 of that fireball center
  When time advances until the flyer is inside the fireball radius or has left the playfield
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the bomber flyer is destroyed by the fireball
  And there are 0 bomber flyers in flight
  And there are 0 enemy missiles in flight

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 06 an unintercepted dropped missile destroys a city
  And a bomber flyer from 0 80 toward 800 80 at speed 100
  And the flyer drops 1 enemy missile targeting city 0 at path progress 0.3
  When time advances until the flyer has passed drop progress 0.3
  And time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And city 0 is not living
  And there are 5 living cities
  And there are 0 enemy missiles in flight

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 07 wave one schedules no flyers
  When the game is at wave 1
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave 1 bomber schedule count is 0
  And wave 1 satellite schedule count is 0

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 07b wave four schedules one bomber
  When the game is at wave 4
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave 4 bomber schedule count is 1
  And wave 4 satellite schedule count is 0

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |

Scenario: Bombers and satellites 07c wave five schedules bomber and satellite
  When the game is at wave 5
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And wave 5 bomber schedule count is 1
  And wave 5 satellite schedule count is 1

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1440           | 1080            |

# mutation-stamp: sha256=cae5e335acdba8561731c1473806fc212aca1189106d6bb7a64d15433a537b78
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-27T18:35:13.188672Z","feature_name":"Title screen","feature_path":"features/title-screen.feature","background_hash":"8e63f035c8dab0c09e62ed95cd3dfb2f8ecc23b566cec139f18d3206495fbae2","implementation_hash":"sha256:024521d7224b933a4e6da75ed4e532759a5cdbf4dd38224a081fc8d138cc7ced","scenarios":[{"index":4,"name":"Title screen 05 start resets score and wave for a new run","scenario_hash":"c6e61a9877c9ac42c412aaafb6dcd699a6cea733cec8bf6c92464cd56ebd2b86","mutation_count":33,"result":{"Total":33,"Killed":33,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:51:43.587078Z"},{"index":6,"name":"Title screen 07 fire does not launch on the title screen","scenario_hash":"6e70b8b9a9b5cdd847010d057c359134ee24fec1513bce819834784f7ab0d937","mutation_count":20,"result":{"Total":20,"Killed":20,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:51:43.587078Z"},{"index":0,"name":"Title screen 01 a new session starts on the title screen","scenario_hash":"bc687455e6c1799a384a2672550314022460eb44dc347550bc8e9b3557578e6c","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:50:18.101442Z"},{"index":1,"name":"Title screen 02 the title screen shows the game name","scenario_hash":"a124cfeb29e4948e00217cf8f223935aacc2ad0cdfb6a2637d263b7659c04ea6","mutation_count":10,"result":{"Total":10,"Killed":10,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:50:18.101442Z"},{"index":2,"name":"Title screen 03 the title screen shows a start affordance","scenario_hash":"62482b2390428a17914da0ba2c002ebb9b1e034c16098988c5fcafb4f7f49b7e","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:50:18.101442Z"},{"index":3,"name":"Title screen 04 start enters playing with a fresh layout","scenario_hash":"3cae391b37e15c62a33eec020a74fb8f93b77825c779b41ef9223f0b2b0e3a25","mutation_count":21,"result":{"Total":21,"Killed":21,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:50:18.101442Z"},{"index":5,"name":"Title screen 06 start uses the current playfield dimensions","scenario_hash":"550468c47539781d08719e2392bf104c9e384504156c2b7d4f9304d192f6d03e","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:50:18.101442Z"},{"index":7,"name":"Title screen 08 confirm from THE END without high score returns to title","scenario_hash":"8b7ac708d5cc92b7bc7cab22cceafceacb470a2cd4fef0de0b7b6df13e819a8a","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T20:50:18.101442Z"}]}
# acceptance-mutation-manifest-end

# Title screen 01 a new session starts on the title screen
# Title screen 02 the title screen shows the game name
# Title screen 03 the title screen shows a start affordance
# Title screen 04 start enters playing with a fresh layout
# Title screen 05 start resets score and wave for a new run
# Title screen 06 start uses the current playfield dimensions
# Title screen 07 fire does not launch on the title screen
# Title screen 08 confirm from THE END without high score returns to title
Feature: Title screen

Background:
  Given a new game with width <width> and height <height>

Scenario: Title screen 01 a new session starts on the title screen
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1440           | 1080            |
  | 1024  | 768    | 1024           | 768             |

Scenario: Title screen 02 the title screen shows the game name
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title
  And the title game name is <game_name>

Examples:
  | width | height | game_name       | expected_width | expected_height |
  | 800   | 600    | Missile Command | 800            | 600             |
  | 1920  | 1080   | Missile Command | 1440           | 1080            |

Scenario: Title screen 03 the title screen shows a start affordance
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title
  And the title shows a start affordance

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1440           | 1080            |

Scenario: Title screen 04 start enters playing with a fresh layout
  When the player starts the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And there are <city_count> living cities
  And there are <battery_count> non-destroyed batteries named left center and right
  And each battery has <ammo> missiles

Examples:
  | width | height | city_count | battery_count | ammo | expected_width | expected_height |
  | 800   | 600    | 6          | 3             | 10   | 800            | 600             |
  | 1920  | 1080   | 6          | 3             | 10   | 1440           | 1080            |
  | 1024  | 768    | 6          | 3             | 10   | 1024           | 768             |

Scenario: Title screen 05 start resets score and wave for a new run
  When the score becomes <prior_score>
  And the game is at wave <prior_wave>
  Then the score is <expected_prior_score>
  And the wave number is <expected_prior_wave>
  When the player starts the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And the score is <score>
  And the wave number is <wave>
  And the multiplier is <multiplier>

Examples:
  | width | height | prior_score | prior_wave | expected_prior_score | expected_prior_wave | score | wave | multiplier | expected_width | expected_height |
  | 800   | 600    | 2500        | 5          | 2500                 | 5                   | 0     | 1    | 1          | 800            | 600             |
  | 800   | 600    | 99999       | 12         | 99999                | 12                  | 0     | 1    | 1          | 800            | 600             |
  | 1920  | 1080   | 100         | 3          | 100                  | 3                   | 0     | 1    | 1          | 1440           | 1080            |

Scenario: Title screen 06 start uses the current playfield dimensions
  When the player starts the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And the horizontal span of the cities is greater than half of width <expected_width>

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1600  | 600    | 800            | 600             |
  | 1920  | 1080   | 1440           | 1080            |

Scenario: Title screen 07 fire does not launch on the title screen
  When the player aims at 400 200
  And the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title
  And there are 0 defensive missiles in flight

Examples:
  | width | height | battery | expected_width | expected_height |
  | 800   | 600    | left    | 800            | 600             |
  | 800   | 600    | center  | 800            | 600             |
  | 800   | 600    | right   | 800            | 600             |
  | 1920  | 1080   | center  | 1440           | 1080            |

Scenario: Title screen 08 confirm from THE END without high score returns to title
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  Then the game is at THE END
  When the player confirms the end screen
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1440           | 1080            |
  | 1024  | 768    | 1024           | 768             |

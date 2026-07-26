# mutation-stamp: sha256=5dff789ea7017e78efc864104dc92b1666a1ff18c34bbefb71f914536e7063b5
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-26T15:05:52.944197Z","feature_name":"New game layout","feature_path":"features/new-game-layout.feature","background_hash":"2ad6c53e758cb94bd90a7e42d0ca6dc51bfdcaa4e3368f8e8c551a4cb55a221c","implementation_hash":"sha256:2685394debdfa12bcc0477f2147f7785220a764e6ec2192465e3a0fe54be09b8","scenarios":[{"index":0,"name":"New game layout 01 starting cities and batteries","scenario_hash":"4b42f2b3b0c314dfce58ac48c128f01e63fa1f7aa916334b4329e2443c7597dd","mutation_count":21,"result":{"Total":21,"Killed":21,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:52.944197Z"},{"index":1,"name":"New game layout 02 cities ordered on the ground","scenario_hash":"a88672d809e2ec02de52ce7867602cef75cc9652c9effa9c1b92bcbde04ff040","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:52.944197Z"},{"index":2,"name":"New game layout 03 batteries left center right on the ground","scenario_hash":"6e787a81958b24ce729bfc96e6cd27280fa86f6c8f7143547e5a13696421ac14","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:52.944197Z"},{"index":3,"name":"New game layout 04 center battery is faster","scenario_hash":"2e73449649e6e3bdf8f29cce6f5892b75d8b9a1079544f09f69b950460ca5c8c","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:52.944197Z"},{"index":4,"name":"New game layout 05 city span scales with playfield width","scenario_hash":"c1a4e4d45c658bafe344021a6ef47986c8cc6908bf5f56bfab2f6a8cc0d4f7dd","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:52.944197Z"}]}
# acceptance-mutation-manifest-end

# New game layout 01 starting cities and batteries
# New game layout 02 cities ordered on the ground
# New game layout 03 batteries left center right on the ground
# New game layout 04 center battery is faster
# New game layout 05 city span scales with playfield width
Feature: New game layout

Background:
  Given a new game with width <width> and height <height>
  And the player starts the game

Scenario: New game layout 01 starting cities and batteries
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <city_count> living cities
  And there are <battery_count> non-destroyed batteries named left center and right
  And each battery has <ammo> missiles

Examples:
  | width | height | expected_width | expected_height | city_count | battery_count | ammo |
  | 800   | 600    | 800            | 600             | 6          | 3             | 10   |
  | 1920  | 1080   | 1920           | 1080            | 6          | 3             | 10   |
  | 1024  | 1024   | 1024           | 1024            | 6          | 3             | 10   |

Scenario: New game layout 02 cities ordered on the ground
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And city x positions increase with city index
  And every city x is between 0 inclusive and <expected_width> exclusive
  And every city y is in the ground band for height <expected_height>
  And the leftmost city x is less than one third of width <expected_width>
  And the rightmost city x is greater than two thirds of width <expected_width>

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

Scenario: New game layout 03 batteries left center right on the ground
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the left battery x is less than the center battery x
  And the center battery x is less than the right battery x
  And the left battery x is less than one third of width <expected_width>
  And the center battery x is between one third and two thirds of width <expected_width>
  And the right battery x is greater than two thirds of width <expected_width>
  And every battery y is in the ground band for height <expected_height>

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 1024   | 1024           | 1024            |

Scenario: New game layout 04 center battery is faster
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the center battery missile speed is greater than the left battery missile speed
  And the center battery missile speed is greater than the right battery missile speed

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: New game layout 05 city span scales with playfield width
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the horizontal span of the cities is greater than half of width <expected_width>
  And the horizontal span of the cities is less than width <expected_width>

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1600  | 600    | 1600           | 600             |
  | 1920  | 1080   | 1920           | 1080            |

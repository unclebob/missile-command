# mutation-stamp: sha256=461bd76d3b9275ae5e3ecbff252180b974070dcb8b5bb67983a6d47d5270ffb0
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-25T02:22:56.328402Z","feature_name":"Browser host","feature_path":"features/browser-host.feature","background_hash":"8e63f035c8dab0c09e62ed95cd3dfb2f8ecc23b566cec139f18d3206495fbae2","implementation_hash":"sha256:d3b8f225c0cc0302f2c15b62c5231eff2fdf4e60ea65510bef9c557da21477de","scenarios":[{"index":0,"name":"Browser host 01 the project documents a browser build and open command","scenario_hash":"d42663b762da700f2e945c0e808245c94ceba8abf9a241f55e72d8b92cedb681","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:25:08.332291Z"},{"index":1,"name":"Browser host 02 starting play uses the canvas playfield size","scenario_hash":"5a60f444d7bb7074f6cb83de3038b45a6ba63cce4842f64b3ffb798e785bb2d7","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:25:08.332291Z"},{"index":2,"name":"Browser host 03 resize reflows layout without a fixed magnified buffer","scenario_hash":"5dd2a59e10c3b0c9fc17fe24b86c9f066e14b281478f55698cec5c26ce7fad06","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:25:08.332291Z"},{"index":3,"name":"Browser host 04 mouse aim and key fire match desktop behavior","scenario_hash":"cf9ee7d21f32572f788b2e7787865bafea2576c4c95ef8fffc992f091a95489f","mutation_count":20,"result":{"Total":20,"Killed":20,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:25:08.332291Z"},{"index":4,"name":"Browser host 05 click zone fire matches desktop behavior","scenario_hash":"8a1e474f44e95997bfbdc5a4d0dbf715f3fb7e938f6d45a74380bb0f70aafb7c","mutation_count":36,"result":{"Total":36,"Killed":36,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:25:08.332291Z"},{"index":5,"name":"Browser host 06 pause and resume work on the browser host","scenario_hash":"ab56cddc3947d743584edc05d92fddb8166313019483895eda427a3c6c6b6517","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:25:08.332291Z"},{"index":6,"name":"Browser host 07 high scores and options persist in localStorage across reload","scenario_hash":"5c359908dba6bbec5014766e50519be85dc647433eb99a8e2b99e1b93d9f9381","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:25:08.332291Z"},{"index":7,"name":"Browser host 08 pure core stays free of host dependencies","scenario_hash":"61c5baa3da43907c7161c9f53278f66ebc4e7e95a2c518746a470b8cb3b81ca1","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:25:08.332291Z"}]}
# acceptance-mutation-manifest-end

# Browser host 01 the project documents a browser build and open command
# Browser host 02 starting play uses the canvas playfield size
# Browser host 03 resize reflows layout without a fixed magnified buffer
# Browser host 04 mouse aim and key fire match desktop behavior
# Browser host 05 click zone fire matches desktop behavior
# Browser host 06 pause and resume work on the browser host
# Browser host 07 high scores and options persist in localStorage across reload
# Browser host 08 pure core stays free of host dependencies
Feature: Browser host

Background:
  Given a new game with width <width> and height <height>

Scenario: Browser host 01 the project documents a browser build and open command
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the documented browser build command is <build_command>
  And the documented browser open path is <open_path>

Examples:
  | width | height | build_command | open_path | expected_width | expected_height |
  | 800   | 600    | bb browser    | index.html | 800           | 600             |
  | 1920  | 1080   | bb browser    | index.html | 1920          | 1080            |

Scenario: Browser host 02 starting play uses the canvas playfield size
  When the player starts the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And there are 6 living cities
  And there are 3 non-destroyed batteries named left center and right

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1280  | 720    | 1280           | 720             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Browser host 03 resize reflows layout without a fixed magnified buffer
  When the player starts the game
  Then the playfield width is <expected_start_width>
  And the playfield height is <expected_start_height>
  When the playfield is resized to width <new_width> and height <new_height>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And city x positions increase with city index
  And every city y is in the ground band for height <expected_height>
  And the horizontal span of the cities is greater than half of width <expected_width>

Examples:
  | width | height | expected_start_width | expected_start_height | new_width | new_height | expected_width | expected_height |
  | 800   | 600    | 800                  | 600                   | 1280      | 720        | 1280           | 720             |
  | 800   | 600    | 800                  | 600                   | 1920      | 1080       | 1920           | 1080            |
  | 1024  | 768    | 1024                 | 768                   | 800       | 600        | 800            | 600             |

Scenario: Browser host 04 mouse aim and key fire match desktop behavior
  When the player starts the game
  And the player aims at 400 200
  And the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the crosshair is at 400 200
  And there are 1 defensive missiles in flight
  And a defensive missile from the <battery> battery targets 400 200

Examples:
  | width | height | battery | expected_width | expected_height |
  | 800   | 600    | left    | 800            | 600             |
  | 800   | 600    | center  | 800            | 600             |
  | 800   | 600    | right   | 800            | 600             |
  | 1920  | 1080   | center  | 1920           | 1080            |

Scenario: Browser host 05 click zone fire matches desktop behavior
  When the player starts the game
  And the player aims at <aim_x> <aim_y>
  And the player clicks at <aim_x> <aim_y>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are 1 defensive missiles in flight
  And a defensive missile from the <battery> battery targets <expected_x> <expected_y>

Examples:
  | width | height | aim_x | aim_y | expected_x | expected_y | battery | expected_width | expected_height |
  | 800   | 600    | 100   | 200   | 100        | 200        | left    | 800            | 600             |
  | 800   | 600    | 400   | 200   | 400        | 200        | center  | 800            | 600             |
  | 800   | 600    | 700   | 200   | 700        | 200        | right   | 800            | 600             |
  | 1920  | 1080   | 100   | 400   | 100        | 400        | left    | 1920           | 1080            |

Scenario: Browser host 06 pause and resume work on the browser host
  When the player starts the game
  And the player pauses the game
  Then the screen is paused
  When the player resumes the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Browser host 07 high scores and options persist in localStorage across reload
  When the player opens options from the title
  And the player sets mute to true
  And the player sets difficulty to easy
  And the player leaves options
  And the browser host options and high scores are persisted to localStorage
  And the browser host page is reloaded with width <width> and height <height>
  When the player opens options from the title
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And mute is true
  And the difficulty is easy

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Browser host 08 pure core stays free of host dependencies
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the architecture check passes for pure core isolation

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

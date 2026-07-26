# mutation-stamp: sha256=4fa08bf6dd4677fa879ab7c012ac04672dbd4dc37c0ccb685ad6895dbfe519e2
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-26T15:19:30.617474Z","feature_name":"Desktop host","feature_path":"features/desktop-host.feature","background_hash":"8e63f035c8dab0c09e62ed95cd3dfb2f8ecc23b566cec139f18d3206495fbae2","implementation_hash":"sha256:d3146f67ead969cdaa44f20eec4f0da7624bce3ebd9a458fcdf28a576f07e4c0","scenarios":[{"index":0,"name":"Desktop host 01 the project documents a desktop launch command","scenario_hash":"8e5010cb468b828c38e8e5731a6395b0f990024f1fec3564cfe36aa3c0835fc9","mutation_count":10,"result":{"Total":10,"Killed":10,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:24:58.099408Z"},{"index":1,"name":"Desktop host 02 starting play uses the requested playfield size","scenario_hash":"1415a6999cb2a31ac2e52753a1db9e74ec5c8adeb9d8a7a383259d8b6e5dd735","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:24:58.099408Z"},{"index":2,"name":"Desktop host 03 resize reflows layout without a fixed magnified buffer","scenario_hash":"14a18fbb385c534203cb90b0647e1b02eab01fe9a5f08fb8e1450edf8783cbb9","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:24:58.099408Z"},{"index":3,"name":"Desktop host 04 mouse aim and key fire are available while playing","scenario_hash":"b0feaf027ef33cc611a9082a82b3f9a96d21fb2c2f1724af228cc042594afd97","mutation_count":20,"result":{"Total":20,"Killed":20,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:24:58.099408Z"},{"index":4,"name":"Desktop host 05 click zone fire is available while playing","scenario_hash":"104d80dfa8bbe2dc98437daf5ab41bdc5b16c248737c48eb42523a78047cc400","mutation_count":36,"result":{"Total":36,"Killed":36,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:24:58.099408Z"},{"index":5,"name":"Desktop host 06 pause and resume work on the desktop host","scenario_hash":"b57e65f3a878ee0cb7fe26728dc738cc24c15f59441c821160ca7d3768fdcf5f","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:24:58.099408Z"},{"index":6,"name":"Desktop host 07 high scores and options persist across desktop restarts","scenario_hash":"fa4acd394a205c2e5ea9a10c52aecbe47f900582f864b6110dac65e10f246c00","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:24:58.099408Z"},{"index":7,"name":"Desktop host 08 pure core stays free of host dependencies","scenario_hash":"95f56e0324ff6334bdadbe82e35a45dc1616098309e709abcf37378ef7ba91ed","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:24:58.099408Z"}]}
# acceptance-mutation-manifest-end

# Desktop host 01 the project documents a desktop launch command
# Desktop host 02 starting play uses the requested playfield size
# Desktop host 03 resize reflows layout without a fixed magnified buffer
# Desktop host 04 mouse aim and key fire are available while playing
# Desktop host 05 click zone fire is available while playing
# Desktop host 06 pause and resume work on the desktop host
# Desktop host 07 high scores and options persist across desktop restarts
# Desktop host 08 pure core stays free of host dependencies
Feature: Desktop host

Background:
  Given a new game with width <width> and height <height>

Scenario: Desktop host 01 the project documents a desktop launch command
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the documented desktop launch command is <command>

Examples:
  | width | height | command | expected_width | expected_height |
  | 800   | 600    | bb play | 800            | 600             |
  | 1920  | 1080   | bb play | 1920           | 1080            |

Scenario: Desktop host 02 starting play uses the requested playfield size
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

Scenario: Desktop host 03 resize reflows layout without a fixed magnified buffer
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

Scenario: Desktop host 04 mouse aim and key fire are available while playing
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

Scenario: Desktop host 05 click zone fire is available while playing
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

Scenario: Desktop host 06 pause and resume work on the desktop host
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

Scenario: Desktop host 07 high scores and options persist across desktop restarts
  When the player opens options from the title
  And the player sets mute to true
  And the player sets difficulty to easy
  And the player leaves options
  And the desktop host options and high scores are persisted
  And the desktop host is restarted with width <width> and height <height>
  When the player opens options from the title
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And mute is true
  And the difficulty is easy

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Desktop host 08 pure core stays free of host dependencies
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the architecture check passes for pure core isolation

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

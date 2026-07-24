# mutation-stamp: sha256=53a58bb610cca855545af71159429f7529d5ab018d081803428f53831ac68b10
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-24T21:00:12.319269Z","feature_name":"Pause","feature_path":"features/pause.feature","background_hash":"a9ec6e117022da9c15cf9c45cedc294cdddfdf941f078d5b2d9a8432079752db","implementation_hash":"sha256:f982cf0c5df302c5ed416956b108d37821a8f6ece429c90ad551fa02835f6107","scenarios":[{"index":0,"name":"Pause 01 pause from playing enters the paused screen","scenario_hash":"1b77e9d5d1a19c44d42c6b82d904db1ded51c085d4d8350acd0e60039a7d6a0e","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:12.319269Z"},{"index":1,"name":"Pause 02 while paused simulation does not advance","scenario_hash":"2510a789d5a768a5314c412ce33f3884fe1246eb6c36e035198f29df4d5e6973","mutation_count":15,"result":{"Total":15,"Killed":15,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:12.319269Z"},{"index":2,"name":"Pause 03 while paused fire does not launch","scenario_hash":"4853e707493b73dfaa8493af248f49a563eab3736d1f5ff8f36cafa5a1ef61af","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:12.319269Z"},{"index":3,"name":"Pause 04 resume returns to playing","scenario_hash":"2f4d42d269a5262ad7c60f1894eb35a2a4e6dc6e89a4bab2f15b34532d7a1ccd","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:12.319269Z"},{"index":4,"name":"Pause 05 resume continues entities from prior state","scenario_hash":"dc6a3fec29e0e018e3f090d0317ae867d35911f7ad768739778ace6e2ddade44","mutation_count":15,"result":{"Total":15,"Killed":15,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:12.319269Z"},{"index":5,"name":"Pause 06 pause is ignored on the title screen","scenario_hash":"ff5acecf9f68a0661a782ad80ac394177f0b315b4c1d3ab25557fd7c3d8c39b4","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:12.319269Z"}]}
# acceptance-mutation-manifest-end

# Pause 01 pause from playing enters the paused screen
# Pause 02 while paused simulation does not advance
# Pause 03 while paused fire does not launch
# Pause 04 resume returns to playing
# Pause 05 resume continues entities from prior state
# Pause 06 pause is ignored on the title screen
Feature: Pause

Background:
  Given a new game with width <width> and height <height>
  When the player starts the game

Scenario: Pause 01 pause from playing enters the paused screen
  Then the screen is playing
  When the player pauses the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is paused

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

Scenario: Pause 02 while paused simulation does not advance
  And an enemy missile targeting city <city_index>
  When time advances by 0.1 seconds
  And the first enemy missile progress is recorded
  When the player pauses the game
  And time advances by 0.5 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is paused
  And the first enemy missile progress equals the recorded progress

Examples:
  | width | height | city_index | expected_width | expected_height |
  | 800   | 600    | 0          | 800            | 600             |
  | 800   | 600    | 3          | 800            | 600             |
  | 1920  | 1080   | 0          | 1920           | 1080            |

Scenario: Pause 03 while paused fire does not launch
  When the player pauses the game
  And the player aims at 400 200
  And the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is paused
  And there are 0 defensive missiles in flight
  And each non-destroyed battery has <ammo> missiles

Examples:
  | width | height | battery | ammo | expected_width | expected_height |
  | 800   | 600    | left    | 10   | 800            | 600             |
  | 800   | 600    | center  | 10   | 800            | 600             |
  | 800   | 600    | right   | 10   | 800            | 600             |
  | 1920  | 1080   | center  | 10   | 1920           | 1080            |

Scenario: Pause 04 resume returns to playing
  When the player pauses the game
  Then the screen is paused
  When the player resumes the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |
  | 1024  | 768    | 1024           | 768             |

Scenario: Pause 05 resume continues entities from prior state
  And an enemy missile targeting city <city_index>
  When time advances by 0.1 seconds
  And the first enemy missile progress is recorded
  When the player pauses the game
  And time advances by 0.5 seconds
  And the player resumes the game
  And time advances by 0.1 seconds
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And the first enemy missile progress is greater than the recorded progress

Examples:
  | width | height | city_index | expected_width | expected_height |
  | 800   | 600    | 0          | 800            | 600             |
  | 800   | 600    | 2          | 800            | 600             |
  | 1920  | 1080   | 0          | 1920           | 1080            |

Scenario: Pause 06 pause is ignored on the title screen
  Given a new game with width <width> and height <height>
  Then the screen is title
  When the player pauses the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

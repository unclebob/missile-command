# mutation-stamp: sha256=9f8bc30ee793939cecbc51a112ba43e5c1c95976c58aaf52068da25fad507013
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-26T15:19:37.151748Z","feature_name":"HUD","feature_path":"features/hud.feature","background_hash":"8e63f035c8dab0c09e62ed95cd3dfb2f8ecc23b566cec139f18d3206495fbae2","implementation_hash":"sha256:23c0c9102a3497ac6bf9375338d62e89774180d1b26cb26f66a70031f90d5178","scenarios":[{"index":0,"name":"HUD 01 playing HUD shows score wave multiplier ammo cities and reserve","scenario_hash":"b44a65e22c81aba79f27797fe265c5961ca2270c71a4eb0cabe96b6ae6c31dfa","mutation_count":36,"result":{"Total":36,"Killed":36,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:14.372782Z"},{"index":1,"name":"HUD 02 HUD score matches core after a scoring event","scenario_hash":"d62ebc900a7a5de2edff92870ba32689d5279beb9ef11a88105e94b531b6658f","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:14.372782Z"},{"index":2,"name":"HUD 03 HUD ammo matches batteries after fire","scenario_hash":"c7ff42129bfc61fb5fab2eddec59a98100ad88f420c69e7ac94d70867b673bd5","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:14.372782Z"},{"index":3,"name":"HUD 04 HUD wave and multiplier match core","scenario_hash":"5f1c3ec05b54e43a6bf2ee6e91b00a13e33c97260a1b5517a955771700adc3e1","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:14.372782Z"},{"index":4,"name":"HUD 05 HUD living cities and bonus reserve match core","scenario_hash":"651c1e9507b9cf17d0f2ec22e6472e628aae1fd977468c5495912170e6fd8686","mutation_count":28,"result":{"Total":28,"Killed":28,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:14.372782Z"},{"index":5,"name":"HUD 06 paused screen still exposes the HUD","scenario_hash":"f14cd3a058aff5a01535a23de4754ad8620dbdad12fa08ed35d563a58c23d1b3","mutation_count":30,"result":{"Total":30,"Killed":30,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:14.372782Z"},{"index":6,"name":"HUD 07 title screen is not required to show the full HUD","scenario_hash":"90ef4b13fddd4c7e094b9a359e035c1c90af2d3852de7d9c7248ddb51a148a69","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:00:14.372782Z"}]}
# acceptance-mutation-manifest-end

# HUD 01 playing HUD shows score wave multiplier ammo cities and reserve
# HUD 02 HUD score matches core after a scoring event
# HUD 03 HUD ammo matches batteries after fire
# HUD 04 HUD wave and multiplier match core
# HUD 05 HUD living cities and bonus reserve match core
# HUD 06 paused screen still exposes the HUD
# HUD 07 title screen is not required to show the full HUD
Feature: HUD

Background:
  Given a new game with width <width> and height <height>

Scenario: HUD 01 playing HUD shows score wave multiplier ammo cities and reserve
  When the player starts the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is playing
  And the hud shows score <score>
  And the hud shows wave <wave>
  And the hud shows multiplier <multiplier>
  And the hud shows left ammo <left_ammo>
  And the hud shows center ammo <center_ammo>
  And the hud shows right ammo <right_ammo>
  And the hud shows living cities <living_cities>
  And the hud shows bonus cities <bonus_cities>

Examples:
  | width | height | score | wave | multiplier | left_ammo | center_ammo | right_ammo | living_cities | bonus_cities | expected_width | expected_height |
  | 800   | 600    | 0     | 1    | 1          | 10        | 10          | 10         | 6             | 0            | 800            | 600             |
  | 1920  | 1080   | 0     | 1    | 1          | 10        | 10          | 10         | 6             | 0            | 1920           | 1080            |
  | 1024  | 768    | 0     | 1    | 1          | 10        | 10          | 10         | 6             | 0            | 1024           | 768             |

Scenario: HUD 02 HUD score matches core after a scoring event
  When the player starts the game
  And the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the score is <expected_score>
  And the hud shows score <expected_score>

Examples:
  | width | height | score | expected_score | expected_width | expected_height |
  | 800   | 600    | 25    | 25             | 800            | 600             |
  | 800   | 600    | 2500  | 2500           | 800            | 600             |
  | 800   | 600    | 12500 | 12500          | 800            | 600             |
  | 1920  | 1080   | 100   | 100            | 1920           | 1080            |

Scenario: HUD 03 HUD ammo matches batteries after fire
  When the player starts the game
  And the player aims at 400 200
  And the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And each non-destroyed battery has matching hud ammo
  And the hud shows <battery> ammo <ammo>

Examples:
  | width | height | battery | ammo | expected_width | expected_height |
  | 800   | 600    | left    | 9    | 800            | 600             |
  | 800   | 600    | center  | 9    | 800            | 600             |
  | 800   | 600    | right   | 9    | 800            | 600             |
  | 1920  | 1080   | center  | 9    | 1920           | 1080            |

Scenario: HUD 04 HUD wave and multiplier match core
  When the player starts the game
  And the game is at wave <wave>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave number is <wave>
  And the multiplier is <multiplier>
  And the hud shows wave <wave>
  And the hud shows multiplier <multiplier>

Examples:
  | width | height | wave | multiplier | expected_width | expected_height |
  | 800   | 600    | 1    | 1          | 800            | 600             |
  | 800   | 600    | 3    | 2          | 800            | 600             |
  | 800   | 600    | 5    | 3          | 800            | 600             |
  | 1920  | 1080   | 11   | 6          | 1920           | 1080            |

Scenario: HUD 05 HUD living cities and bonus reserve match core
  When the player starts the game
  And city 0 has been destroyed
  And the bonus city reserve is set to <bonus_cities>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <living_cities> living cities
  And the bonus city reserve is <expected_bonus_cities>
  And the hud shows living cities <living_cities>
  And the hud shows bonus cities <expected_bonus_cities>

Examples:
  | width | height | living_cities | bonus_cities | expected_bonus_cities | expected_width | expected_height |
  | 800   | 600    | 5             | 0            | 0                     | 800            | 600             |
  | 800   | 600    | 5             | 2            | 2                     | 800            | 600             |
  | 800   | 600    | 5             | 1            | 1                     | 800            | 600             |
  | 1920  | 1080   | 5             | 3            | 3                     | 1920           | 1080            |

Scenario: HUD 06 paused screen still exposes the HUD
  When the player starts the game
  And the score becomes <score>
  And the player pauses the game
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is paused
  And the hud shows score <expected_score>
  And the hud shows wave <wave>
  And the hud shows multiplier <multiplier>
  And the hud shows living cities <living_cities>
  And the hud shows bonus cities <bonus_cities>

Examples:
  | width | height | score | expected_score | wave | multiplier | living_cities | bonus_cities | expected_width | expected_height |
  | 800   | 600    | 500   | 500            | 1    | 1          | 6             | 0            | 800            | 600             |
  | 800   | 600    | 0     | 0              | 1    | 1          | 6             | 0            | 800            | 600             |
  | 1920  | 1080   | 1000  | 1000           | 1    | 1          | 6             | 0            | 1920           | 1080            |

Scenario: HUD 07 title screen is not required to show the full HUD
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the screen is title
  And the full playing hud is not required

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

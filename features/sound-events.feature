# mutation-stamp: sha256=2c5d18997e9a43447dc1ebe5ec50890aeaf6ef4ebf0029ed38267afc406e1935
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-25T16:06:20.686767Z","feature_name":"Sound events","feature_path":"features/sound-events.feature","background_hash":"a9ec6e117022da9c15cf9c45cedc294cdddfdf941f078d5b2d9a8432079752db","implementation_hash":"sha256:d8e435c28f68d41542715f334d6a2259638511cb18f0ee5be30a1659efd16f56","scenarios":[{"index":1,"name":"Sound events 02 destroying an enemy with a fireball emits an intercepted event","scenario_hash":"3666a5b2a5846cc39aaef57637fb84c9d3e4eb0309ebd054235c871a0ecef1cb","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:36.686252Z"},{"index":5,"name":"Sound events 06 completing a wave emits a wave banner event","scenario_hash":"b765c354acbeab9c339f3454497c359290c1983aa38d465ea8911cbce4dc57fe","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-25T15:06:36.686252Z"},{"index":0,"name":"Sound events 01 firing emits a launch event","scenario_hash":"386555750804228f472fdac62b96417d816a49468d57f8c43996f697c71c6545","mutation_count":20,"result":{"Total":20,"Killed":20,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:56.942335Z"},{"index":2,"name":"Sound events 03 destroying a city emits a city destroyed event","scenario_hash":"b2317ce96adfda5d46012c802ccc791b40f9b2cc52b7bd2575adb8428c8ac9ee","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:56.942335Z"},{"index":3,"name":"Sound events 04 destroying a battery emits a battery destroyed event","scenario_hash":"5d22757623682553af0108a0c5c4764335cc39dc7c2005cebf2bd167dddee8c8","mutation_count":24,"result":{"Total":24,"Killed":24,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:56.942335Z"},{"index":4,"name":"Sound events 05 firing to the low ammo threshold emits a low ammo event","scenario_hash":"dcf58dd08c345a71014e886877b9502fa5b799b03eb5974fe3c7abfca95bdcdc","mutation_count":20,"result":{"Total":20,"Killed":20,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:56.942335Z"},{"index":6,"name":"Sound events 07 earning a bonus city emits a bonus city event","scenario_hash":"152b7935ee64e21981c0ef5da8d1fbc554af328ff852c3d878ac5d4116aa8a0a","mutation_count":21,"result":{"Total":21,"Killed":21,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:56.942335Z"},{"index":7,"name":"Sound events 08 entering THE END emits a the end event","scenario_hash":"48965efb64f56b7a7ad44f2a69938336b9c7fed6830ce3fea47bf11be0b85e33","mutation_count":15,"result":{"Total":15,"Killed":15,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:56.942335Z"},{"index":8,"name":"Sound events 09 mute does not remove core sfx events","scenario_hash":"df1ebe0a4e08fe4cbac728f6ce09cc39fba5e057661454dedf529a35300c8e3c","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-07-24T21:23:56.942335Z"}]}
# acceptance-mutation-manifest-end

# Sound events 01 firing emits a launch event
# Sound events 02 destroying an enemy with a fireball emits an intercepted event
# Sound events 03 destroying a city emits a city destroyed event
# Sound events 04 destroying a battery emits a battery destroyed event
# Sound events 05 firing to the low ammo threshold emits a low ammo event
# Sound events 06 completing a wave emits a wave banner event
# Sound events 07 earning a bonus city emits a bonus city event
# Sound events 08 entering THE END emits a the end event
# Sound events 09 mute does not remove core sfx events
Feature: Sound events

Background:
  Given a new game with width <width> and height <height>
  When the player starts the game

Scenario: Sound events 01 firing emits a launch event
  And the player aims at 400 200
  When the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And an sfx event sfx/launch was emitted

Examples:
  | width | height | battery | expected_width | expected_height |
  | 800   | 600    | left    | 800            | 600             |
  | 800   | 600    | center  | 800            | 600             |
  | 800   | 600    | right   | 800            | 600             |
  | 1920  | 1080   | center  | 1920           | 1080            |

Scenario: Sound events 02 destroying an enemy with a fireball emits an intercepted event
  And an enemy missile targeting city 1
  And a fireball at 400 250 with radius 40
  And the enemy missile path passes within distance 40 of that fireball center
  When time advances until the enemy missile is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the enemy missile is destroyed by the fireball
  And an sfx event sfx/intercepted was emitted

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Sound events 03 destroying a city emits a city destroyed event
  And an enemy missile targeting city 0
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And city 0 is not living
  And an sfx event sfx/city-destroyed was emitted

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Sound events 04 destroying a battery emits a battery destroyed event
  And an enemy missile targeting battery <battery>
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <battery> battery is destroyed
  And an sfx event <event> was emitted

Examples:
  | width | height | battery | event                  | expected_width | expected_height |
  | 800   | 600    | left    | sfx/battery-destroyed  | 800            | 600             |
  | 800   | 600    | center  | sfx/battery-destroyed  | 800            | 600             |
  | 800   | 600    | right   | sfx/battery-destroyed  | 800            | 600             |
  | 1920  | 1080   | left    | sfx/battery-destroyed  | 1920           | 1080            |

Scenario: Sound events 05 firing to the low ammo threshold emits a low ammo event
  And every non-destroyed battery has 2 missiles
  And the player aims at 400 200
  When the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <battery> battery has 1 missiles
  And an sfx event sfx/low-ammo was emitted

Examples:
  | width | height | battery | expected_width | expected_height |
  | 800   | 600    | left    | 800            | 600             |
  | 800   | 600    | center  | 800            | 600             |
  | 800   | 600    | right   | 800            | 600             |
  | 1920  | 1080   | left    | 1920           | 1080            |

Scenario: Sound events 06 completing a wave emits a wave banner event
  And every non-destroyed battery has 10 missiles
  And the current wave has 1 scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave is complete
  And an sfx event sfx/wave was emitted

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

Scenario: Sound events 07 earning a bonus city emits a bonus city event
  When the score becomes <score>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And an sfx event <event> was emitted
  And the number of bonus city earned events is <award_count>

Examples:
  | width | height | score | event           | award_count | expected_width | expected_height |
  | 800   | 600    | 10000 | sfx/bonus-city  | 1           | 800            | 600             |
  | 800   | 600    | 20000 | sfx/bonus-city  | 2           | 800            | 600             |
  | 1920  | 1080   | 10000 | sfx/bonus-city  | 1           | 1920           | 1080            |

Scenario: Sound events 08 entering THE END emits a the end event
  And all cities have been destroyed
  And the bonus city reserve is set to 0
  When game over conditions are evaluated
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the game is at THE END
  And an sfx event <event> was emitted

Examples:
  | width | height | event       | expected_width | expected_height |
  | 800   | 600    | sfx/the-end | 800            | 600             |
  | 1920  | 1080   | sfx/the-end | 1920           | 1080            |
  | 1024  | 768    | sfx/the-end | 1024           | 768             |

Scenario: Sound events 09 mute does not remove core sfx events
  And mute is true
  And the player aims at 400 200
  When the player fires the left battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And mute is true
  And an sfx event sfx/launch was emitted

Examples:
  | width | height | expected_width | expected_height |
  | 800   | 600    | 800            | 600             |
  | 1920  | 1080   | 1920           | 1080            |

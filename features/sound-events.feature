# Sound events 01 firing emits a launch event
# Sound events 02 destroying an enemy with a fireball emits an explosion event
# Sound events 03 destroying a city emits a city destroyed event
# Sound events 04 destroying a battery emits a battery destroyed event
# Sound events 05 firing to the low ammo threshold emits a low ammo event
# Sound events 06 completing a wave emits a wave clear event
# Sound events 07 earning a bonus city emits a bonus city event
# Sound events 08 entering THE END emits a the end event
# Sound events 09 mute does not remove core sfx events
Feature: Sound events

Background:
  Given a new game with width <width> and height <height>
  When the player starts the game

Scenario: Sound events 01 firing emits a launch event
  And the player aims at <aim_x> <aim_y>
  When the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And an sfx event <event> was emitted

Examples:
  | width | height | aim_x | aim_y | battery | event      | expected_width | expected_height |
  | 800   | 600    | 400   | 200   | left    | sfx/launch | 800            | 600             |
  | 800   | 600    | 400   | 200   | center  | sfx/launch | 800            | 600             |
  | 800   | 600    | 400   | 200   | right   | sfx/launch | 800            | 600             |
  | 1920  | 1080   | 960   | 400   | center  | sfx/launch | 1920           | 1080            |

Scenario: Sound events 02 destroying an enemy with a fireball emits an explosion event
  And an enemy missile targeting city <city_index>
  And a fireball at <fb_x> <fb_y> with radius <radius>
  And the enemy missile path passes within distance <radius> of that fireball center
  When time advances until the enemy missile is inside the fireball radius or has impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the enemy missile is destroyed by the fireball
  And an sfx event <event> was emitted

Examples:
  | width | height | city_index | fb_x | fb_y | radius | event          | expected_width | expected_height |
  | 800   | 600    | 1          | 400  | 250  | 40     | sfx/explosion  | 800            | 600             |
  | 800   | 600    | 2          | 400  | 200  | 50     | sfx/explosion  | 800            | 600             |
  | 1920  | 1080   | 3          | 960  | 400  | 50     | sfx/explosion  | 1920           | 1080            |

Scenario: Sound events 03 destroying a city emits a city destroyed event
  And an enemy missile targeting city <city_index>
  When time advances until enemy missiles impact or are destroyed
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And city <city_index> is not living
  And an sfx event <event> was emitted

Examples:
  | width | height | city_index | event               | expected_width | expected_height |
  | 800   | 600    | 0          | sfx/city-destroyed  | 800            | 600             |
  | 800   | 600    | 2          | sfx/city-destroyed  | 800            | 600             |
  | 1920  | 1080   | 5          | sfx/city-destroyed  | 1920           | 1080            |

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
  And every non-destroyed battery has <ammo_before> missiles
  And the player aims at <aim_x> <aim_y>
  When the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the <battery> battery has <ammo_after> missiles
  And an sfx event <event> was emitted

Examples:
  | width | height | battery | ammo_before | ammo_after | aim_x | aim_y | event         | expected_width | expected_height |
  | 800   | 600    | left    | 2           | 1          | 400   | 200   | sfx/low-ammo  | 800            | 600             |
  | 800   | 600    | center  | 2           | 1          | 400   | 200   | sfx/low-ammo  | 800            | 600             |
  | 800   | 600    | right   | 2           | 1          | 400   | 200   | sfx/low-ammo  | 800            | 600             |
  | 1920  | 1080   | left    | 2           | 1          | 960   | 400   | sfx/low-ammo  | 1920           | 1080            |

Scenario: Sound events 06 completing a wave emits a wave clear event
  And every non-destroyed battery has <ammo> missiles
  And the current wave has 1 scheduled enemies still active
  When time advances until all wave enemies are destroyed or have impacted
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And the wave is complete
  And an sfx event <event> was emitted

Examples:
  | width | height | ammo | event          | expected_width | expected_height |
  | 800   | 600    | 10   | sfx/wave-clear | 800            | 600             |
  | 800   | 600    | 5    | sfx/wave-clear | 800            | 600             |
  | 1920  | 1080   | 10   | sfx/wave-clear | 1920           | 1080            |

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
  And mute is <mute>
  And the player aims at <aim_x> <aim_y>
  When the player fires the <battery> battery
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And mute is <mute>
  And an sfx event <event> was emitted

Examples:
  | width | height | mute  | aim_x | aim_y | battery | event      | expected_width | expected_height |
  | 800   | 600    | true  | 400   | 200   | left    | sfx/launch | 800            | 600             |
  | 800   | 600    | false | 400   | 200   | left    | sfx/launch | 800            | 600             |
  | 1920  | 1080   | true  | 960   | 400   | center  | sfx/launch | 1920           | 1080            |

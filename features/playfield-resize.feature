# mutation-stamp: sha256=07eeb37d5bebd3c3dcf9b284db50e723e499ed50fc6af472fcd1c9fd4a56a5aa
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-07-26T15:26:27.765555Z","feature_name":"Playfield resize","feature_path":"features/playfield-resize.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"sha256:e334ef8c3f64bf79cf7cd86c0b92525a36dacd964410e540d553c3e700107aa3","scenarios":[{"index":0,"name":"Playfield resize 01 reflows a fresh game","scenario_hash":"2ccaeebd7ff3651f78d037a2953711582de711cb4051c5875c290931ce8b7814","mutation_count":18,"result":{"Total":18,"Killed":18,"Survived":0,"Errors":0},"tested_at":"2026-07-26T15:05:56.565267Z"}]}
# acceptance-mutation-manifest-end

# Playfield resize 01 reflows a fresh game
Feature: Playfield resize

Scenario: Playfield resize 01 reflows a fresh game
  Given a new game with width 800 and height 600
  And the player starts the game
  When the playfield is resized to width <new_width> and height <new_height>
  Then the playfield width is <expected_width>
  And the playfield height is <expected_height>
  And there are <city_count> living cities
  And every city x is between 0 inclusive and <expected_width> exclusive
  And every city y is in the ground band for height <expected_height>
  And the left battery x is less than one third of width <expected_width>
  And the right battery x is greater than two thirds of width <expected_width>
  And each battery has <ammo> missiles

Examples:
  | new_width | new_height | expected_width | expected_height | city_count | ammo |
  | 1920      | 1080       | 1920           | 1080            | 6          | 10   |
  | 800       | 600        | 800            | 600             | 6          | 10   |
  | 1280      | 720        | 1280           | 720             | 6          | 10   |

# Playfield resize 01 reflows a fresh game
Feature: Playfield resize

Scenario: Playfield resize 01 reflows a fresh game
  Given a new game with width <width> and height <height>
  When the playfield is resized to width <new_width> and height <new_height>
  Then the playfield width is <new_width>
  And the playfield height is <new_height>
  And there are <city_count> living cities
  And every city x is between 0 inclusive and <new_width> exclusive
  And every city y is in the ground band for height <new_height>
  And the left battery x is less than one third of width <new_width>
  And the right battery x is greater than two thirds of width <new_width>
  And each battery has <ammo> missiles

Examples:
  | width | height | new_width | new_height | city_count | ammo |
  | 800   | 600    | 1920      | 1080       | 6          | 10   |
  | 1920  | 1080   | 800       | 600        | 6          | 10   |
  | 1024  | 1024   | 1280      | 720        | 6          | 10   |

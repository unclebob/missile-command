# Project foundation 01 new game records playfield size
Feature: Project foundation

Scenario: Project foundation 01 new game records playfield size
  Given a new game with width <width> and height <height>
  Then the playfield width is <width>
  And the playfield height is <height>

Examples:
  | width | height |
  | 800   | 600    |
  | 1920  | 1080   |

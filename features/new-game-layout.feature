# New game layout 01 starting cities and batteries
# New game layout 02 cities ordered on the ground
# New game layout 03 batteries left center right on the ground
# New game layout 04 center battery is faster
# New game layout 05 city span scales with playfield width
Feature: New game layout

Background:
  Given a new game with width <width> and height <height>

Scenario: New game layout 01 starting cities and batteries
  Then there are <city_count> living cities
  And there are <battery_count> non-destroyed batteries named left center and right
  And each battery has <ammo> missiles

Examples:
  | width | height | city_count | battery_count | ammo |
  | 800   | 600    | 6          | 3             | 10   |
  | 1920  | 1080   | 6          | 3             | 10   |
  | 1024  | 1024   | 6          | 3             | 10   |

Scenario: New game layout 02 cities ordered on the ground
  Then city x positions increase with city index
  And every city x is between 0 inclusive and <width> exclusive
  And every city y is in the ground band for height <height>
  And the leftmost city x is less than one third of width <width>
  And the rightmost city x is greater than two thirds of width <width>

Examples:
  | width | height |
  | 800   | 600    |
  | 1920  | 1080   |
  | 1024  | 768    |

Scenario: New game layout 03 batteries left center right on the ground
  Then the left battery x is less than the center battery x
  And the center battery x is less than the right battery x
  And the left battery x is less than one third of width <width>
  And the center battery x is between one third and two thirds of width <width>
  And the right battery x is greater than two thirds of width <width>
  And every battery y is in the ground band for height <height>

Examples:
  | width | height |
  | 800   | 600    |
  | 1920  | 1080   |
  | 1024  | 1024   |

Scenario: New game layout 04 center battery is faster
  Then the center battery missile speed is greater than the left battery missile speed
  And the center battery missile speed is greater than the right battery missile speed

Examples:
  | width | height |
  | 800   | 600    |
  | 1920  | 1080   |

Scenario: New game layout 05 city span scales with playfield width
  Then the horizontal span of the cities is greater than half of width <width>
  And the horizontal span of the cities is less than width <width>

Examples:
  | width | height |
  | 800   | 600    |
  | 1600  | 600    |
  | 1920  | 1080   |

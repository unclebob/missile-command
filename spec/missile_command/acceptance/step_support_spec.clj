(ns missile-command.acceptance.step-support-spec
  (:require [speclj.core :refer :all]
            [missile-command.acceptance.step-support :as support]))

(describe "require-value"
  (it "reads a string-keyed example value"
    (should= "800" (support/require-value {"width" "800"} "width")))

  (it "reads a keyword-keyed example value"
    (should= "600" (support/require-value {:height "600"} "height")))

  (it "fails when the parameter is missing"
    (should-throw Exception #"missing example value for width"
      (support/require-value {} "width"))))

(describe "parse-int"
  (it "parses integer strings"
    (should= 800 (support/parse-int "800" "width")))

  (it "fails on non-integers"
    (should-throw Exception #"invalid integer for width: no"
      (support/parse-int "no" "width"))))

(describe "example-int"
  (it "reads and parses an example parameter"
    (should= 1920 (support/example-int {"width" "1920"} "width" "width"))))

(describe "example-battery"
  (it "parses left center and right battery ids"
    (should= :left (support/example-battery {"battery" "left"} "battery"))
    (should= :center (support/example-battery {"battery" "center"} "battery"))
    (should= :right (support/example-battery {"battery" "right"} "battery")))

  (it "fails on unknown battery names"
    (should-throw Exception #"unknown battery: top"
      (support/example-battery {"battery" "top"} "battery"))))

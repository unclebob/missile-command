(ns missile-command.input-spec
  (:require [speclj.core :refer :all]
            [missile-command.input :as input]))

(describe "click-zone"
  (it "splits the playfield into equal horizontal thirds"
    (should= :left (input/click-zone 900 0))
    (should= :left (input/click-zone 900 299))
    (should= :center (input/click-zone 900 300))
    (should= :center (input/click-zone 900 599))
    (should= :right (input/click-zone 900 600))
    (should= :right (input/click-zone 900 899))))

(describe "click-fallback-order"
  (it "prefers the zone battery then adjacent batteries"
    (should= [:left :center :right] (input/click-fallback-order :left))
    (should= [:center :left :right] (input/click-fallback-order :center))
    (should= [:right :center :left] (input/click-fallback-order :right))))

(describe "first-preferred"
  (it "returns the first battery that satisfies the predicate"
    (should= :center
             (input/first-preferred :left #{:center :right}))
    (should= :left
             (input/first-preferred :left #{:left :center}))
    (should= nil
             (input/first-preferred :right #{}))))

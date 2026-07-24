(ns missile-command.core-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.core :as core]))

(def playfield-size-gen
  (gen/large-integer* {:min 1 :max 10000}))

(defspec new-game-preserves-playfield-dimensions
  100
  (for-all [width playfield-size-gen
            height playfield-size-gen]
    (let [state (core/new-game {:width width :height height})]
      (and (= width (core/playfield-width state))
           (= height (core/playfield-height state))))))

(defspec playfield-accessors-agree-with-map-keys
  50
  (for-all [width playfield-size-gen
            height playfield-size-gen]
    (let [state (core/new-game {:width width :height height})]
      (and (= (:width state) (core/playfield-width state))
           (= (:height state) (core/playfield-height state))))))

(deftest property-suite-loads
  (is (fn? core/new-game)))

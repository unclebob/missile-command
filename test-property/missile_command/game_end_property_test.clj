(ns missile-command.game-end-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.game-end :as game-end]))

(defspec should-enter-only-when-both-counts-zero
  50
  (for-all [living (gen/large-integer* {:min 0 :max 6})
            reserve (gen/large-integer* {:min 0 :max 6})]
    (= (and (zero? living) (zero? reserve))
       (game-end/should-enter? living reserve))))

(defspec fill-radius-covers-corners
  40
  (for-all [width (gen/large-integer* {:min 100 :max 4000})
            height (gen/large-integer* {:min 100 :max 3000})]
    (let [r (double (game-end/fill-radius width height))
          corner (Math/sqrt (+ (Math/pow (/ width 2.0) 2)
                               (Math/pow (/ height 2.0) 2)))]
      (>= r corner))))

(defspec end-fireball-geometry-is-centered
  30
  (for-all [width (gen/large-integer* {:min 100 :max 2000})
            height (gen/large-integer* {:min 100 :max 1500})]
    (let [fb (game-end/make-fireball 7 width height 1.0 1.0)
          layout (game-end/message-layout fb)]
      (and (game-end/fireball-centered? fb width height)
           (game-end/message-centered? layout width height)
           (game-end/message-fills-max-expanse? fb)))))

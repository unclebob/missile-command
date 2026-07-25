(ns missile-command.combat-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.combat :as combat]
            [missile-command.core :as core]
            [missile-command.missiles :as missiles]
            [missile-command.sfx :as sfx]))

(defspec spawn-fireball-emits-boom-and-grows-id
  30
  (for-all [x (gen/large-integer* {:min 0 :max 800})
            y (gen/large-integer* {:min 0 :max 600})]
    (let [state (core/new-game {:width 800 :height 600})
          after (combat/spawn-fireball-at state x y)
          fb (first (core/fireballs after))]
      (and (= 1 (count (core/fireballs after)))
           (= (double x) (double (:x fb)))
           (= (double y) (double (:y fb)))
           (sfx/emitted? after :sfx/boom)
           (= (inc (long (or (:next-entity-id state) 0)))
              (long (:next-entity-id after)))))))

(defspec tick-defensive-phase-arrives-as-fireball
  25
  (for-all []
    (let [state (-> (core/start-game (core/new-game {:width 800 :height 600}))
                    (#(:state (core/handle % {:type :aim :x 400 :y 100})))
                    (#(:state (core/handle % {:type :fire :battery :center}))))
          missile (first (core/defensive-missiles state))
          after (loop [s state n 0]
                  (cond
                    (seq (core/fireballs s)) s
                    (> n 500) s
                    :else (recur (combat/tick-defensive-phase s 0.05) (inc n))))]
      (and (some? missile)
           (empty? (core/defensive-missiles after))
           (seq (core/fireballs after))
           (sfx/emitted? after :sfx/boom)))))

(defspec destroy-targets-marks-only-in-radius
  40
  (for-all [r (gen/double* {:min 5.0 :max 40.0 :NaN? false :infinite? false})
            dx (gen/double* {:min -60.0 :max 60.0 :NaN? false :infinite? false})
            dy (gen/double* {:min -60.0 :max 60.0 :NaN? false :infinite? false})]
    (let [dist (Math/sqrt (+ (* dx dx) (* dy dy)))
          state {:fireballs [{:x 0.0 :y 0.0 :radius r}]
                 :destroyable-targets [{:id 1 :x dx :y dy :destroyed? false}
                                       {:id 2 :x 1000.0 :y 1000.0 :destroyed? false}]}
          after (combat/destroy-targets-in-fireballs state)
          t1 (first (:destroyable-targets after))
          t2 (second (:destroyable-targets after))]
      (and (= (<= dist r) (boolean (:destroyed? t1)))
           (not (:destroyed? t2))))))

(defspec tick-fireballs-drops-expired
  20
  (for-all []
    (let [fb (missiles/make-fireball 1 10 20)
          life (missiles/fireball-lifetime fb)
          state {:fireballs [fb]}
          after (combat/tick-fireballs state (+ life 0.1))]
      (empty? (:fireballs after)))))

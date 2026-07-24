(ns missile-command.missiles-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.missiles :as missiles]))

(defspec clamp-dt-never-exceeds-max
  100
  (for-all [dt (gen/double* {:min 0.0 :max 100.0 :NaN? false :infinite? false})]
    (<= (missiles/clamp-dt dt) missiles/max-dt)))

(defspec position-at-progress-is-on-segment
  80
  (for-all [x0 (gen/large-integer* {:min -1000 :max 1000})
            y0 (gen/large-integer* {:min -1000 :max 1000})
            x1 (gen/large-integer* {:min -1000 :max 1000})
            y1 (gen/large-integer* {:min -1000 :max 1000})
            p (gen/double* {:min 0.0 :max 1.0 :NaN? false :infinite? false})]
    (let [m {:x0 x0 :y0 y0 :x1 x1 :y1 y1}
          pos (missiles/position-at-progress m p)
          expected-x (+ x0 (* p (- x1 x0)))
          expected-y (+ y0 (* p (- y1 y0)))]
      (and (< (Math/abs (- (:x pos) expected-x)) 1e-9)
           (< (Math/abs (- (:y pos) expected-y)) 1e-9)))))

(defspec fireball-radius-peaks-then-returns
  40
  (for-all []
    (let [fb (missiles/make-fireball 1 0 0)
          expand (:expand-seconds fb)
          life (missiles/fireball-lifetime fb)
          r0 (missiles/fireball-radius-at fb 0.0)
          r-mid (missiles/fireball-radius-at fb (/ expand 2.0))
          r-peak (missiles/fireball-radius-at fb expand)
          r-late (missiles/fireball-radius-at fb (+ expand (/ (:contract-seconds fb) 2.0)))
          r-end (missiles/fireball-radius-at fb life)]
      (and (zero? r0)
           (< r-mid r-peak)
           (<= r-late r-peak)
           (zero? r-end)))))

(defspec point-in-fireball-respects-radius
  80
  (for-all [r (gen/double* {:min 1.0 :max 100.0 :NaN? false :infinite? false})
            dx (gen/double* {:min -200.0 :max 200.0 :NaN? false :infinite? false})
            dy (gen/double* {:min -200.0 :max 200.0 :NaN? false :infinite? false})]
    (let [fb {:x 0.0 :y 0.0 :radius r}
          dist (Math/sqrt (+ (* dx dx) (* dy dy)))]
      (= (<= dist r) (boolean (missiles/point-in-fireball? fb dx dy))))))

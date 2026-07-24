(ns missile-command.missiles-spec
  (:require [speclj.core :refer :all]
            [missile-command.missiles :as missiles]))

(describe "make-defensive"
  (it "records origin target battery and speed from the battery"
    (let [bat {:x 10 :y 20 :missile-speed 300.0}
          aim {:x 100 :y 50}
          m (missiles/make-defensive 7 :center bat aim)]
      (should= 7 (:id m))
      (should= :center (:battery m))
      (should= 10 (:x0 m))
      (should= 20 (:y0 m))
      (should= 100 (:x1 m))
      (should= 50 (:y1 m))
      (should= 300.0 (:speed m))
      (should= 0.0 (:progress m))
      (should= 10.0 (double (:x m)))
      (should= 20.0 (double (:y m))))))

(describe "clamp-dt"
  (it "limits large time steps"
    (should= missiles/max-dt (missiles/clamp-dt 1.0))
    (should= 0.01 (missiles/clamp-dt 0.01))))

(describe "advance-defensive"
  (it "moves toward the aim point"
    (let [m (missiles/make-defensive 1 :left {:x 0 :y 0 :missile-speed 100.0}
                                     {:x 100 :y 0})
          advanced (missiles/advance-defensive m 0.1)]
      (should= 0.1 (:progress advanced))
      (should= 10.0 (:x advanced))))

  (it "arrives when progress reaches one"
    (let [m (missiles/make-defensive 1 :left {:x 0 :y 0 :missile-speed 1000.0}
                                     {:x 10 :y 0})]
      (should= missiles/arrived (missiles/advance-defensive m 1.0)))))

(describe "fireballs"
  (it "expands then contracts then expires"
    (let [fb (missiles/make-fireball 1 0 0)
          mid (missiles/advance-fireball fb (/ missiles/fireball-expand-seconds 2))
          peak (missiles/advance-fireball fb missiles/fireball-expand-seconds)
          shrink (missiles/advance-fireball fb (+ missiles/fireball-expand-seconds
                                                  (/ missiles/fireball-contract-seconds 2)))
          gone (missiles/advance-fireball fb (+ missiles/fireball-expand-seconds
                                                missiles/fireball-contract-seconds 0.01))]
      (should (< 0 (:radius mid)))
      (should (< (:radius mid) missiles/fireball-max-radius))
      (should= missiles/fireball-max-radius (:radius peak))
      (should (< (:radius shrink) missiles/fireball-max-radius))
      (should= missiles/expired gone)))

  (it "detects points inside the blast"
    (let [fb (assoc (missiles/make-fireball 1 0 0) :radius 10.0)]
      (should (missiles/point-in-fireball? fb 0 0))
      (should (missiles/point-in-fireball? fb 3 4))
      (should-not (missiles/point-in-fireball? fb 20 0)))))

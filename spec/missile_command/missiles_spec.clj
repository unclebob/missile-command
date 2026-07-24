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
      (should= 20.0 (double (:y m)))))

  (it "computes path length as Euclidean distance from a non-zero origin"
    (let [m (missiles/make-defensive 1 :left {:x 10 :y 20 :missile-speed 1.0}
                                     {:x 13 :y 24})]
      (should= 5.0 (missiles/path-length m)))))

(describe "position-at-progress"
  (it "interpolates along a diagonal segment and clamps progress"
    (let [m (missiles/make-defensive 1 :left {:x 10 :y 20 :missile-speed 1.0}
                                     {:x 110 :y 120})]
      (should= 10.0 (double (:x (missiles/position-at-progress m 0.0))))
      (should= 20.0 (double (:y (missiles/position-at-progress m 0.0))))
      (should= 60.0 (double (:x (missiles/position-at-progress m 0.5))))
      (should= 70.0 (double (:y (missiles/position-at-progress m 0.5))))
      (should= 110.0 (double (:x (missiles/position-at-progress m 1.0))))
      (should= 120.0 (double (:y (missiles/position-at-progress m 1.0))))
      (should= 110.0 (double (:x (missiles/position-at-progress m 2.0))))
      (should= 10.0 (double (:x (missiles/position-at-progress m -1.0))))
      (should= 20.0 (double (:y (missiles/position-at-progress m -1.0)))))))

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

  (it "arrives when progress reaches one exactly"
    (let [m (-> (missiles/make-defensive 1 :left {:x 0 :y 0 :missile-speed 10.0}
                                         {:x 10 :y 0})
                (assoc :progress 0.5))]
      ;; length 10, speed 10, dt 0.5 => delta 0.5 => progress 1.0
      (should= missiles/arrived (missiles/advance-defensive m 0.5))))

  (it "arrives when progress exceeds one"
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
          life (missiles/fireball-lifetime fb)
          almost (missiles/advance-fireball fb (- life 1.0e-9))
          gone (missiles/advance-fireball fb life)]
      (should (< 0 (:radius mid)))
      (should (< (:radius mid) missiles/fireball-max-radius))
      (should= missiles/fireball-max-radius (:radius peak))
      (should (< (:radius shrink) missiles/fireball-max-radius))
      (should-not= missiles/expired almost)
      (should= missiles/expired gone)))

  (it "uses phase boundaries for radius"
    (let [fb (missiles/make-fireball 1 0 0)
          expand missiles/fireball-expand-seconds
          contract missiles/fireball-contract-seconds]
      (should= :pre (missiles/fireball-phase fb 0.0))
      (should= :pre (missiles/fireball-phase fb -1.0))
      (should= :expand (missiles/fireball-phase fb (/ expand 2)))
      (should= :expand (missiles/fireball-phase fb (- expand 1.0e-9)))
      (should= :contract (missiles/fireball-phase fb expand))
      (should= :contract (missiles/fireball-phase fb (+ expand (/ contract 2))))
      (should= :post (missiles/fireball-phase fb (+ expand contract)))
      (should= 0.0 (missiles/fireball-radius-at fb 0.0))
      (should= 0.0 (missiles/fireball-radius-at fb -1.0))
      (should= missiles/fireball-max-radius (missiles/fireball-radius-at fb expand))
      (should= 0.0 (missiles/fireball-radius-at fb (+ expand contract)))
      (should (< 0.0 (missiles/fireball-radius-at fb (/ expand 2))))
      (should (< (missiles/fireball-radius-at fb (+ expand (/ contract 2)))
                 missiles/fireball-max-radius))))

  (it "detects points inside and on the blast boundary"
    (let [fb (assoc (missiles/make-fireball 1 0 0) :radius 5.0)]
      (should (missiles/point-in-fireball? fb 0 0))
      (should (missiles/point-in-fireball? fb 3 4))
      (should (missiles/point-in-fireball? fb 5 0))
      (should-not (missiles/point-in-fireball? fb 5.01 0))
      (should-not (missiles/point-in-fireball? fb 20 0))))

  (it "keeps static fireballs fixed forever"
    (let [fb (missiles/make-static-fireball 2 5 6 12.0)
          later (missiles/advance-fireball fb 10.0)]
      (should= 12.0 (:radius later))
      (should (:static? later)))))

(describe "enemy missiles"
  (it "builds a ballistic enemy toward a target"
    (let [m (missiles/make-enemy 9 {:x 1 :y 0} {:x 1 :y 100}
                                 50.0 :city 2)]
      (should= 9 (:id m))
      (should= :city (:target-kind m))
      (should= 2 (:target-id m))
      (should= 0.0 (:progress m))
      (should= 1.0 (double (:x m)))))

  (it "advances or arrives like defensive missiles"
    (let [m (missiles/make-enemy 1 {:x 0 :y 0} {:x 100 :y 0} 100.0 :city 0)
          advanced (missiles/advance-enemy m 0.1)]
      (should= 0.1 (:progress advanced))
      (should= missiles/arrived
               (missiles/advance-enemy m 2.0)))))

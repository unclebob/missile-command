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
      (should= 300.0 (:speed m)))))

(ns missile-command.jvm.window-spec
  (:require [speclj.core :refer :all]
            [missile-command.jvm.window :as window]))

(describe "centered-location"
  (it "centers a window inside screen bounds"
    (should= {:x 100 :y 50}
             (window/centered-location {:x 0 :y 0 :width 1000 :height 700} 800 600)))

  (it "offsets into a non-origin screen"
    ;; 1728 + (1920-800)/2 = 2288; 413 + (1080-600)/2 = 653
    (should= {:x 2288 :y 653}
             (window/centered-location {:x 1728 :y 413 :width 1920 :height 1080}
                                       800 600))))

(describe "screen-bounds-containing"
  (it "returns a rectangle map with positive size"
    (let [b (window/screen-bounds-containing 0 0)]
      (should (number? (:x b)))
      (should (number? (:y b)))
      (should (pos? (:width b)))
      (should (pos? (:height b))))))

(describe "pointer-location"
  (it "returns integer coordinates"
    (let [p (window/pointer-location)]
      (should (number? (:x p)))
      (should (number? (:y p))))))

(ns missile-command.game-end-spec
  (:require [speclj.core :refer :all]
            [missile-command.game-end :as game-end]))

(describe "THE END pure policy"
  (it "enters only when living cities and reserve are both zero"
    (should (game-end/should-enter? 0 0))
    (should-not (game-end/should-enter? 1 0))
    (should-not (game-end/should-enter? 0 1))
    (should-not (game-end/should-enter? 2 3)))

  (it "uses THE END not Game Over as the message"
    (should= "THE END" game-end/message-text)
    (should= "Game Over" game-end/wrong-message-text)
    (should-not= game-end/message-text game-end/wrong-message-text))

  (it "covers playfield corners with fill radius"
    (let [r (game-end/fill-radius 800 600)
          cx 400.0
          cy 300.0
          corner-dist (Math/sqrt (+ (* 400.0 400.0) (* 300.0 300.0)))]
      (should (>= (double r) corner-dist))))

  (it "builds a centered end fireball"
    (let [fb (game-end/make-fireball 1 800 600 2.0 1.0)]
      (should (game-end/fireball-centered? fb 800 600))
      (should= (double (game-end/fill-radius 800 600))
               (double (:max-radius fb)))
      (should (:end-fireball? fb))
      (should-not= false (:end-fireball? fb))
      (should (game-end/message-fills-max-expanse? fb))
      (should (game-end/message-centered?
               (game-end/message-layout fb) 800 600))))

  (it "treats a fireball as filling when radius is at least 99% of max"
    (let [max-r 100.0
          almost (assoc (game-end/make-fireball 1 800 600 1.0 1.0)
                        :max-radius max-r
                        :radius (* 0.99 max-r))
          under (assoc almost :radius (* 0.989 max-r))]
      (should (game-end/fireball-fills-playfield? almost))
      (should-not (game-end/fireball-fills-playfield? under))))

  (it "reports point visibility only for live end fireballs"
    (let [fb (assoc (game-end/make-fireball 1 800 600 1.0 1.0)
                    :radius 50.0
                    :max-radius 500.0)]
      (should (game-end/point-visible? fb 400 300))
      (should-not (game-end/point-visible? nil 400 300))
      (should-not (game-end/point-visible? fb 0 0)))))

(ns missile-command.scoring
  "Score point tables, pure award math, and bonus-city threshold policy.
  Multiplier schedule lives in waves.")

(def points-enemy-missile 25)
(def points-smart-bomb 125)
(def points-flyer 100)
(def points-unused-missile 5)
(def points-surviving-city 100)
(def default-bonus-city-threshold 10000)

(defn with-multiplier
  "Scale base points by the wave multiplier."
  [base-points multiplier]
  (* (long base-points) (long multiplier)))

(defn base-kill-points
  "Base score for destroying an enemy of the given kind (:smart or ballistic)."
  [enemy-kind]
  (if (= enemy-kind :smart)
    points-smart-bomb
    points-enemy-missile))

(defn enemy-kill-points
  "Points for destroying one enemy at the given multiplier.
  Arity-1 defaults to a normal ballistic missile; arity-2 takes enemy kind."
  ([multiplier]
   (enemy-kill-points :ballistic multiplier))
  ([enemy-kind multiplier]
   (with-multiplier (base-kill-points enemy-kind) multiplier)))

(defn flyer-kill-points
  "Points for destroying a bomber or satellite at the given multiplier."
  [multiplier]
  (with-multiplier points-flyer multiplier))

(defn wave-end-points
  "Unused ammo and surviving cities scaled by the completing wave's multiplier."
  [unused-ammo living-city-count multiplier]
  (with-multiplier
    (+ (* (long unused-ammo) points-unused-missile)
       (* (long living-city-count) points-surviving-city))
    multiplier))

(defn thresholds-crossed
  "How many bonus-city thresholds the score has crossed."
  [score-value threshold]
  (let [t (long threshold)]
    (if (pos? t)
      (quot (long score-value) t)
      0)))

(defn new-bonus-city-awards
  "New reserve cities earned since already-awarded count."
  [score-value threshold already-awarded]
  (max 0 (- (thresholds-crossed score-value threshold)
            (long already-awarded))))

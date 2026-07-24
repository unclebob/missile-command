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

(def ^:private no-threshold-crossings 0)

(defn thresholds-crossed
  "How many bonus-city thresholds the score has crossed."
  [score-value threshold]
  (let [t (long threshold)]
    (if-not (pos? t)
      no-threshold-crossings
      (quot (long score-value) t))))

(defn new-bonus-city-awards
  "New reserve cities earned since already-awarded count."
  [score-value threshold already-awarded]
  (max 0 (- (thresholds-crossed score-value threshold)
            (long already-awarded))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T14:40:19.640353-05:00", :module-hash "1215470259", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-150707323"} {:id "def/points-enemy-missile", :kind "def", :line 5, :end-line 5, :hash "804910811"} {:id "def/points-smart-bomb", :kind "def", :line 6, :end-line 6, :hash "847017128"} {:id "def/points-flyer", :kind "def", :line 7, :end-line 7, :hash "674029873"} {:id "def/points-unused-missile", :kind "def", :line 8, :end-line 8, :hash "-65581501"} {:id "def/points-surviving-city", :kind "def", :line 9, :end-line 9, :hash "1254723278"} {:id "def/default-bonus-city-threshold", :kind "def", :line 10, :end-line 10, :hash "-371953978"} {:id "defn/with-multiplier", :kind "defn", :line 12, :end-line 15, :hash "30054051"} {:id "defn/base-kill-points", :kind "defn", :line 17, :end-line 22, :hash "1487147242"} {:id "defn/enemy-kill-points", :kind "defn", :line 24, :end-line 30, :hash "1257060982"} {:id "defn/flyer-kill-points", :kind "defn", :line 32, :end-line 35, :hash "1365436964"} {:id "defn/wave-end-points", :kind "defn", :line 37, :end-line 43, :hash "-942170136"} {:id "def/no-threshold-crossings", :kind "def", :line 45, :end-line 45, :hash "-678677679"} {:id "defn/thresholds-crossed", :kind "defn", :line 47, :end-line 53, :hash "1518687396"} {:id "defn/new-bonus-city-awards", :kind "defn", :line 55, :end-line 59, :hash "-1239773855"}]}
;; clj-mutate-manifest-end

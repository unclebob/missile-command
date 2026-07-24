(ns missile-command.waves)

(def initial-wave 1)
(def full-ammo 10)
(def max-multiplier 6)
(def points-enemy-missile 25)
(def points-unused-missile 5)
(def points-surviving-city 100)

;; Wave-1 base speed (px/s). Kept moderate so early play is defendable on
;; a ~600px playfield (~11s sky→ground at 50 px/s). Higher waves ramp 25%/step.
(def base-enemy-speed 50.0)
(def enemy-speed-wave-factor 0.25)

(defn enemy-count
  "Number of ballistic enemies scheduled for a wave."
  [wave]
  (+ 2 wave))

(defn enemy-speed
  "Enemy missile speed (px/s) for a wave."
  [wave]
  (* base-enemy-speed (+ 1.0 (* enemy-speed-wave-factor (dec wave)))))

(defn multiplier
  "Score multiplier for a wave: +1× every two waves, capped at max-multiplier."
  [wave]
  (min max-multiplier (+ 1 (quot (dec (long wave)) 2))))

(defn schedule-metrics
  "Observable difficulty metrics for a wave."
  [wave]
  {:wave wave
   :enemy-count (enemy-count wave)
   :enemy-speed (enemy-speed wave)
   :multiplier (multiplier wave)})

(defn harder?
  "True when high-wave metrics exceed low-wave by count or speed."
  [low-metrics high-metrics]
  (or (> (:enemy-count high-metrics) (:enemy-count low-metrics))
      (> (:enemy-speed high-metrics) (:enemy-speed low-metrics))))

(defn sky-origin-x
  "Deterministic sky entry x for the i-th of n wave enemies across playfield width."
  [width i n]
  (let [w (double width)]
    (if (pos? n)
      (* w (/ (+ (double i) 0.5) (double n)))
      0.0)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T13:09:05.499805-05:00", :module-hash "-1289698230", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "644944117"} {:id "def/initial-wave", :kind "def", :line 3, :end-line 3, :hash "-1132024059"} {:id "def/full-ammo", :kind "def", :line 4, :end-line 4, :hash "1169396743"} {:id "defn/enemy-count", :kind "defn", :line 6, :end-line 9, :hash "761033334"} {:id "defn/enemy-speed", :kind "defn", :line 11, :end-line 14, :hash "-1246023165"} {:id "defn/schedule-metrics", :kind "defn", :line 16, :end-line 21, :hash "1068899611"} {:id "defn/harder?", :kind "defn", :line 23, :end-line 27, :hash "346906739"}]}
;; clj-mutate-manifest-end

(ns missile-command.waves)

(def initial-wave 1)
(def full-ammo 10)
(def max-multiplier 6)

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

(defn mirv-count
  "Number of MIRV-capable enemies scheduled for a wave (0 on early waves)."
  [wave]
  (max 0 (- (quot (long wave) 2) 1)))

(defn smart-bomb-count
  "Number of smart bombs scheduled for a wave (0 until later waves)."
  [wave]
  (max 0 (quot (- (long wave) 5) 2)))

(defn- flyer-count-from-wave
  "1 scheduled flyer when wave is at least min-wave, else 0."
  [wave min-wave]
  (if (>= (long wave) (long min-wave)) 1 0))

(defn bomber-count
  "Bombers scheduled for a wave (from wave 8)."
  [wave]
  (flyer-count-from-wave wave 8))

(defn satellite-count
  "Satellites scheduled for a wave (from wave 9)."
  [wave]
  (flyer-count-from-wave wave 9))

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
   :multiplier (multiplier wave)
   :mirv-count (mirv-count wave)
   :smart-bomb-count (smart-bomb-count wave)
   :bomber-count (bomber-count wave)
   :satellite-count (satellite-count wave)})

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
;; {:version 1, :tested-at "2026-07-24T14:45:57.987796-05:00", :module-hash "1607008161", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "644944117"} {:id "def/initial-wave", :kind "def", :line 3, :end-line 3, :hash "-1132024059"} {:id "def/full-ammo", :kind "def", :line 4, :end-line 4, :hash "1169396743"} {:id "def/max-multiplier", :kind "def", :line 5, :end-line 5, :hash "315372844"} {:id "def/base-enemy-speed", :kind "def", :line 9, :end-line 9, :hash "-227771228"} {:id "def/enemy-speed-wave-factor", :kind "def", :line 10, :end-line 10, :hash "-600801268"} {:id "defn/enemy-count", :kind "defn", :line 12, :end-line 15, :hash "761033334"} {:id "defn/enemy-speed", :kind "defn", :line 17, :end-line 20, :hash "-341813790"} {:id "defn/mirv-count", :kind "defn", :line 22, :end-line 25, :hash "-969546742"} {:id "defn/smart-bomb-count", :kind "defn", :line 27, :end-line 30, :hash "1265738633"} {:id "defn-/flyer-count-from-wave", :kind "defn-", :line 32, :end-line 35, :hash "-1203787558"} {:id "defn/bomber-count", :kind "defn", :line 37, :end-line 40, :hash "1315798360"} {:id "defn/satellite-count", :kind "defn", :line 42, :end-line 45, :hash "2000732935"} {:id "defn/multiplier", :kind "defn", :line 47, :end-line 50, :hash "666855802"} {:id "defn/schedule-metrics", :kind "defn", :line 52, :end-line 62, :hash "-951056565"} {:id "defn/harder?", :kind "defn", :line 64, :end-line 68, :hash "346906739"} {:id "defn/sky-origin-x", :kind "defn", :line 70, :end-line 76, :hash "-107469784"}]}
;; clj-mutate-manifest-end

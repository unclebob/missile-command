(ns missile-command.waves
  (:require [missile-command.options :as options]))

(def initial-wave 1)
(def full-ammo 10)
(def max-multiplier 6)

;; Wave-1 base speed (px/s). ~15s sky→ground on a 600px playfield at 40 px/s.
;; Higher waves add 12.5% of base per step (5 px/s each wave).
(def base-enemy-speed 40.0)
(def enemy-speed-wave-factor 0.125)

(def attacks-per-wave 3)

(defn enemy-count
  "Number of ballistic enemies scheduled for a wave (arcade base).
  Fixed at three attacks per wave; difficulty still ramps via speed/specials."
  [wave]
  (long attacks-per-wave))

(defn enemy-speed
  "Enemy missile speed (px/s) for a wave (arcade base)."
  [wave]
  (* base-enemy-speed (+ 1.0 (* enemy-speed-wave-factor (dec wave)))))

(defn mirv-count
  "Number of MIRV-capable enemies scheduled for a wave (0 on early waves)."
  [wave]
  (max 0 (- (quot (long wave) 2) 1)))

(defn smart-bomb-count
  "Number of smart bombs scheduled for a wave (from wave 3)."
  [wave]
  (max 0 (quot (- (long wave) 2) 2)))

(defn- flyer-count-from-wave
  "1 scheduled flyer when wave is at least min-wave, else 0."
  [wave min-wave]
  (if (>= (long wave) (long min-wave)) 1 0))

(defn bomber-count
  "Bombers scheduled for a wave (from wave 4)."
  [wave]
  (flyer-count-from-wave wave 4))

(defn satellite-count
  "Satellites scheduled for a wave (from wave 5)."
  [wave]
  (flyer-count-from-wave wave 5))

(defn multiplier
  "Score multiplier for a wave: +1× every two waves, capped at max-multiplier."
  [wave]
  (min max-multiplier (+ 1 (quot (dec (long wave)) 2))))

(defn schedule-metrics
  "Observable difficulty metrics for a wave.
  Optional difficulty preset scales enemy count/speed (arcade default)."
  ([wave]
   (schedule-metrics wave options/difficulty-arcade))
  ([wave difficulty]
   (let [factor (options/difficulty-factor difficulty)
         arcade-count (enemy-count wave)
         arcade-speed (enemy-speed wave)]
     {:wave wave
      :enemy-count (options/scale-enemy-count arcade-count factor)
      :enemy-speed (options/scale-enemy-speed arcade-speed factor)
      :multiplier (multiplier wave)
      :mirv-count (mirv-count wave)
      :smart-bomb-count (smart-bomb-count wave)
      :bomber-count (bomber-count wave)
      :satellite-count (satellite-count wave)})))

(defn harder?
  "True when high-wave metrics exceed low-wave by count or speed."
  [low-metrics high-metrics]
  (or (> (:enemy-count high-metrics) (:enemy-count low-metrics))
      (> (:enemy-speed high-metrics) (:enemy-speed low-metrics))))

(defn schedule-metrics-for-state
  "Wave schedule metrics using the state's difficulty options."
  [state wave-number]
  (schedule-metrics wave-number
                    (options/difficulty (options/of state))))

(defn sky-origin-x
  "Deterministic sky entry x for the i-th of n wave enemies across playfield width."
  [width i n]
  (let [w (double width)]
    (if (pos? n)
      (* w (/ (+ (double i) 0.5) (double n)))
      0.0)))

(defn target-pool
  "Eligible wave targets from living city ids and non-destroyed battery ids."
  [city-ids battery-ids]
  (into (mapv (fn [id] [:city id]) city-ids)
        (mapv (fn [id] [:battery id]) battery-ids)))

(defn cycle-targets
  "Take n targets cycling the eligible city+battery pool."
  [pool n]
  (if (seq pool) (vec (take n (cycle pool))) []))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-25T10:05:40.784359-05:00", :module-hash "1889971339", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "1345759526"} {:id "def/initial-wave", :kind "def", :line 4, :end-line 4, :hash "-1132024059"} {:id "def/full-ammo", :kind "def", :line 5, :end-line 5, :hash "1169396743"} {:id "def/max-multiplier", :kind "def", :line 6, :end-line 6, :hash "315372844"} {:id "def/base-enemy-speed", :kind "def", :line 10, :end-line 10, :hash "1317821312"} {:id "def/enemy-speed-wave-factor", :kind "def", :line 11, :end-line 11, :hash "-1903396821"} {:id "def/attacks-per-wave", :kind "def", :line 13, :end-line 13, :hash "383603444"} {:id "defn/enemy-count", :kind "defn", :line 15, :end-line 19, :hash "1989133071"} {:id "defn/enemy-speed", :kind "defn", :line 21, :end-line 24, :hash "688271298"} {:id "defn/mirv-count", :kind "defn", :line 26, :end-line 29, :hash "-969546742"} {:id "defn/smart-bomb-count", :kind "defn", :line 31, :end-line 34, :hash "-2054990989"} {:id "defn-/flyer-count-from-wave", :kind "defn-", :line 36, :end-line 39, :hash "-1203787558"} {:id "defn/bomber-count", :kind "defn", :line 41, :end-line 44, :hash "844241977"} {:id "defn/satellite-count", :kind "defn", :line 46, :end-line 49, :hash "1569826066"} {:id "defn/multiplier", :kind "defn", :line 51, :end-line 54, :hash "666855802"} {:id "defn/schedule-metrics", :kind "defn", :line 56, :end-line 72, :hash "-372091869"} {:id "defn/harder?", :kind "defn", :line 74, :end-line 78, :hash "346906739"} {:id "defn/schedule-metrics-for-state", :kind "defn", :line 80, :end-line 84, :hash "-1998674515"} {:id "defn/sky-origin-x", :kind "defn", :line 86, :end-line 92, :hash "-107469784"} {:id "defn/target-pool", :kind "defn", :line 94, :end-line 98, :hash "101013267"} {:id "defn/cycle-targets", :kind "defn", :line 100, :end-line 103, :hash "595141872"}]}
;; clj-mutate-manifest-end

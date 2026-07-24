(ns missile-command.waves)

(def initial-wave 1)
(def full-ammo 10)

(defn enemy-count
  "Number of ballistic enemies scheduled for a wave."
  [wave]
  (+ 2 wave))

(defn enemy-speed
  "Enemy missile speed (px/s) for a wave."
  [wave]
  (* 100.0 (+ 1.0 (* 0.25 (dec wave)))))

(defn schedule-metrics
  "Observable difficulty metrics for a wave."
  [wave]
  {:wave wave
   :enemy-count (enemy-count wave)
   :enemy-speed (enemy-speed wave)})

(defn harder?
  "True when high-wave metrics exceed low-wave by count or speed."
  [low-metrics high-metrics]
  (or (> (:enemy-count high-metrics) (:enemy-count low-metrics))
      (> (:enemy-speed high-metrics) (:enemy-speed low-metrics))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T13:09:05.499805-05:00", :module-hash "-1289698230", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "644944117"} {:id "def/initial-wave", :kind "def", :line 3, :end-line 3, :hash "-1132024059"} {:id "def/full-ammo", :kind "def", :line 4, :end-line 4, :hash "1169396743"} {:id "defn/enemy-count", :kind "defn", :line 6, :end-line 9, :hash "761033334"} {:id "defn/enemy-speed", :kind "defn", :line 11, :end-line 14, :hash "-1246023165"} {:id "defn/schedule-metrics", :kind "defn", :line 16, :end-line 21, :hash "1068899611"} {:id "defn/harder?", :kind "defn", :line 23, :end-line 27, :hash "346906739"}]}
;; clj-mutate-manifest-end

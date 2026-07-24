(ns missile-command.game-end
  "Pure THE END policy, geometry, and presentation helpers."
  (:require [missile-command.missiles :as missiles]))

(def message-text "THE END")
(def wrong-message-text "Game Over")
(def ^:private end-fireball-marker true)

(defn should-enter?
  "True when no living cities remain and the bonus reserve is empty."
  [living-city-count bonus-reserve]
  (and (zero? (long living-city-count))
       (zero? (long bonus-reserve))))

(defn fill-radius
  "Radius from center that covers the playfield corners."
  [width height]
  (let [w (double width)
        h (double height)]
    (Math/ceil (Math/sqrt (+ (* (/ w 2.0) (/ w 2.0))
                             (* (/ h 2.0) (/ h 2.0)))))))

(defn make-fireball
  [fireball-id width height expand-seconds contract-seconds]
  (let [cx (quot width 2)
        cy (quot height 2)]
    {:id fireball-id
     :x cx
     :y cy
     :age 0.0
     :radius 0.0
     :max-radius (fill-radius width height)
     :expand-seconds expand-seconds
     :contract-seconds contract-seconds
     :end-fireball? end-fireball-marker}))

(defn reveal-fraction
  [fireball]
  (let [max-r (double (or (:max-radius fireball) 1.0))
        r (double (or (:radius fireball) 0.0))]
    (min 1.0 (/ r max-r))))

(defn- coords-centered?
  [x y width height]
  (and (= (long x) (quot width 2))
       (= (long y) (quot height 2))))

(defn fireball-centered?
  [fireball width height]
  (and fireball
       (coords-centered? (:x fireball) (:y fireball) width height)))

(defn fireball-fills-playfield?
  [fireball]
  (and fireball
       (>= (double (:radius fireball))
           (* 0.99 (double (:max-radius fireball))))))

(defn message-layout
  "Glyph bounds: square matching the max end-fireball diameter."
  [fireball]
  (let [r (double (or (:max-radius fireball) 0.0))
        d (* 2.0 r)]
    {:center-x (double (or (:x fireball) 0.0))
     :center-y (double (or (:y fireball) 0.0))
     :width d
     :height d
     :radius r}))

(defn message-fills-max-expanse?
  [fireball]
  (let [layout (message-layout fireball)
        d (* 2.0 (double (:max-radius fireball 0.0)))]
    (and fireball
         (= (:width layout) d)
         (= (:height layout) d))))

(defn message-centered?
  [layout width height]
  (coords-centered? (:center-x layout) (:center-y layout) width height))

(defn point-visible?
  [fireball x y]
  (boolean
   (when fireball
     (missiles/point-in-fireball? fireball x y))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T15:41:19.785843-05:00", :module-hash "2082265044", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-999749667"} {:id "def/message-text", :kind "def", :line 5, :end-line 5, :hash "275705922"} {:id "def/wrong-message-text", :kind "def", :line 6, :end-line 6, :hash "-2095033555"} {:id "def/end-fireball-marker", :kind "def", :line 7, :end-line 7, :hash "-1808708233"} {:id "defn/should-enter?", :kind "defn", :line 9, :end-line 13, :hash "1063679764"} {:id "defn/fill-radius", :kind "defn", :line 15, :end-line 21, :hash "-869671818"} {:id "defn/make-fireball", :kind "defn", :line 23, :end-line 35, :hash "1243157513"} {:id "defn/reveal-fraction", :kind "defn", :line 37, :end-line 41, :hash "-846952140"} {:id "defn-/coords-centered?", :kind "defn-", :line 43, :end-line 46, :hash "-1137442894"} {:id "defn/fireball-centered?", :kind "defn", :line 48, :end-line 51, :hash "174824089"} {:id "defn/fireball-fills-playfield?", :kind "defn", :line 53, :end-line 57, :hash "1732154877"} {:id "defn/message-layout", :kind "defn", :line 59, :end-line 68, :hash "-400208368"} {:id "defn/message-fills-max-expanse?", :kind "defn", :line 70, :end-line 76, :hash "-939310460"} {:id "defn/message-centered?", :kind "defn", :line 78, :end-line 80, :hash "2140552443"} {:id "defn/point-visible?", :kind "defn", :line 82, :end-line 86, :hash "1535941698"}]}
;; clj-mutate-manifest-end

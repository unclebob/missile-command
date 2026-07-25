(ns missile-command.wave-banner
  "Between-waves WAVE N banner: enter then exit motion."
  (:require [missile-command.screens :as screens]))

(def phase-enter :enter)
(def phase-exit :exit)
;; Long enough to read WAVE N while sliding (was 0.6 / 0.6).
(def enter-duration 2.0)
(def exit-duration 2.0)
(def offscreen-margin 80.0)

(defn screen?
  [state]
  (screens/wave-banner? state))

(defn of
  [state]
  (:wave-banner state))

(defn text
  [state]
  (or (:text (of state)) ""))

(defn subtitle
  "Optional second line (e.g. Bonus City when a city was restored)."
  [state]
  (or (:subtitle (of state)) ""))

(defn bonus-city?
  [state]
  (boolean (:bonus-city? (of state))))

(defn announced-wave
  [state]
  (long (or (:announced-wave (of state)) 0)))

(defn phase
  [state]
  (:phase (of state)))

(defn text-position
  [state]
  (let [b (of state)]
    {:x (double (or (:x b) 0.0))
     :y (double (or (:y b) 0.0))}))

(defn- playfield-center
  [width height]
  {:x (/ (double width) 2.0)
   :y (/ (double height) 2.0)})

(defn distance-to-center
  ([state]
   (distance-to-center state
                       (or (:width state) 0)
                       (or (:height state) 0)))
  ([state width height]
   (let [c (playfield-center width height)
         p (text-position state)
         dx (- (:x p) (:x c))
         dy (- (:y p) (:y c))]
     (Math/sqrt (+ (* dx dx) (* dy dy))))))

(def bonus-city-subtitle "Bonus City")

(defn- banner-text-for
  [wave]
  (str "WAVE " (long wave)))

(defn- lerp
  [a b t]
  (+ (double a) (* (- (double b) (double a)) (double t))))

(defn- clamp01
  [t]
  (max 0.0 (min 1.0 (double t))))

(defn make
  ([width height announced-wave]
   (make width height announced-wave false))
  ([width height announced-wave bonus-city?]
   (let [c (playfield-center width height)
         start-x (- 0.0 offscreen-margin)
         bonus? (boolean bonus-city?)]
     {:announced-wave (long announced-wave)
      :text (banner-text-for announced-wave)
      :bonus-city? bonus?
      :subtitle (if bonus? bonus-city-subtitle "")
      :phase phase-enter
      :progress 0.0
      :x start-x
      :y (:y c)
      :enter-start-x start-x
      :center-x (:x c)
      :center-y (:y c)
      :exit-end-x (+ (double width) offscreen-margin)})))

(defn enter
  "Put state on wave-banner screen announcing wave.
  Optional bonus-city? shows a Bonus City subtitle when a city was restored."
  ([state announced-wave]
   (enter state announced-wave false))
  ([state announced-wave bonus-city?]
   (enter state
          (or (:width state) 0)
          (or (:height state) 0)
          announced-wave
          bonus-city?))
  ([state width height announced-wave]
   (enter state width height announced-wave false))
  ([state width height announced-wave bonus-city?]
   (assoc state
          :screen screens/wave-banner
          :wave-banner (make width height announced-wave bonus-city?))))

(defn clear
  "Leave banner and restore playing screen."
  [state]
  (assoc state
         :screen screens/playing
         :wave-banner nil))

(defn- phase-duration
  [ph]
  (if (= ph phase-enter) enter-duration exit-duration))

(defn- complete-enter
  [state b]
  (assoc state :wave-banner
         (assoc b
                :phase phase-exit
                :progress 0.0
                :x (:center-x b)
                :y (:center-y b))))

(defn- advance-banner-x
  [state b ph progress']
  (let [x (if (= ph phase-enter)
            (lerp (:enter-start-x b) (:center-x b) progress')
            (lerp (:center-x b) (:exit-end-x b) progress'))]
    (assoc state :wave-banner
           (assoc b :progress progress' :x x :y (:center-y b)))))

(defn- finish-phase
  [state b ph finish-fn]
  (if (= ph phase-enter)
    (complete-enter state b)
    (finish-fn state)))

(defn- tick-banner
  [state b dt finish-fn]
  (let [ph (:phase b)
        progress (double (or (:progress b) 0.0))
        progress' (clamp01 (+ progress (/ (double dt) (phase-duration ph))))]
    (if (>= progress' 1.0)
      (finish-phase state b ph finish-fn)
      (advance-banner-x state b ph progress'))))

(defn tick
  "Advance banner animation; returns updated state.
  When finished, calls finish-fn with state (no banner)."
  [state dt finish-fn]
  (if-let [b (of state)]
    (tick-banner state b dt finish-fn)
    (finish-fn state)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T16:26:40.312112-05:00", :module-hash "-1757701544", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-1156496799"} {:id "def/phase-enter", :kind "def", :line 5, :end-line 5, :hash "-1325013704"} {:id "def/phase-exit", :kind "def", :line 6, :end-line 6, :hash "571724543"} {:id "def/enter-duration", :kind "def", :line 7, :end-line 7, :hash "969557294"} {:id "def/exit-duration", :kind "def", :line 8, :end-line 8, :hash "-1906795612"} {:id "def/offscreen-margin", :kind "def", :line 9, :end-line 9, :hash "-1275849371"} {:id "defn/screen?", :kind "defn", :line 11, :end-line 13, :hash "-626378302"} {:id "defn/of", :kind "defn", :line 15, :end-line 17, :hash "-981582658"} {:id "defn/text", :kind "defn", :line 19, :end-line 21, :hash "1107135439"} {:id "defn/announced-wave", :kind "defn", :line 23, :end-line 25, :hash "-1105766107"} {:id "defn/phase", :kind "defn", :line 27, :end-line 29, :hash "-496719693"} {:id "defn/text-position", :kind "defn", :line 31, :end-line 35, :hash "1102855509"} {:id "defn-/playfield-center", :kind "defn-", :line 37, :end-line 40, :hash "-466748197"} {:id "defn/distance-to-center", :kind "defn", :line 42, :end-line 52, :hash "737446024"} {:id "defn-/banner-text-for", :kind "defn-", :line 54, :end-line 56, :hash "-1082966793"} {:id "defn-/lerp", :kind "defn-", :line 58, :end-line 60, :hash "-1528249028"} {:id "defn-/clamp01", :kind "defn-", :line 62, :end-line 64, :hash "2048038306"} {:id "defn/make", :kind "defn", :line 66, :end-line 79, :hash "-963403718"} {:id "defn/enter", :kind "defn", :line 81, :end-line 91, :hash "-1932914319"} {:id "defn/clear", :kind "defn", :line 93, :end-line 98, :hash "725549826"} {:id "defn/tick", :kind "defn", :line 100, :end-line 124, :hash "-455791148"}]}
;; clj-mutate-manifest-end

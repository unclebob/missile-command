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
;; {:version 1, :tested-at "2026-07-25T10:12:01.181259-05:00", :module-hash "-1984950152", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-1156496799"} {:id "def/phase-enter", :kind "def", :line 5, :end-line 5, :hash "-1325013704"} {:id "def/phase-exit", :kind "def", :line 6, :end-line 6, :hash "571724543"} {:id "def/enter-duration", :kind "def", :line 8, :end-line 8, :hash "212342138"} {:id "def/exit-duration", :kind "def", :line 9, :end-line 9, :hash "698481786"} {:id "def/offscreen-margin", :kind "def", :line 10, :end-line 10, :hash "-1275849371"} {:id "defn/screen?", :kind "defn", :line 12, :end-line 14, :hash "-626378302"} {:id "defn/of", :kind "defn", :line 16, :end-line 18, :hash "-981582658"} {:id "defn/text", :kind "defn", :line 20, :end-line 22, :hash "1107135439"} {:id "defn/subtitle", :kind "defn", :line 24, :end-line 27, :hash "1027294748"} {:id "defn/bonus-city?", :kind "defn", :line 29, :end-line 31, :hash "1429856038"} {:id "defn/announced-wave", :kind "defn", :line 33, :end-line 35, :hash "-1105766107"} {:id "defn/phase", :kind "defn", :line 37, :end-line 39, :hash "-496719693"} {:id "defn/text-position", :kind "defn", :line 41, :end-line 45, :hash "1102855509"} {:id "defn-/playfield-center", :kind "defn-", :line 47, :end-line 50, :hash "-466748197"} {:id "defn/distance-to-center", :kind "defn", :line 52, :end-line 62, :hash "737446024"} {:id "def/bonus-city-subtitle", :kind "def", :line 64, :end-line 64, :hash "1429876496"} {:id "defn-/banner-text-for", :kind "defn-", :line 66, :end-line 68, :hash "-1082966793"} {:id "defn-/lerp", :kind "defn-", :line 70, :end-line 72, :hash "-1528249028"} {:id "defn-/clamp01", :kind "defn-", :line 74, :end-line 76, :hash "2048038306"} {:id "defn/make", :kind "defn", :line 78, :end-line 96, :hash "-999226636"} {:id "defn/enter", :kind "defn", :line 98, :end-line 114, :hash "-1173265665"} {:id "defn/clear", :kind "defn", :line 116, :end-line 121, :hash "725549826"} {:id "defn-/phase-duration", :kind "defn-", :line 123, :end-line 125, :hash "-847565443"} {:id "defn-/complete-enter", :kind "defn-", :line 127, :end-line 134, :hash "-1727395524"} {:id "defn-/advance-banner-x", :kind "defn-", :line 136, :end-line 142, :hash "-79769897"} {:id "defn-/finish-phase", :kind "defn-", :line 144, :end-line 148, :hash "-539197714"} {:id "defn-/tick-banner", :kind "defn-", :line 150, :end-line 157, :hash "-1415388649"} {:id "defn/tick", :kind "defn", :line 159, :end-line 165, :hash "1743369184"}]}
;; clj-mutate-manifest-end

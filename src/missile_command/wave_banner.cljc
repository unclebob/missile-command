(ns missile-command.wave-banner
  "Between-waves WAVE N banner: enter then exit motion."
  (:require [missile-command.screens :as screens]))

(def phase-enter :enter)
(def phase-exit :exit)
(def enter-duration 0.6)
(def exit-duration 0.6)
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
  [width height announced-wave]
  (let [c (playfield-center width height)
        start-x (- 0.0 offscreen-margin)]
    {:announced-wave (long announced-wave)
     :text (banner-text-for announced-wave)
     :phase phase-enter
     :progress 0.0
     :x start-x
     :y (:y c)
     :enter-start-x start-x
     :center-x (:x c)
     :center-y (:y c)
     :exit-end-x (+ (double width) offscreen-margin)}))

(defn enter
  "Put state on wave-banner screen announcing wave."
  ([state announced-wave]
   (enter state
          (or (:width state) 0)
          (or (:height state) 0)
          announced-wave))
  ([state width height announced-wave]
   (assoc state
          :screen screens/wave-banner
          :wave-banner (make width height announced-wave))))

(defn clear
  "Leave banner and restore playing screen."
  [state]
  (assoc state
         :screen screens/playing
         :wave-banner nil))

(defn tick
  "Advance banner animation; returns updated state.
  When finished, calls finish-fn with state (no banner)."
  [state dt finish-fn]
  (let [b (of state)]
    (if-not b
      (finish-fn state)
      (let [ph (:phase b)
            progress (double (or (:progress b) 0.0))
            dur (if (= ph phase-enter) enter-duration exit-duration)
            progress' (clamp01 (+ progress (/ (double dt) dur)))]
        (if (and (= ph phase-enter) (>= progress' 1.0))
          (assoc state :wave-banner
                 (assoc b
                        :phase phase-exit
                        :progress 0.0
                        :x (:center-x b)
                        :y (:center-y b)))
          (if (and (= ph phase-exit) (>= progress' 1.0))
            (finish-fn state)
            (let [x (if (= ph phase-enter)
                      (lerp (:enter-start-x b) (:center-x b) progress')
                      (lerp (:center-x b) (:exit-end-x b) progress'))]
              (assoc state :wave-banner
                     (assoc b :progress progress' :x x :y (:center-y b))))))))))

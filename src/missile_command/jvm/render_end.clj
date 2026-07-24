(ns missile-command.jvm.render-end
  "Host drawing for THE END overlay."
  (:require [quil.core :as q]
            [missile-command.core :as core])
  (:import [java.awt Shape]
           [java.awt.geom Ellipse2D$Double]
           [processing.awt PGraphicsJava2D]))

(defn- fit-text-height
  "Text height so msg width fits inside ~75% of the max fireball diameter."
  [msg max-r]
  (let [max-r (double max-r)
        ;; Diameter is 2*max-r; leave a clear rim inside the disk.
        target-w (* 1.45 max-r)
        probe 48.0]
    (q/text-size probe)
    (let [tw (double (q/text-width (str msg)))]
      (max 10.0 (* probe (/ target-w (max tw 1.0)))))))

(defn- with-disk-clip!
  "Restrict drawing to the circle centered at (cx,cy) with radius r."
  [cx cy r draw-fn]
  (let [r (double r)
        cx (double cx)
        cy (double cy)
        g (q/current-graphics)]
    (if (instance? PGraphicsJava2D g)
      (let [^java.awt.Graphics2D g2 (.-g2 ^PGraphicsJava2D g)
            ^Shape prev (.getClip g2)
            disk (Ellipse2D$Double. (- cx r) (- cy r) (* 2.0 r) (* 2.0 r))]
        (try
          (.clip g2 disk)
          (draw-fn)
          (finally
            (.setClip g2 prev))))
      ;; Fallback: axis-aligned box of the disk (still better than no clip).
      (do
        (q/clip (- cx r) (- cy r) (* 2.0 r) (* 2.0 r))
        (try
          (draw-fn)
          (finally
            (q/no-clip)))))))

(defn- draw-message!
  [cx cy r max-r msg]
  (let [text-h (fit-text-height msg max-r)]
    (with-disk-clip! cx cy r
      (fn []
        (q/text-align :center :center)
        (q/text-size text-h)
        ;; Dark body + light edge so letters read on the orange fireball.
        (q/fill 255 240 200)
        (doseq [[dx dy] [[-2 0] [2 0] [0 -2] [0 2]]]
          (q/text (str msg) (+ cx dx) (+ cy dy)))
        (q/fill 30 10 0)
        (q/text (str msg) cx cy)
        (q/text-align :left :baseline)))))

(defn overlay!
  "Draw the centered end fireball and clip THE END letters to its disk."
  [state]
  (when-let [fb (core/end-fireball state)]
    (let [r (double (:radius fb 0.0))
          cx (double (:x fb))
          cy (double (:y fb))
          msg (or (core/end-message state) "THE END")
          max-r (double (:max-radius fb))]
      (when (pos? r)
        (q/no-stroke)
        (q/fill 255 120 30 160)
        (q/ellipse cx cy (* 2 r) (* 2 r))
        (q/fill 255 200 80 200)
        (q/ellipse cx cy r r)
        (when (pos? (core/end-message-reveal state))
          (draw-message! cx cy r max-r msg))))))

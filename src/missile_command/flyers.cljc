(ns missile-command.flyers
  "Pure flyer path motion and drop scheduling helpers."
  (:require [missile-command.missiles :as missiles]))

(defn path-length
  [flyer]
  (let [dx (- (double (:x1 flyer)) (double (:x0 flyer)))
        dy (- (double (:y1 flyer)) (double (:y0 flyer)))]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn position-at
  [flyer progress]
  (let [p (max 0.0 (min 1.0 (double progress)))]
    {:x (+ (double (:x0 flyer)) (* p (- (double (:x1 flyer)) (double (:x0 flyer)))))
     :y (+ (double (:y0 flyer)) (* p (- (double (:y1 flyer)) (double (:y0 flyer)))))}))

(defn advance
  "Advance flyer by dt. Returns :left when past the end of the path."
  [flyer dt]
  (let [len (path-length flyer)
        speed (double (:speed flyer))
        delta (if (zero? len) 1.0 (/ (* speed dt) len))
        progress (+ (double (:progress flyer 0.0)) delta)]
    (if (>= progress 1.0)
      :left
      (merge flyer
             {:progress progress}
             (position-at flyer progress)))))

(defn pending-drops
  [flyer progress]
  (let [fired (or (:drops-fired flyer) #{})]
    (filterv (fn [drop]
               (and (not (contains? fired (:id drop)))
                    (<= (double (:at-progress drop)) (double progress))))
             (or (:drops flyer) []))))

(defn hit-by-fireball?
  [flyer fireballs]
  (some #(missiles/point-in-fireball? % (:x flyer) (:y flyer)) fireballs))

(defn make
  [flyer-id flyer-kind start-x start-y end-x end-y speed]
  {:id flyer-id
   :kind (keyword flyer-kind)
   :x0 (double start-x)
   :y0 (double start-y)
   :x1 (double end-x)
   :y1 (double end-y)
   :speed (double speed)
   :progress 0.0
   :x (double start-x)
   :y (double start-y)
   :drops []
   :drops-fired #{}})

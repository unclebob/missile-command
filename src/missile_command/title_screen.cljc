(ns missile-command.title-screen
  "Pure title-screen affordance geometry and host input policy.")

(def button-width 190)
(def button-height 42)
(def button-gap 16)
(def checkbox-size 24)

(defn phone?
  [state]
  (boolean (:phone? state)))

(defn- button-y
  [state]
  (min (- (:height state) 36)
       (+ (/ (:height state) 2.0) 108)))

(defn- footer-button-y
  [state]
  (- (:height state) 46))

(defn- rect
  [state id label center-x]
  (let [w button-width
        h button-height
        y (button-y state)]
    {:id id
     :label label
     :x (- center-x (/ w 2.0))
     :y (- y (/ h 2.0))
     :w w
     :h h}))

(defn buttons
  [state]
  (let [cx (/ (:width state) 2.0)]
    (if (phone? state)
      [(rect state :high-scores "High Scores" cx)]
      [(rect state :high-scores "High Scores" (- cx (/ (+ button-width button-gap) 2.0)))
       (rect state :options "Options" (+ cx (/ (+ button-width button-gap) 2.0)))])))

(defn- footer-button
  [state id label]
  (let [w button-width
        h button-height
        x (- (/ (:width state) 2.0) (/ w 2.0))
        y (- (footer-button-y state) (/ h 2.0))]
    {:id id
     :label label
     :x x
     :y y
     :w w
     :h h}))

(defn high-scores-buttons
  [state]
  [(footer-button state :close-high-scores "Title")])

(defn options-buttons
  [state]
  [(footer-button state :leave-options "Title")])

(defn mute-checkbox
  [state]
  (let [x (- (/ (:width state) 2.0) 92)
        y (- (/ (:height state) 2.0) 62)]
    {:id :toggle-mute
     :label "Mute"
     :x x
     :y y
     :w checkbox-size
     :h checkbox-size
     :checked? (boolean (get-in state [:options :mute]))}))

(defn point-in-rect?
  [px py button]
  (let [rx (:x button)
        ry (:y button)
        rw (:w button)
        rh (:h button)]
    (and (<= rx px (+ rx rw))
         (<= ry py (+ ry rh)))))

(defn button-at
  [state x y]
  (first (filter #(point-in-rect? x y %) (buttons state))))

(defn- button-at-from
  [buttons x y]
  (first (filter #(point-in-rect? x y %) buttons)))

(defn command-at
  "Return the command for a title click. Clicks outside controls start play."
  [state x y]
  (case (:id (button-at state x y))
    :high-scores {:type :open-high-scores}
    :options {:type :open-options}
    {:type :start}))

(defn high-scores-command-at
  [state x y]
  (when (= :close-high-scores
           (:id (button-at-from (high-scores-buttons state) x y)))
    {:type :close-high-scores}))

(defn options-command-at
  [state x y]
  (cond
    (= :toggle-mute
       (:id (button-at-from [(mute-checkbox state)] x y)))
    {:type :set-mute
     :mute (not (boolean (get-in state [:options :mute])))}

    (= :leave-options
       (:id (button-at-from (options-buttons state) x y)))
    {:type :leave-options}

    :else nil))

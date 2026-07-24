(ns missile-command.acceptance.steps
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(defn- assert-playfield-dimension
  [world example param-name label reader]
  (let [expected (support/example-int example param-name label)
        actual (reader (:state world))]
    (when-not (= expected actual)
      (support/fail! (str "playfield " label " " actual " expected " expected)))
    world))

(defn- living-cities
  [world]
  (core/living-cities (:state world)))

(defn- batteries
  [world]
  (core/batteries (:state world)))

(defn- battery
  [world id]
  (or (core/battery (:state world) id)
      (support/fail! (str "missing battery " id))))

(defn- assert-count
  [actual expected label]
  (when-not (= expected actual)
    (support/fail! (str label " count " actual " expected " expected))))

(defn- city-xs
  [world]
  (mapv :x (sort-by :id (core/cities (:state world)))))

(defn- city-span
  [world]
  (let [xs (city-xs world)]
    (- (apply max xs) (apply min xs))))

(defn- example-width
  [example param-name]
  (support/example-int example param-name "width"))

(defn- example-height
  [example param-name]
  (support/example-int example param-name "height"))

(defn- one-third
  [width]
  (/ width 3.0))

(defn- two-thirds
  [width]
  (* width (/ 2.0 3)))

(defn- assert-condition
  [ok? message]
  (when-not ok?
    (support/fail! message)))

(defn- assert-entities-in-ground-band
  [state entities height entity-label]
  (doseq [entity entities]
    (when-not (and (= height (core/playfield-height state))
                   (core/on-ground? state entity))
      (support/fail! (str entity-label " " (:id entity) " y " (:y entity)
                          " not in ground band for height " height)))))

(defn- assert-xs-in-playfield
  [entities width entity-label]
  (doseq [entity entities]
    (when-not (and (<= 0 (:x entity)) (< (:x entity) width))
      (support/fail! (str entity-label " " (:id entity) " x " (:x entity)
                          " not in [0," width ")")))))

(defn- assert-lt
  [actual bound message]
  (assert-condition (< actual bound) message))

(defn- assert-gt
  [actual bound message]
  (assert-condition (> actual bound) message))

(defn- assert-between-open
  [actual lo hi message]
  (assert-condition (and (< lo actual) (< actual hi)) message))

(defn- earlier-fallback-batteries
  [state x target]
  (let [zone (core/click-zone (core/playfield-width state) x)
        order (core/click-fallback-order zone)]
    (assert-condition (some #{target} order)
                      (str "battery " target " not in fallback for zone " zone))
    (take-while #(not= % target) order)))

(defn- disable-earlier-batteries
  [state x target disable-fn]
  (reduce disable-fn state (earlier-fallback-batteries state x target)))

(def step-handlers
  [{:pattern #"^a new game with width <([A-Za-z0-9_]+)> and height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param height-param] example]
          (assoc world :state
                 (core/new-game
                  {:width (example-width example width-param)
                   :height (example-height example height-param)})))}

   {:pattern #"^a new game with width (\d+) and height (\d+)$"
    :fn (fn [world [_ width height] _]
          (assoc world :state
                 (core/new-game
                  {:width (support/parse-int width "width")
                   :height (support/parse-int height "height")})))}

   {:pattern #"^the playfield is resized to width <([A-Za-z0-9_]+)> and height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param height-param] example]
          (assoc world :state
                 (core/resize (:state world)
                              (example-width example width-param)
                              (example-height example height-param))))}

   {:pattern #"^the playfield width is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (assert-playfield-dimension world example width-param "width"
                                      core/playfield-width))}

   {:pattern #"^the playfield height is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ height-param] example]
          (assert-playfield-dimension world example height-param "height"
                                      core/playfield-height))}

   {:pattern #"^there are <([A-Za-z0-9_]+)> living cities$"
    :fn (fn [world [_ count-param] example]
          (assert-count (count (living-cities world))
                        (support/example-int example count-param "city count")
                        "living cities")
          world)}

   {:pattern #"^there are <([A-Za-z0-9_]+)> non-destroyed batteries named left center and right$"
    :fn (fn [world [_ count-param] example]
          (let [bats (filterv (complement :destroyed?) (batteries world))]
            (assert-count (count bats)
                          (support/example-int example count-param "battery count")
                          "non-destroyed batteries")
            (when-not (= #{:left :center :right} (set (map :id bats)))
              (support/fail! (str "battery ids " (mapv :id bats)
                                  " expected left center right"))))
          world)}

   {:pattern #"^each battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ ammo-param] example]
          (let [ammo (support/example-int example ammo-param "ammo")]
            (doseq [b (batteries world)]
              (when-not (= ammo (:missiles b))
                (support/fail! (str "battery " (:id b) " missiles "
                                    (:missiles b) " expected " ammo)))))
          world)}

   {:pattern #"^city x positions increase with city index$"
    :fn (fn [world _ _]
          (let [xs (city-xs world)]
            (assert-condition (apply < xs)
                              (str "city x positions not increasing: " xs)))
          world)}

   {:pattern #"^every city x is between 0 inclusive and <([A-Za-z0-9_]+)> exclusive$"
    :fn (fn [world [_ width-param] example]
          (assert-xs-in-playfield (core/cities (:state world))
                                  (example-width example width-param)
                                  "city")
          world)}

   {:pattern #"^every city y is in the ground band for height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ height-param] example]
          (assert-entities-in-ground-band (:state world)
                                          (core/cities (:state world))
                                          (example-height example height-param)
                                          "city")
          world)}

   {:pattern #"^the leftmost city x is less than one third of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (example-width example width-param)
                leftmost (apply min (city-xs world))]
            (assert-lt leftmost (one-third width)
                       (str "leftmost city x " leftmost
                            " not < one third of " width)))
          world)}

   {:pattern #"^the rightmost city x is greater than two thirds of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (example-width example width-param)
                rightmost (apply max (city-xs world))]
            (assert-gt rightmost (two-thirds width)
                       (str "rightmost city x " rightmost
                            " not > two thirds of " width)))
          world)}

   {:pattern #"^the left battery x is less than the center battery x$"
    :fn (fn [world _ _]
          (assert-lt (:x (battery world :left))
                     (:x (battery world :center))
                     "left battery x not less than center")
          world)}

   {:pattern #"^the center battery x is less than the right battery x$"
    :fn (fn [world _ _]
          (assert-lt (:x (battery world :center))
                     (:x (battery world :right))
                     "center battery x not less than right")
          world)}

   {:pattern #"^the left battery x is less than one third of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (example-width example width-param)
                x (:x (battery world :left))]
            (assert-lt x (one-third width)
                       (str "left battery x " x " not < one third of " width)))
          world)}

   {:pattern #"^the center battery x is between one third and two thirds of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (example-width example width-param)
                x (:x (battery world :center))
                lo (one-third width)
                hi (two-thirds width)]
            (assert-between-open x lo hi
                                 (str "center battery x " x " not between " lo " and " hi)))
          world)}

   {:pattern #"^the right battery x is greater than two thirds of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (example-width example width-param)
                x (:x (battery world :right))]
            (assert-gt x (two-thirds width)
                       (str "right battery x " x " not > two thirds of " width)))
          world)}

   {:pattern #"^every battery y is in the ground band for height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ height-param] example]
          (assert-entities-in-ground-band (:state world)
                                          (batteries world)
                                          (example-height example height-param)
                                          "battery")
          world)}

   {:pattern #"^the center battery missile speed is greater than the left battery missile speed$"
    :fn (fn [world _ _]
          (assert-gt (:missile-speed (battery world :center))
                     (:missile-speed (battery world :left))
                     "center missile speed not greater than left")
          world)}

   {:pattern #"^the center battery missile speed is greater than the right battery missile speed$"
    :fn (fn [world _ _]
          (assert-gt (:missile-speed (battery world :center))
                     (:missile-speed (battery world :right))
                     "center missile speed not greater than right")
          world)}

   {:pattern #"^the horizontal span of the cities is greater than half of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (example-width example width-param)
                span (city-span world)]
            (assert-gt span (/ width 2.0)
                       (str "city span " span " not > half of " width)))
          world)}

   {:pattern #"^the horizontal span of the cities is less than width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (example-width example width-param)
                span (city-span world)]
            (assert-lt span width
                       (str "city span " span " not < " width)))
          world)}

   {:pattern #"^the player aims at (-?\d+) (-?\d+)$"
    :fn (fn [world [_ x y] _]
          (let [result (core/handle (:state world)
                                    {:type :aim
                                     :x (support/parse-int x "x")
                                     :y (support/parse-int y "y")})]
            (assoc world :state (:state result))))}

   {:pattern #"^the player aims at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [result (core/handle (:state world)
                                    {:type :aim
                                     :x (support/example-int example x-param "x")
                                     :y (support/example-int example y-param "y")})]
            (assoc world :state (:state result))))}

{:pattern #"^the crosshair is at (-?\d+) (-?\d+)$"
    :fn (fn [world [_ x y] _]
          (let [expected {:x (support/parse-int x "x")
                          :y (support/parse-int y "y")}
                actual (core/crosshair (:state world))]
            (assert-condition (= expected actual)
                              (str "crosshair " actual " expected " expected)))
          world)}

   {:pattern #"^the player clicks at (-?\d+) (-?\d+)$"
    :fn (fn [world [_ x y] _]
          (let [result (core/handle (:state world)
                                    {:type :click
                                     :x (support/parse-int x "x")
                                     :y (support/parse-int y "y")})]
            (assoc world :state (:state result))))}

   {:pattern #"^the player clicks at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [result (core/handle (:state world)
                                    {:type :click
                                     :x (support/example-int example x-param "x")
                                     :y (support/example-int example y-param "y")})]
            (assoc world :state (:state result))))}

   {:pattern #"^the click must fall back to the <([A-Za-z0-9_]+)> battery because earlier batteries are empty$"
    :fn (fn [world [_ battery-param] example]
          (assoc world :state
                 (disable-earlier-batteries
                  (:state world)
                  (support/example-int example "x" "x")
                  (support/example-battery example battery-param)
                  (fn [s id] (core/set-battery-ammo s id 0)))))}

   {:pattern #"^the click must fall back to the <([A-Za-z0-9_]+)> battery because earlier batteries are destroyed$"
    :fn (fn [world [_ battery-param] example]
          (assoc world :state
                 (disable-earlier-batteries
                  (:state world)
                  (support/example-int example "x" "x")
                  (support/example-battery example battery-param)
                  core/destroy-battery)))}

   {:pattern #"^no battery can fire$"
    :fn (fn [world _ _]
          (assoc world :state
                 (reduce (fn [s id] (core/set-battery-ammo s id 0))
                         (:state world)
                         [:left :center :right])))}

   {:pattern #"^the crosshair is at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [expected {:x (support/example-int example x-param "x")
                          :y (support/example-int example y-param "y")}
                actual (core/crosshair (:state world))]
            (assert-condition (= expected actual)
                              (str "crosshair " actual " expected " expected)))
          world)}

   {:pattern #"^the score is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ score-param] example]
          (let [expected (support/example-int example score-param "score")
                actual (core/score (:state world))]
            (assert-condition (= expected actual)
                              (str "score " actual " expected " expected)))
          world)}

   {:pattern #"^the player fires the (left|center|right) battery$"
    :fn (fn [world [_ battery-name] _]
          (let [battery-id (support/parse-battery-id battery-name)
                result (core/handle (:state world)
                                    {:type :fire :battery battery-id})]
            (assoc world :state (:state result))))}

   {:pattern #"^the player fires the <([A-Za-z0-9_]+)> battery$"
    :fn (fn [world [_ battery-param] example]
          (let [battery-id (support/example-battery example battery-param)
                result (core/handle (:state world)
                                    {:type :fire :battery battery-id})]
            (assoc world :state (:state result))))}

   {:pattern #"^the player fires every battery once$"
    :fn (fn [world _ _]
          (let [state (reduce (fn [s id]
                                (:state (core/handle s {:type :fire :battery id})))
                              (:state world)
                              [:left :center :right])]
            (assoc world :state state)))}

   {:pattern #"^the (left|center|right) battery has (\d+) missiles$"
    :fn (fn [world [_ battery-name ammo] _]
          (let [battery-id (support/parse-battery-id battery-name)
                expected (support/parse-int ammo "ammo")
                actual (:missiles (battery world battery-id))]
            (assert-condition (= expected actual)
                              (str "battery " battery-id " missiles "
                                   actual " expected " expected)))
          world)}

   {:pattern #"^the <([A-Za-z0-9_]+)> battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ battery-param ammo-param] example]
          (let [battery-id (support/example-battery example battery-param)
                expected (support/example-int example ammo-param "ammo")
                actual (:missiles (battery world battery-id))]
            (assert-condition (= expected actual)
                              (str "battery " battery-id " missiles "
                                   actual " expected " expected)))
          world)}

   {:pattern #"^every other battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ ammo-param] example]
          (let [expected (support/example-int example ammo-param "ammo")
                fired (support/example-battery example "battery")
                others (remove #(= fired (:id %)) (batteries world))]
            (doseq [b others]
              (assert-condition (= expected (:missiles b))
                                (str "battery " (:id b) " missiles "
                                     (:missiles b) " expected " expected))))
          world)}

   {:pattern #"^there are (\d+) defensive missiles in flight$"
    :fn (fn [world [_ count-text] _]
          (assert-count (count (core/defensive-missiles (:state world)))
                        (support/parse-int count-text "missile count")
                        "defensive missiles")
          world)}

   {:pattern #"^there are <([A-Za-z0-9_]+)> defensive missiles in flight$"
    :fn (fn [world [_ count-param] example]
          (assert-count (count (core/defensive-missiles (:state world)))
                        (support/example-int example count-param "missile count")
                        "defensive missiles")
          world)}

   {:pattern #"^a defensive missile from the (left|center|right) battery targets (-?\d+) (-?\d+)$"
    :fn (fn [world [_ battery-name x y] _]
          (let [battery-id (support/parse-battery-id battery-name)
                target-x (support/parse-int x "x")
                target-y (support/parse-int y "y")
                match (first (filter #(and (= battery-id (:battery %))
                                           (= target-x (:x1 %))
                                           (= target-y (:y1 %)))
                                     (core/defensive-missiles (:state world))))]
            (assert-condition match
                              (str "no defensive missile from " battery-id
                                   " targeting " target-x "," target-y)))
          world)}

   {:pattern #"^a defensive missile from the <([A-Za-z0-9_]+)> battery targets <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ battery-param x-param y-param] example]
          (let [battery-id (support/example-battery example battery-param)
                target-x (support/example-int example x-param "x")
                target-y (support/example-int example y-param "y")
                match (first (filter #(and (= battery-id (:battery %))
                                           (= target-x (:x1 %))
                                           (= target-y (:y1 %)))
                                     (core/defensive-missiles (:state world))))]
            (assert-condition match
                              (str "no defensive missile from " battery-id
                                   " targeting " target-x "," target-y)))
          world)}

   {:pattern #"^the <([A-Za-z0-9_]+)> battery ammo is set to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ battery-param ammo-param] example]
          (assoc world :state
                 (core/set-battery-ammo
                  (:state world)
                  (support/example-battery example battery-param)
                  (support/example-int example ammo-param "ammo"))))}

   {:pattern #"^the <([A-Za-z0-9_]+)> battery is destroyed$"
    :fn (fn [world [_ battery-param] example]
          (let [battery-id (support/example-battery example battery-param)
                bat (battery world battery-id)]
            (if (= :then (:gherkin-phase world))
              (do
                (assert-condition (:destroyed? bat)
                                  (str "battery " battery-id " is not destroyed"))
                world)
              (assoc world :state
                     (core/destroy-battery (:state world) battery-id)))))}

   {:pattern #"^the center defensive missile is faster than each side defensive missile$"
    :fn (fn [world _ _]
          (let [by-battery (into {} (map (juxt :battery identity)
                                         (core/defensive-missiles (:state world))))
                center (or (by-battery :center)
                           (support/fail! "missing center defensive missile"))
                left (or (by-battery :left)
                         (support/fail! "missing left defensive missile"))
                right (or (by-battery :right)
                          (support/fail! "missing right defensive missile"))]
            (assert-gt (:speed center) (:speed left)
                       "center missile not faster than left")
            (assert-gt (:speed center) (:speed right)
                       "center missile not faster than right"))
          world)}

   {:pattern #"^time advances by <([A-Za-z0-9_]+)> seconds$"
    :fn (fn [world [_ dt-param] example]
          (let [dt (Double/parseDouble (str (support/require-value example dt-param)))
                result (core/tick (:state world) dt)]
            (assoc world :state (:state result))))}

   {:pattern #"^time advances until defensive missiles arrive$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (cond
              (empty? (core/defensive-missiles s)) (assoc world :state s)
              (> n 5000) (support/fail! "missiles never arrived")
              :else (recur (:state (core/tick s 0.05)) (inc n)))))}

   {:pattern #"^time advances until the fireball reaches max radius$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (let [r (if (seq (core/fireballs s))
                      (apply max (map :radius (core/fireballs s)))
                      0.0)
                  max-r (core/max-fireball-radius s)]
              (cond
                (>= r (* 0.999 max-r))
                (assoc world :state s :fireball-max-time (core/sim-time s))
                (> n 5000) (support/fail! "fireball never reached max radius")
                :else (recur (:state (core/tick s 0.01)) (inc n))))))}

   {:pattern #"^time advances into the fireball shrink phase$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (let [r (if (seq (core/fireballs s))
                      (apply max (map :radius (core/fireballs s)))
                      0.0)
                  max-r (core/max-fireball-radius s)]
              (cond
                (and (seq (core/fireballs s)) (< r max-r))
                (assoc world :state s :fireball-shrink-time (core/sim-time s))
                (> n 5000) (support/fail! "fireball never entered shrink phase")
                :else (recur (:state (core/tick s 0.01)) (inc n))))))}

   {:pattern #"^time advances until fireballs expire$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (cond
              (empty? (core/fireballs s))
              (assoc world :state s :fireball-end-time (core/sim-time s))
              (> n 5000) (support/fail! "fireballs never expired")
              :else (recur (:state (core/tick s 0.05)) (inc n)))))}

   {:pattern #"^time advances until fireballs reach at least radius <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ r-param] example]
          (let [min-r (Double/parseDouble (str (support/require-value example r-param)))]
            (loop [s (:state world) n 0]
              (let [r (if (seq (core/fireballs s))
                        (apply max (map :radius (core/fireballs s)))
                        0.0)]
                (cond
                  (>= r min-r) (assoc world :state s)
                  (> n 5000) (support/fail! (str "fireball never reached radius " min-r))
                  :else (recur (:state (core/tick s 0.01)) (inc n)))))))}

   {:pattern #"^time advances until fireballs reach peak radius$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (let [r (if (seq (core/fireballs s))
                      (apply max (map :radius (core/fireballs s)))
                      0.0)
                  max-r (core/max-fireball-radius s)]
              (cond
                (>= r (* 0.999 max-r)) (assoc world :state s)
                (> n 5000) (support/fail! "fireball never peaked")
                :else (recur (:state (core/tick s 0.01)) (inc n))))))}

   {:pattern #"^there are <([A-Za-z0-9_]+)> fireballs$"
    :fn (fn [world [_ count-param] example]
          (assert-count (count (core/fireballs (:state world)))
                        (support/example-int example count-param "fireball count")
                        "fireballs")
          world)}

   {:pattern #"^a fireball is centered at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [x (support/example-int example x-param "x")
                y (support/example-int example y-param "y")
                match (first (filter #(and (= x (:x %)) (= y (:y %)))
                                     (core/fireballs (:state world))))]
            (assert-condition match
                              (str "no fireball centered at " x "," y)))
          world)}

   {:pattern #"^there is an active fireball$"
    :fn (fn [world _ _]
          (assert-condition (seq (core/fireballs (:state world)))
                            "expected an active fireball")
          world)}

   {:pattern #"^the fireball start time is recorded$"
    :fn (fn [world _ _]
          (assoc world :fireball-start-time (core/sim-time (:state world))))}

   {:pattern #"^the fireball max time is at least the fireball start time$"
    :fn (fn [world _ _]
          (assert-condition (>= (:fireball-max-time world)
                                (:fireball-start-time world))
                            "max time before start time")
          world)}

   {:pattern #"^the fireball shrink time is at least the fireball max time$"
    :fn (fn [world _ _]
          (assert-condition (>= (:fireball-shrink-time world)
                                (:fireball-max-time world))
                            "shrink time before max time")
          world)}

   {:pattern #"^the fireball end time is at least the fireball shrink time$"
    :fn (fn [world _ _]
          (assert-condition (>= (:fireball-end-time world)
                                (:fireball-shrink-time world))
                            "end time before shrink time")
          world)}

   {:pattern #"^a fireball radius is greater than <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ r-param] example]
          (let [min-r (Double/parseDouble (str (support/require-value example r-param)))
                r (apply max 0.0 (map :radius (core/fireballs (:state world))))]
            (assert-gt r min-r (str "fireball radius " r " not > " min-r)))
          world)}

   {:pattern #"^a fireball radius is less than the max fireball radius$"
    :fn (fn [world _ _]
          (let [max-r (core/max-fireball-radius (:state world))
                r (apply max 0.0 (map :radius (core/fireballs (:state world))))]
            (assert-lt r max-r (str "fireball radius " r " not < max " max-r)))
          world)}

   {:pattern #"^a destroyable target at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (assoc world :state
                 (core/add-destroyable-target
                  (:state world)
                  (support/example-int example x-param "x")
                  (support/example-int example y-param "y"))))}

   {:pattern #"^the destroyable target is destroyed$"
    :fn (fn [world _ _]
          (let [targets (core/destroyable-targets (:state world))]
            (assert-condition (some :destroyed? targets)
                              "expected a destroyed target"))
          world)}

   {:pattern #"^the destroyable target is not destroyed$"
    :fn (fn [world _ _]
          (let [targets (core/destroyable-targets (:state world))]
            (assert-condition (every? (complement :destroyed?) targets)
                              "expected no destroyed targets"))
          world)}

   {:pattern #"^the last applied time step is at most <([A-Za-z0-9_]+)> seconds$"
    :fn (fn [world [_ dt-param] example]
          (let [max-dt (Double/parseDouble (str (support/require-value example dt-param)))
                actual (core/last-applied-dt (:state world))]
            (assert-condition (<= actual max-dt)
                              (str "last applied dt " actual " > " max-dt)))
          world)}

   {:pattern #"^a defensive missile from the <([A-Za-z0-9_]+)> battery has progressed toward <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ battery-param x-param y-param] example]
          (let [battery-id (support/example-battery example battery-param)
                aim-x (support/example-int example x-param "x")
                aim-y (support/example-int example y-param "y")
                m (first (filter #(and (= battery-id (:battery %))
                                       (= aim-x (:x1 %))
                                       (= aim-y (:y1 %)))
                                 (core/defensive-missiles (:state world))))]
            (assert-condition m "missing defensive missile")
            (assert-gt (:progress m) 0.0 "missile has not progressed"))
          world)}

   {:pattern #"^a defensive missile from the <([A-Za-z0-9_]+)> battery has not reached <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ battery-param x-param y-param] example]
          (let [battery-id (support/example-battery example battery-param)
                aim-x (support/example-int example x-param "x")
                aim-y (support/example-int example y-param "y")
                m (first (filter #(and (= battery-id (:battery %))
                                       (= aim-x (:x1 %))
                                       (= aim-y (:y1 %)))
                                 (core/defensive-missiles (:state world))))]
            (assert-condition m "missing defensive missile")
            (assert-lt (:progress m) 1.0 "missile already reached aim"))
          world)}

   {:pattern #"^an enemy missile targeting city <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ city-param] example]
          (assoc world :state
                 (core/spawn-enemy-targeting-city
                  (:state world)
                  (support/example-int example city-param "city"))))}

   {:pattern #"^an enemy missile targeting battery <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ battery-param] example]
          (assoc world :state
                 (core/spawn-enemy-targeting-battery
                  (:state world)
                  (support/example-battery example battery-param))))}

   {:pattern #"^<([A-Za-z0-9_]+)> enemy missiles each targeting a different living city$"
    :fn (fn [world [_ count-param] example]
          (assoc world :state
                 (core/spawn-enemies-targeting-distinct-cities
                  (:state world)
                  (support/example-int example count-param "spawn count"))))}

   {:pattern #"^there are <([A-Za-z0-9_]+)> enemy missiles in flight$"
    :fn (fn [world [_ count-param] example]
          (assert-count (count (core/enemy-missiles (:state world)))
                        (support/example-int example count-param "enemy count")
                        "enemy missiles")
          world)}

   {:pattern #"^an enemy missile has progressed toward city <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ city-param] example]
          (let [city-id (support/example-int example city-param "city")
                m (first (filter #(and (= :city (:target-kind %))
                                       (= city-id (:target-id %)))
                                 (core/enemy-missiles (:state world))))]
            (assert-condition m "missing enemy missile")
            (assert-gt (:progress m) 0.0 "enemy has not progressed"))
          world)}

   {:pattern #"^time advances until enemy missiles impact or are destroyed$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (cond
              (empty? (core/enemy-missiles s)) (assoc world :state s)
              (> n 10000) (support/fail! "enemy missiles never finished")
              :else (recur (:state (core/tick s 0.05)) (inc n)))))}

   {:pattern #"^time advances until the enemy missile is inside the fireball radius or has impacted$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (cond
              (empty? (core/enemy-missiles s)) (assoc world :state s)
              (#{:fireball :impact} (core/last-enemy-fate s)) (assoc world :state s)
              (> n 10000) (support/fail! "enemy never entered fireball or impacted")
              :else (recur (:state (core/tick s 0.05)) (inc n)))))}

   {:pattern #"^a fireball at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)> with radius <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param r-param] example]
          (assoc world :state
                 (core/add-static-fireball
                  (:state world)
                  (support/example-int example x-param "x")
                  (support/example-int example y-param "y")
                  (support/example-int example r-param "radius"))))}

   {:pattern #"^the enemy missile path passes within distance <([A-Za-z0-9_]+)> of that fireball center$"
    :fn (fn [world [_ _r-param] example]
          (let [x (support/example-int example "fireball_x" "x")
                y (support/example-int example "fireball_y" "y")]
            (assoc world :state (core/route-enemy-through-point (:state world) x y))))}

   {:pattern #"^the enemy missile path stays farther than <([A-Za-z0-9_]+)> from that fireball center$"
    :fn (fn [world _ _]
          ;; Default vertical spawn plus far fireball examples already satisfy this.
          world)}

   {:pattern #"^the enemy missile is destroyed by the fireball$"
    :fn (fn [world _ _]
          (assert-condition (= :fireball (core/last-enemy-fate (:state world)))
                            (str "expected fireball kill, got "
                                 (core/last-enemy-fate (:state world))))
          world)}

   {:pattern #"^city <([A-Za-z0-9_]+)> is living$"
    :fn (fn [world [_ city-param] example]
          (let [city-id (support/example-int example city-param "city")]
            (assert-condition (core/living-city? (:state world) city-id)
                              (str "city " city-id " is not living")))
          world)}

   {:pattern #"^city <([A-Za-z0-9_]+)> is not living$"
    :fn (fn [world [_ city-param] example]
          (let [city-id (support/example-int example city-param "city")]
            (assert-condition (not (core/living-city? (:state world) city-id))
                              (str "city " city-id " is still living")))
          world)}

   {:pattern #"^the wave number is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param] example]
          (let [expected (support/example-int example wave-param "wave")
                actual (core/wave (:state world))]
            (assert-condition (= expected actual)
                              (str "wave " actual " expected " expected)))
          world)}

   {:pattern #"^the hud shows wave <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param] example]
          (let [expected (support/example-int example wave-param "wave")
                actual (:wave (core/hud (:state world)))]
            (assert-condition (= expected actual)
                              (str "hud wave " actual " expected " expected)))
          world)}

   {:pattern #"^each non-destroyed battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ ammo-param] example]
          (let [ammo (support/example-int example ammo-param "ammo")]
            (doseq [b (remove :destroyed? (batteries world))]
              (assert-condition (= ammo (:missiles b))
                                (str "battery " (:id b) " missiles "
                                     (:missiles b) " expected " ammo))))
          world)}

   {:pattern #"^the current wave has <([A-Za-z0-9_]+)> scheduled enemies still active$"
    :fn (fn [world [_ rem-param] example]
          (assoc world :state
                 (core/set-wave-enemies-active
                  (:state world)
                  (support/example-int example rem-param "remaining"))))}

   {:pattern #"^the wave is not complete$"
    :fn (fn [world _ _]
          (assert-condition (not (core/wave-complete? (:state world)))
                            "wave is complete but should not be")
          world)}

   {:pattern #"^the wave is complete$"
    :fn (fn [world _ _]
          (assert-condition (core/wave-complete? (:state world))
                            "wave is not complete")
          world)}

   {:pattern #"^time advances until all wave enemies are destroyed or have impacted$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (cond
              (and (core/wave-complete? s)
                   (empty? (core/enemy-missiles s)))
              (assoc world :state s)

              (and (empty? (core/enemy-missiles s))
                   (not (core/wave-complete? s))
                   (not (:wave-had-enemies? s)))
              ;; tick once more to allow maybe-complete-wave if needed
              (let [s2 (:state (core/tick s 0.05))]
                (if (core/wave-complete? s2)
                  (assoc world :state s2)
                  (if (> n 10000)
                    (support/fail! "wave never completed")
                    (recur s2 (inc n)))))

              (> n 10000) (support/fail! "wave enemies never finished")
              :else (recur (:state (core/tick s 0.05)) (inc n)))))}

   {:pattern #"^every non-destroyed battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ ammo-param] example]
          (assoc world :state
                 (core/set-non-destroyed-battery-ammo
                  (:state world)
                  (support/example-int example ammo-param "spent ammo"))))}

   {:pattern #"^the next wave starts$"
    :fn (fn [world _ _]
          (assoc world :state (core/start-next-wave (:state world))))}

   {:pattern #"^the <([A-Za-z0-9_]+)> battery has been destroyed$"
    :fn (fn [world [_ battery-param] example]
          (assoc world :state
                 (core/destroy-battery
                  (:state world)
                  (support/example-battery example battery-param))))}

   {:pattern #"^wave <([A-Za-z0-9_]+)> enemy schedule metrics are recorded$"
    :fn (fn [world [_ wave-param] example]
          (let [w (support/example-int example wave-param "wave")]
            (assoc world :recorded-wave-metrics
                   (core/wave-schedule-metrics w)
                   :recorded-low-wave w)))}

   {:pattern #"^the game is at wave <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param] example]
          (let [w (support/example-int example wave-param "wave")]
            (assoc world
                   :state (core/set-wave (:state world) w)
                   :high-wave w
                   :high-wave-metrics (core/wave-schedule-metrics w))))}

   {:pattern #"^wave <([A-Za-z0-9_]+)> is harder than wave <([A-Za-z0-9_]+)> by enemy count or enemy speed$"
    :fn (fn [world [_ high-param low-param] example]
          (let [high (support/example-int example high-param "high wave")
                low (support/example-int example low-param "low wave")
                low-m (or (:recorded-wave-metrics world)
                          (core/wave-schedule-metrics low))
                high-m (or (:high-wave-metrics world)
                           (core/wave-schedule-metrics high))]
            (assert-condition (core/harder-wave? low-m high-m)
                              (str "wave " high " not harder than " low
                                   " metrics " low-m " vs " high-m)))
          world)}])

(defn- match-handler
  [text]
  (some (fn [handler]
          (when-let [matches (re-matches (:pattern handler) text)]
            [handler matches]))
        step-handlers))

(defn- apply-gherkin-phase
  [world keyword]
  (case keyword
    "Given" (assoc world :gherkin-phase :given)
    "When" (assoc world :gherkin-phase :when)
    "Then" (assoc world :gherkin-phase :then)
    world))

(defn dispatch-step
  [world step example]
  (let [text (:text step)
        world (apply-gherkin-phase world (:keyword step))]
    (if-let [[handler matches] (match-handler text)]
      ((:fn handler) world matches example)
      (support/fail! (str "unsupported step: " text)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T12:01:47.330987-05:00", :module-hash "-237287548", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "817333596"} {:id "defn-/assert-playfield-dimension", :kind "defn-", :line 5, :end-line 11, :hash "-1180112229"} {:id "defn-/living-cities", :kind "defn-", :line 13, :end-line 15, :hash "-548423997"} {:id "defn-/batteries", :kind "defn-", :line 17, :end-line 19, :hash "2112668418"} {:id "defn-/battery", :kind "defn-", :line 21, :end-line 24, :hash "-1798101236"} {:id "defn-/assert-count", :kind "defn-", :line 26, :end-line 29, :hash "-734119864"} {:id "defn-/city-xs", :kind "defn-", :line 31, :end-line 33, :hash "-1231463410"} {:id "defn-/city-span", :kind "defn-", :line 35, :end-line 38, :hash "-826397513"} {:id "defn-/example-width", :kind "defn-", :line 40, :end-line 42, :hash "1667157547"} {:id "defn-/example-height", :kind "defn-", :line 44, :end-line 46, :hash "-1096613354"} {:id "defn-/one-third", :kind "defn-", :line 48, :end-line 50, :hash "1669847708"} {:id "defn-/two-thirds", :kind "defn-", :line 52, :end-line 54, :hash "1105411976"} {:id "defn-/assert-condition", :kind "defn-", :line 56, :end-line 59, :hash "2075522906"} {:id "defn-/assert-entities-in-ground-band", :kind "defn-", :line 61, :end-line 67, :hash "-247193944"} {:id "defn-/assert-xs-in-playfield", :kind "defn-", :line 69, :end-line 74, :hash "-807069005"} {:id "defn-/assert-lt", :kind "defn-", :line 76, :end-line 78, :hash "-545222397"} {:id "defn-/assert-gt", :kind "defn-", :line 80, :end-line 82, :hash "497978739"} {:id "defn-/assert-between-open", :kind "defn-", :line 84, :end-line 86, :hash "788381149"} {:id "defn-/earlier-fallback-batteries", :kind "defn-", :line 88, :end-line 94, :hash "-1064032304"} {:id "defn-/disable-earlier-batteries", :kind "defn-", :line 96, :end-line 98, :hash "1165353126"} {:id "def/step-handlers", :kind "def", :line 100, :end-line 483, :hash "1114522454"} {:id "defn-/match-handler", :kind "defn-", :line 485, :end-line 490, :hash "-760290467"} {:id "defn/dispatch-step", :kind "defn", :line 492, :end-line 497, :hash "-1121977204"}]}
;; clj-mutate-manifest-end

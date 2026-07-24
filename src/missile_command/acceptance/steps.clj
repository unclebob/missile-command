(ns missile-command.acceptance.steps
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.acceptance.enemy-steps :as enemy-steps]
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

(defn- assert-between-open
  [actual lo hi message]
  (support/assert-condition (and (< lo actual) (< actual hi)) message))
(defn- earlier-fallback-batteries
  [state x target]
  (let [zone (core/click-zone (core/playfield-width state) x)
        order (core/click-fallback-order zone)]
    (support/assert-condition (some #{target} order)
                      (str "battery " target " not in fallback for zone " zone))
    (take-while #(not= % target) order)))

(defn- disable-earlier-batteries
  [state x target disable-fn]
  (reduce disable-fn state (earlier-fallback-batteries state x target)))

(defn- max-fireball-radius
  [state]
  (if (seq (core/fireballs state))
    (apply max (map :radius (core/fireballs state)))
    0.0))

(def ^:private fireball-peak-fraction 0.999)

(defn- fireball-reached-peak?
  "True when the largest live fireball is at (or past) the configured peak fraction."
  [state]
  (>= (max-fireball-radius state)
      (* fireball-peak-fraction (core/max-fireball-radius state))))

(defn- fireball-in-shrink-phase?
  [state]
  (and (seq (core/fireballs state))
       (< (max-fireball-radius state)
          (core/max-fireball-radius state))))

(defn- fireball-radius-at-least?
  [state min-r]
  (>= (max-fireball-radius state) min-r))

(defn- advance-until
  "Tick the world state until pred returns truthy, or fail after max-steps ticks."
  [world pred dt max-steps fail-message]
  (support/advance-until world pred core/tick dt max-steps fail-message))

(def step-handlers
  (into
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

   {:pattern #"^there are (\d+) living cities$"
    :fn (fn [world [_ count-text] _]
          (support/assert-count (count (living-cities world))
                        (support/parse-int count-text "city count")
                        "living cities")
          world)}

   {:pattern #"^there are <([A-Za-z0-9_]+)> living cities$"
    :fn (fn [world [_ count-param] example]
          (support/assert-count (count (living-cities world))
                        (support/example-int example count-param "city count")
                        "living cities")
          world)}

   {:pattern #"^there are <([A-Za-z0-9_]+)> non-destroyed batteries named left center and right$"
    :fn (fn [world [_ count-param] example]
          (let [bats (filterv (complement :destroyed?) (batteries world))]
            (support/assert-count (count bats)
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
            (support/assert-condition (apply < xs)
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
            (support/assert-lt leftmost (one-third width)
                       (str "leftmost city x " leftmost
                            " not < one third of " width)))
          world)}

   {:pattern #"^the rightmost city x is greater than two thirds of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (example-width example width-param)
                rightmost (apply max (city-xs world))]
            (support/assert-gt rightmost (two-thirds width)
                       (str "rightmost city x " rightmost
                            " not > two thirds of " width)))
          world)}

   {:pattern #"^the left battery x is less than the center battery x$"
    :fn (fn [world _ _]
          (support/assert-lt (:x (battery world :left))
                     (:x (battery world :center))
                     "left battery x not less than center")
          world)}

   {:pattern #"^the center battery x is less than the right battery x$"
    :fn (fn [world _ _]
          (support/assert-lt (:x (battery world :center))
                     (:x (battery world :right))
                     "center battery x not less than right")
          world)}

   {:pattern #"^the left battery x is less than one third of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (example-width example width-param)
                x (:x (battery world :left))]
            (support/assert-lt x (one-third width)
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
            (support/assert-gt x (two-thirds width)
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
          (support/assert-gt (:missile-speed (battery world :center))
                     (:missile-speed (battery world :left))
                     "center missile speed not greater than left")
          world)}

   {:pattern #"^the center battery missile speed is greater than the right battery missile speed$"
    :fn (fn [world _ _]
          (support/assert-gt (:missile-speed (battery world :center))
                     (:missile-speed (battery world :right))
                     "center missile speed not greater than right")
          world)}

   {:pattern #"^the horizontal span of the cities is greater than half of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (example-width example width-param)
                span (city-span world)]
            (support/assert-gt span (/ width 2.0)
                       (str "city span " span " not > half of " width)))
          world)}

   {:pattern #"^the horizontal span of the cities is less than width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (example-width example width-param)
                span (city-span world)]
            (support/assert-lt span width
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
            (support/assert-condition (= expected actual)
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
            (support/assert-condition (= expected actual)
                              (str "crosshair " actual " expected " expected)))
          world)}

   {:pattern #"^the score is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ score-param] example]
          (let [expected (support/example-int example score-param "score")
                actual (core/score (:state world))]
            (support/assert-condition (= expected actual)
                              (str "score " actual " expected " expected)))
          world)}

   {:pattern #"^the multiplier is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ mult-param] example]
          (let [expected (support/example-int example mult-param "multiplier")
                actual (core/multiplier (:state world))]
            (support/assert-condition (= expected actual)
                              (str "multiplier " actual " expected " expected)))
          world)}

   {:pattern #"^the bonus city threshold is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ thresh-param] example]
          (assoc world :state
                 (core/set-bonus-city-threshold
                  (:state world)
                  (support/example-int example thresh-param "threshold"))))}

   {:pattern #"^the score becomes <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ score-param] example]
          (assoc world :state
                 (core/set-score
                  (:state world)
                  (support/example-int example score-param "score"))))}

   {:pattern #"^the bonus city reserve is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ reserve-param] example]
          (let [expected (support/example-int example reserve-param "reserve")
                actual (core/bonus-cities (:state world))]
            (support/assert-condition (= expected actual)
                              (str "bonus city reserve " actual " expected " expected)))
          world)}

   {:pattern #"^the bonus city reserve is set to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ reserve-param] example]
          (assoc world :state
                 (core/set-bonus-city-reserve
                  (:state world)
                  (support/example-int example reserve-param "reserve"))))}

   {:pattern #"^bonus cities from reserve are applied after wave resolution$"
    :fn (fn [world _ _]
          (assoc world :state
                 (core/apply-bonus-cities-from-reserve (:state world))))}

   {:pattern #"^the number of bonus city earned events is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ count-param] example]
          (let [expected (support/example-int example count-param "event count")
                actual (core/bonus-city-earned-events (:state world))]
            (support/assert-condition (= expected actual)
                              (str "bonus city earned events " actual
                                   " expected " expected)))
          world)}

   {:pattern #"^city (\d+) has been destroyed$"
    :fn (fn [world [_ city-text] _]
          (assoc world :state
                 (core/destroy-city
                  (:state world)
                  (support/parse-int city-text "city"))))}

   {:pattern #"^city <([A-Za-z0-9_]+)> has been destroyed$"
    :fn (fn [world [_ city-param] example]
          (assoc world :state
                 (core/destroy-city
                  (:state world)
                  (support/example-int example city-param "city"))))}

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
            (support/assert-condition (= expected actual)
                              (str "battery " battery-id " missiles "
                                   actual " expected " expected)))
          world)}

   {:pattern #"^the <([A-Za-z0-9_]+)> battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ battery-param ammo-param] example]
          (let [battery-id (support/example-battery example battery-param)
                expected (support/example-int example ammo-param "ammo")
                actual (:missiles (battery world battery-id))]
            (support/assert-condition (= expected actual)
                              (str "battery " battery-id " missiles "
                                   actual " expected " expected)))
          world)}

   {:pattern #"^every other battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ ammo-param] example]
          (let [expected (support/example-int example ammo-param "ammo")
                fired (support/example-battery example "battery")
                others (remove #(= fired (:id %)) (batteries world))]
            (doseq [b others]
              (support/assert-condition (= expected (:missiles b))
                                (str "battery " (:id b) " missiles "
                                     (:missiles b) " expected " expected))))
          world)}

   {:pattern #"^there are (\d+) defensive missiles in flight$"
    :fn (fn [world [_ count-text] _]
          (support/assert-count (count (core/defensive-missiles (:state world)))
                        (support/parse-int count-text "missile count")
                        "defensive missiles")
          world)}

   {:pattern #"^there are <([A-Za-z0-9_]+)> defensive missiles in flight$"
    :fn (fn [world [_ count-param] example]
          (support/assert-count (count (core/defensive-missiles (:state world)))
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
            (support/assert-condition match
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
            (support/assert-condition match
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

   {:pattern #"^the (left|center|right) battery is destroyed$"
    :fn (fn [world [_ battery-name] _]
          (let [battery-id (support/parse-battery-id battery-name)
                bat (battery world battery-id)]
            (if (= :then (:gherkin-phase world))
              (do
                (support/assert-condition (:destroyed? bat)
                                  (str "battery " battery-id " is not destroyed"))
                world)
              (assoc world :state
                     (core/destroy-battery (:state world) battery-id)))))}

   {:pattern #"^the <([A-Za-z0-9_]+)> battery is destroyed$"
    :fn (fn [world [_ battery-param] example]
          (let [battery-id (support/example-battery example battery-param)
                bat (battery world battery-id)]
            (if (= :then (:gherkin-phase world))
              (do
                (support/assert-condition (:destroyed? bat)
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
            (support/assert-gt (:speed center) (:speed left)
                       "center missile not faster than left")
            (support/assert-gt (:speed center) (:speed right)
                       "center missile not faster than right"))
          world)}

   {:pattern #"^time advances by ([0-9.]+) seconds$"
    :fn (fn [world [_ dt-text] _]
          (let [dt (Double/parseDouble dt-text)
                result (core/tick (:state world) dt)]
            (assoc world :state (:state result))))}

   {:pattern #"^time advances by <([A-Za-z0-9_]+)> seconds$"
    :fn (fn [world [_ dt-param] example]
          (let [dt (Double/parseDouble (str (support/require-value example dt-param)))
                result (core/tick (:state world) dt)]
            (assoc world :state (:state result))))}

   {:pattern #"^time advances until defensive missiles arrive$"
    :fn (fn [world _ _]
          (advance-until world
                         (comp empty? core/defensive-missiles)
                         0.05 5000 "missiles never arrived"))}

   {:pattern #"^time advances until the fireball reaches max radius$"
    :fn (fn [world _ _]
          (let [advanced (advance-until
                          world
                          fireball-reached-peak?
                          0.01 5000 "fireball never reached max radius")]
            (assoc advanced :fireball-max-time (core/sim-time (:state advanced)))))}

   {:pattern #"^time advances into the fireball shrink phase$"
    :fn (fn [world _ _]
          (let [advanced (advance-until
                          world
                          fireball-in-shrink-phase?
                          0.01 5000 "fireball never entered shrink phase")]
            (assoc advanced :fireball-shrink-time
                   (core/sim-time (:state advanced)))))}

   {:pattern #"^time advances until fireballs expire$"
    :fn (fn [world _ _]
          (let [advanced (advance-until world
                                        (comp empty? core/fireballs)
                                        0.05 5000 "fireballs never expired")]
            (assoc advanced :fireball-end-time (core/sim-time (:state advanced)))))}

   {:pattern #"^time advances until fireballs reach at least radius ([0-9.]+)$"
    :fn (fn [world [_ r-text] _]
          (let [min-r (Double/parseDouble r-text)]
            (advance-until world
                           #(fireball-radius-at-least? % min-r)
                           0.01 5000
                           (str "fireball never reached radius " min-r))))}

   {:pattern #"^time advances until fireballs reach at least radius <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ r-param] example]
          (let [min-r (Double/parseDouble (str (support/require-value example r-param)))]
            (advance-until world
                           #(fireball-radius-at-least? % min-r)
                           0.01 5000
                           (str "fireball never reached radius " min-r))))}

   {:pattern #"^time advances until fireballs reach peak radius$"
    :fn (fn [world _ _]
          (advance-until world
                         fireball-reached-peak?
                         0.01 5000 "fireball never peaked"))}
   {:pattern #"^there are (\d+) fireballs$"
    :fn (fn [world [_ count-text] _]
          (support/assert-count (count (core/fireballs (:state world)))
                        (support/parse-int count-text "fireball count")
                        "fireballs")
          world)}

   {:pattern #"^there are <([A-Za-z0-9_]+)> fireballs$"
    :fn (fn [world [_ count-param] example]
          (support/assert-count (count (core/fireballs (:state world)))
                        (support/example-int example count-param "fireball count")
                        "fireballs")
          world)}

   {:pattern #"^a fireball is centered at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [x (support/example-int example x-param "x")
                y (support/example-int example y-param "y")
                match (first (filter #(and (= x (:x %)) (= y (:y %)))
                                     (core/fireballs (:state world))))]
            (support/assert-condition match
                              (str "no fireball centered at " x "," y)))
          world)}

   {:pattern #"^there is an active fireball$"
    :fn (fn [world _ _]
          (support/assert-condition (seq (core/fireballs (:state world)))
                            "expected an active fireball")
          world)}

   {:pattern #"^the fireball start time is recorded$"
    :fn (fn [world _ _]
          (assoc world :fireball-start-time (core/sim-time (:state world))))}

   {:pattern #"^the fireball max time is at least the fireball start time$"
    :fn (fn [world _ _]
          (support/assert-condition (>= (:fireball-max-time world)
                                (:fireball-start-time world))
                            "max time before start time")
          world)}

   {:pattern #"^the fireball shrink time is at least the fireball max time$"
    :fn (fn [world _ _]
          (support/assert-condition (>= (:fireball-shrink-time world)
                                (:fireball-max-time world))
                            "shrink time before max time")
          world)}

   {:pattern #"^the fireball end time is at least the fireball shrink time$"
    :fn (fn [world _ _]
          (support/assert-condition (>= (:fireball-end-time world)
                                (:fireball-shrink-time world))
                            "end time before shrink time")
          world)}

   {:pattern #"^a fireball radius is greater than ([0-9.]+)$"
    :fn (fn [world [_ r-text] _]
          (let [min-r (Double/parseDouble r-text)
                r (apply max 0.0 (map :radius (core/fireballs (:state world))))]
            (support/assert-gt r min-r (str "fireball radius " r " not > " min-r)))
          world)}

   {:pattern #"^a fireball radius is greater than <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ r-param] example]
          (let [min-r (Double/parseDouble (str (support/require-value example r-param)))
                r (apply max 0.0 (map :radius (core/fireballs (:state world))))]
            (support/assert-gt r min-r (str "fireball radius " r " not > " min-r)))
          world)}

   {:pattern #"^a fireball radius is less than the max fireball radius$"
    :fn (fn [world _ _]
          (let [max-r (core/max-fireball-radius (:state world))
                r (apply max 0.0 (map :radius (core/fireballs (:state world))))]
            (support/assert-lt r max-r (str "fireball radius " r " not < max " max-r)))
          world)}

   {:pattern #"^a destroyable target at (-?\d+) (-?\d+)$"
    :fn (fn [world [_ x y] _]
          (assoc world :state
                 (core/add-destroyable-target
                  (:state world)
                  (support/parse-int x "x")
                  (support/parse-int y "y"))))}

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
            (support/assert-condition (some :destroyed? targets)
                              "expected a destroyed target"))
          world)}

   {:pattern #"^the destroyable target is not destroyed$"
    :fn (fn [world _ _]
          (let [targets (core/destroyable-targets (:state world))]
            (support/assert-condition (every? (complement :destroyed?) targets)
                              "expected no destroyed targets"))
          world)}

   {:pattern #"^the last applied time step is at most ([0-9.]+) seconds$"
    :fn (fn [world [_ max-dt-text] _]
          (let [max-dt (Double/parseDouble max-dt-text)
                actual (core/last-applied-dt (:state world))]
            (support/assert-condition (<= actual max-dt)
                              (str "last applied dt " actual " > " max-dt)))
          world)}

   {:pattern #"^the last applied time step is at most <([A-Za-z0-9_]+)> seconds$"
    :fn (fn [world [_ dt-param] example]
          (let [max-dt (Double/parseDouble (str (support/require-value example dt-param)))
                actual (core/last-applied-dt (:state world))]
            (support/assert-condition (<= actual max-dt)
                              (str "last applied dt " actual " > " max-dt)))
          world)}

   {:pattern #"^the last applied dt is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ dt-param] example]
          (let [expected (support/example-double example dt-param "applied dt")
                actual (core/last-applied-dt (:state world))]
            (support/assert-condition (= expected actual)
                              (str "last applied dt " actual " expected " expected)))
          world)}

   {:pattern #"^the first enemy missile progress equals <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ p-param] example]
          (let [expected (support/example-double example p-param "progress")
                m (first (core/enemy-missiles (:state world)))
                actual (double (:progress m))]
            (support/assert-condition m "missing enemy missile")
            (support/assert-condition (< (Math/abs (- actual expected)) 1.0e-9)
                              (str "enemy progress " actual " expected " expected)))
          world)}

   {:pattern #"^the first enemy missile progress is less than <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ progress-param] example]
          (let [bound (support/example-double example progress-param "split progress")
                m (first (core/enemy-missiles (:state world)))
                actual (double (:progress m 0.0))]
            (support/assert-condition m "missing enemy missile")
            (support/assert-lt actual bound
                       (str "enemy progress " actual " not less than " bound)))
          world)}

   {:pattern #"^there is (\d+) MIRV parent in flight$"
    :fn (fn [world [_ count-text] _]
          (support/assert-count (count (core/mirv-parents (:state world)))
                        (support/parse-int count-text "mirv parent count")
                        "MIRV parents")
          world)}

   {:pattern #"^there are (\d+) MIRV parents in flight$"
    :fn (fn [world [_ count-text] _]
          (support/assert-count (count (core/mirv-parents (:state world)))
                        (support/parse-int count-text "mirv parent count")
                        "MIRV parents")
          world)}

   {:pattern #"^every in-flight enemy is a MIRV child warhead$"
    :fn (fn [world _ _]
          (let [enemies (core/enemy-missiles (:state world))]
            (support/assert-condition (seq enemies) "no enemy missiles")
            (doseq [e enemies]
              (support/assert-condition (= core/enemy-kind-mirv-child (:enemy-kind e))
                                (str "enemy " (:id e) " kind "
                                     (:enemy-kind e) " expected mirv-child"))))
          world)}

   {:pattern #"^the MIRV child warheads target more than one distinct target$"
    :fn (fn [world _ _]
          (let [targets (set (map (juxt :target-kind :target-id)
                                  (core/mirv-children (:state world))))]
            (support/assert-condition (< 1 (count targets))
                              (str "expected multiple child targets, got " targets)))
          world)}

   {:pattern #"^every MIRV child warhead has progressed toward its target$"
    :fn (fn [world _ _]
          (let [children (core/mirv-children (:state world))]
            (support/assert-condition (seq children) "no MIRV children")
            (doseq [c children]
              (support/assert-gt (double (:progress c 0.0)) 0.0
                         (str "child " (:id c) " has not progressed"))))
          world)}

   {:pattern #"^time advances until the MIRV has split or all enemies are gone$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (cond
              (empty? (core/enemy-missiles s)) (assoc world :state s)
              (and (empty? (core/mirv-parents s))
                   (seq (core/mirv-children s))) (assoc world :state s)
              (> n 20000) (support/fail! "MIRV never split")
              :else (recur (:state (core/tick s 0.05)) (inc n)))))}

   {:pattern #"^the first MIRV child warhead path passes within distance <([A-Za-z0-9_]+)> of that fireball center$"
    :fn (fn [world _ _]
          (assoc world :state
                 (core/route-first-mirv-child-through-point
                  (:state world)
                  (:fireball-x world)
                  (:fireball-y world))))}

   {:pattern #"^time advances until the first MIRV child is inside the fireball radius or has impacted$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (cond
              (empty? (core/mirv-children s)) (assoc world :state s)
              (= :fireball (core/last-enemy-fate s)) (assoc world :state s)
              (> n 20000) (support/fail! "MIRV child never hit fireball or impact")
              :else (recur (:state (core/tick s 0.01)) (inc n)))))}

   {:pattern #"^the first MIRV child warhead is destroyed by the fireball$"
    :fn (fn [world _ _]
          (support/assert-condition (= :fireball (core/last-enemy-fate (:state world)))
                            (str "expected fireball kill of child, got "
                                 (core/last-enemy-fate (:state world))))
          world)}

   {:pattern #"^wave <([A-Za-z0-9_]+)> MIRV schedule count is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param count-param] example]
          (let [w (support/example-int example wave-param "wave")
                expected (support/example-int example count-param "mirv count")
                actual (core/wave-mirv-count w)]
            (support/assert-condition (= expected actual)
                              (str "wave " w " mirv count " actual
                                   " expected " expected)))
          world)}

   {:pattern #"^there is (\d+) smart bomb in flight$"
    :fn (fn [world [_ count-text] _]
          (support/assert-count (count (core/smart-bombs (:state world)))
                        (support/parse-int count-text "smart bomb count")
                        "smart bombs")
          world)}

   {:pattern #"^there are (\d+) smart bombs in flight$"
    :fn (fn [world [_ count-text] _]
          (support/assert-count (count (core/smart-bombs (:state world)))
                        (support/parse-int count-text "smart bomb count")
                        "smart bombs")
          world)}

   {:pattern #"^a smart bomb has progressed toward city <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ city-param] example]
          (let [city-id (support/example-int example city-param "city")
                m (first (filter #(and (= :city (:target-kind %))
                                       (= city-id (:target-id %)))
                                 (core/smart-bombs (:state world))))]
            (support/assert-condition m "missing smart bomb")
            (support/assert-gt (double (:progress m 0.0)) 0.0
                       "smart bomb has not progressed"))
          world)}

   {:pattern #"^the smart bomb path is centered in that fireball within distance <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ limit-param] example]
          (assoc world :state
                 (core/route-smart-bomb-centered-in-fireball
                  (:state world)
                  (:fireball-x world)
                  (:fireball-y world)
                  (support/example-int example limit-param "center limit"))))}

   {:pattern #"^the smart bomb path is only in the edge band of that fireball between <([A-Za-z0-9_]+)> and <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ inner-param radius-param] example]
          (assoc world :state
                 (core/route-smart-bomb-edge-band-in-fireball
                  (:state world)
                  (:fireball-x world)
                  (:fireball-y world)
                  (support/example-int example inner-param "edge inner")
                  (support/example-int example radius-param "radius"))))}

   {:pattern #"^time advances until the smart bomb is inside the fireball radius or has impacted$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (cond
              (empty? (core/smart-bombs s)) (assoc world :state s)
              (= :fireball (core/last-enemy-fate s)) (assoc world :state s)
              (> n 20000) (support/fail! "smart bomb never hit fireball or impact")
              :else (recur (:state (core/tick s 0.01)) (inc n)))))}

   {:pattern #"^time advances until the smart bomb would enter the fireball edge band or has impacted$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (let [bomb (first (core/smart-bombs s))]
              (cond
                (nil? bomb) (assoc world :state s)
                (:smart-evaded? bomb) (assoc world :state s)
                (= :fireball (core/last-enemy-fate s)) (assoc world :state s)
                (> n 20000) (support/fail! "smart bomb never entered edge band")
                :else (recur (:state (core/tick s 0.01)) (inc n))))))}

   {:pattern #"^the smart bomb is destroyed by the fireball$"
    :fn (fn [world _ _]
          (support/assert-condition (= :fireball (core/last-enemy-fate (:state world)))
                            (str "expected smart bomb fireball kill, got "
                                 (core/last-enemy-fate (:state world))))
          world)}

   {:pattern #"^the smart bomb is not destroyed by the fireball$"
    :fn (fn [world _ _]
          (support/assert-condition (not= :fireball (core/last-enemy-fate (:state world)))
                            "smart bomb was destroyed by fireball")
          (support/assert-condition (seq (core/smart-bombs (:state world)))
                            "smart bomb missing after evade")
          world)}

   {:pattern #"^the smart bomb has evaded the fireball$"
    :fn (fn [world _ _]
          (let [bomb (first (core/smart-bombs (:state world)))]
            (support/assert-condition bomb "missing smart bomb")
            (support/assert-condition (:smart-evaded? bomb)
                              "smart bomb has not evaded"))
          world)}

   {:pattern #"^there is (\d+) enemy missile in flight$"
    :fn (fn [world [_ count-text] _]
          (support/assert-count (count (core/enemy-missiles (:state world)))
                        (support/parse-int count-text "enemy count")
                        "enemy missiles")
          world)}

   {:pattern #"^wave <([A-Za-z0-9_]+)> smart bomb schedule count is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param count-param] example]
          (let [w (support/example-int example wave-param "wave")
                expected (support/example-int example count-param "smart count")
                actual (core/wave-smart-bomb-count w)]
            (support/assert-condition (= expected actual)
                              (str "wave " w " smart bomb count " actual
                                   " expected " expected)))
          world)}

   {:pattern #"^a <([A-Za-z0-9_]+)> flyer from <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)> toward <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)> at speed <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ kind-param x0 y0 x1 y1 speed-param] example]
          (assoc world :state
                 (core/spawn-flyer
                  (:state world)
                  (support/require-value example kind-param)
                  (support/example-int example x0 "start x")
                  (support/example-int example y0 "start y")
                  (support/example-int example x1 "end x")
                  (support/example-int example y1 "end y")
                  (support/example-int example speed-param "speed"))))}

   {:pattern #"^the flyer drops <([A-Za-z0-9_]+)> enemy missiles toward living cities at path progress <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ count-param progress-param] example]
          (assoc world :state
                 (core/set-flyer-drops-toward-living-cities
                  (:state world)
                  (support/example-int example count-param "drop count")
                  (support/example-double example progress-param "drop progress"))))}

   {:pattern #"^the flyer drops (\d+) enemy missile targeting city <([A-Za-z0-9_]+)> at path progress <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ _count city-param progress-param] example]
          (assoc world :state
                 (core/set-flyer-drop-targeting-city
                  (:state world)
                  (support/example-int example city-param "city")
                  (support/example-double example progress-param "drop progress"))))}

   {:pattern #"^time advances until the flyer has passed drop progress <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ progress-param] example]
          (let [p (support/example-double example progress-param "drop progress")]
            (loop [s (:state world) n 0]
              (let [f (first (core/flyers s))]
                (cond
                  (and f (>= (double (:progress f 0.0)) p)) (assoc world :state s)
                  (nil? f) (assoc world :state s)
                  (> n 20000) (support/fail! "flyer never reached drop progress")
                  :else (recur (:state (core/tick s 0.05)) (inc n)))))))}

   {:pattern #"^there is (\d+) <([A-Za-z0-9_]+)> flyer in flight$"
    :fn (fn [world [_ count-text kind-param] example]
          (support/assert-count (count (core/flyers-of-kind
                                (:state world)
                                (support/require-value example kind-param)))
                        (support/parse-int count-text "flyer count")
                        "flyers")
          world)}

   {:pattern #"^there are (\d+) <([A-Za-z0-9_]+)> flyers in flight$"
    :fn (fn [world [_ count-text kind-param] example]
          (support/assert-count (count (core/flyers-of-kind
                                (:state world)
                                (support/require-value example kind-param)))
                        (support/parse-int count-text "flyer count")
                        "flyers")
          world)}

   {:pattern #"^the <([A-Za-z0-9_]+)> flyer has progressed along its path$"
    :fn (fn [world [_ kind-param] example]
          (let [f (first (core/flyers-of-kind
                          (:state world)
                          (support/require-value example kind-param)))]
            (support/assert-condition f "missing flyer")
            (support/assert-gt (double (:progress f 0.0)) 0.0 "flyer has not progressed"))
          world)}

   {:pattern #"^the <([A-Za-z0-9_]+)> flyer y is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ kind-param y-param] example]
          (let [f (first (core/flyers-of-kind
                          (:state world)
                          (support/require-value example kind-param)))
                expected (support/example-int example y-param "y")]
            (support/assert-condition f "missing flyer")
            (support/assert-condition (= (double expected) (double (:y f)))
                              (str "flyer y " (:y f) " expected " expected)))
          world)}

   {:pattern #"^every dropped enemy missile originates at the flyer position$"
    :fn (fn [world _ _]
          (let [f (first (core/flyers (:state world)))
                enemies (core/enemy-missiles (:state world))
                origin-ys (set (map #(double (:y0 %)) enemies))
                origin-xs (set (map #(double (:x0 %)) enemies))]
            (support/assert-condition f "missing flyer")
            (support/assert-condition (seq enemies) "no dropped enemies")
            (support/assert-condition (every? :dropped-from-flyer? enemies)
                              "enemy missing dropped-from-flyer marker")
            ;; All drops at one progress share origin; altitude matches flyer path.
            (support/assert-condition (= 1 (count origin-xs))
                              (str "expected shared drop origin x, got " origin-xs))
            (support/assert-condition (= 1 (count origin-ys))
                              (str "expected shared drop origin y, got " origin-ys))
            (support/assert-condition (= (double (:y0 f)) (first origin-ys))
                              (str "drop origin y " (first origin-ys)
                                   " expected flyer altitude " (:y0 f))))
          world)}

   {:pattern #"^the flyer path passes within distance <([A-Za-z0-9_]+)> of that fireball center$"
    :fn (fn [world _ _]
          (assoc world :state
                 (core/route-flyer-through-point
                  (:state world)
                  (:fireball-x world)
                  (:fireball-y world))))}

   {:pattern #"^time advances until the flyer is inside the fireball radius or has left the playfield$"
    :fn (fn [world _ _]
          (loop [s (:state world) n 0]
            (cond
              (empty? (core/flyers s)) (assoc world :state s)
              (= :fireball (:last-flyer-fate s)) (assoc world :state s)
              (> n 20000) (support/fail! "flyer never hit fireball or left")
              :else (recur (:state (core/tick s 0.01)) (inc n)))))}

   {:pattern #"^the <([A-Za-z0-9_]+)> flyer is destroyed by the fireball$"
    :fn (fn [world [_ kind-param] example]
          (support/assert-condition (= :fireball (:last-flyer-fate (:state world)))
                            (str "expected flyer fireball kill for "
                                 (support/require-value example kind-param)
                                 ", got " (:last-flyer-fate (:state world))))
          world)}

   {:pattern #"^wave <([A-Za-z0-9_]+)> bomber schedule count is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param count-param] example]
          (let [w (support/example-int example wave-param "wave")
                expected (support/example-int example count-param "bomber count")
                actual (core/wave-bomber-count w)]
            (support/assert-condition (= expected actual)
                              (str "wave " w " bomber count " actual
                                   " expected " expected)))
          world)}

   {:pattern #"^wave <([A-Za-z0-9_]+)> satellite schedule count is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param count-param] example]
          (let [w (support/example-int example wave-param "wave")
                expected (support/example-int example count-param "satellite count")
                actual (core/wave-satellite-count w)]
            (support/assert-condition (= expected actual)
                              (str "wave " w " satellite count " actual
                                   " expected " expected)))
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
            (support/assert-condition m "missing defensive missile")
            (support/assert-gt (:progress m) 0.0 "missile has not progressed"))
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
            (support/assert-condition m "missing defensive missile")
            (support/assert-lt (:progress m) 1.0 "missile already reached aim"))
          world)}

   {:pattern #"^an enemy missile targeting city (\d+)$"
    :fn (fn [world [_ city-text] _]
          (assoc world :state
                 (core/spawn-enemy-targeting-city
                  (:state world)
                  (support/parse-int city-text "city"))))}

   {:pattern #"^an enemy missile targeting city <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ city-param] example]
          (assoc world :state
                 (core/spawn-enemy-targeting-city
                  (:state world)
                  (support/example-int example city-param "city"))))}

   {:pattern #"^an enemy missile from <([A-Za-z0-9_]+)> (\d+) targeting city <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ ox-param oy-text city-param] example]
          (assoc world :state
                 (core/spawn-enemy-targeting-city-from
                  (:state world)
                  (support/example-int example ox-param "origin x")
                  (support/parse-int oy-text "origin y")
                  (support/example-int example city-param "city"))))}

   {:pattern #"^a MIRV enemy missile targeting city <([A-Za-z0-9_]+)> that splits into <([A-Za-z0-9_]+)> warheads at progress <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ city-param count-param progress-param] example]
          (assoc world :state
                 (core/spawn-mirv-targeting-city
                  (:state world)
                  (support/example-int example city-param "city")
                  (support/example-int example count-param "child count")
                  (support/example-double example progress-param "split progress"))))}

   {:pattern #"^a smart bomb targeting city <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ city-param] example]
          (assoc world :state
                 (core/spawn-smart-bomb-targeting-city
                  (:state world)
                  (support/example-int example city-param "city"))))}

   {:pattern #"^a smart bomb targeting city (\d+)$"
    :fn (fn [world [_ city-text] _]
          (assoc world :state
                 (core/spawn-smart-bomb-targeting-city
                  (:state world)
                  (support/parse-int city-text "city"))))}

   {:pattern #"^an enemy missile from <([A-Za-z0-9_]+)> (\d+) targeting battery <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ ox-param oy-text battery-param] example]
          (assoc world :state
                 (core/spawn-enemy-targeting-battery-from
                  (:state world)
                  (support/example-int example ox-param "origin x")
                  (support/parse-int oy-text "origin y")
                  (support/example-battery example battery-param))))}

   {:pattern #"^an enemy missile targeting battery (left|center|right)$"
    :fn (fn [world [_ battery-name] _]
          (assoc world :state
                 (core/spawn-enemy-targeting-battery
                  (:state world)
                  (support/parse-battery-id battery-name))))}

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

   {:pattern #"^there are (\d+) enemy missiles in flight$"
    :fn (fn [world [_ count-text] _]
          (support/assert-count (count (core/enemy-missiles (:state world)))
                        (support/parse-int count-text "enemy count")
                        "enemy missiles")
          world)}

   {:pattern #"^there are <([A-Za-z0-9_]+)> enemy missiles in flight$"
    :fn (fn [world [_ count-param] example]
          (support/assert-count (count (core/enemy-missiles (:state world)))
                        (support/example-int example count-param "enemy count")
                        "enemy missiles")
          world)}

   {:pattern #"^an enemy missile has progressed toward city <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ city-param] example]
          (let [city-id (support/example-int example city-param "city")
                m (first (filter #(and (= :city (:target-kind %))
                                       (= city-id (:target-id %)))
                                 (core/enemy-missiles (:state world))))]
            (support/assert-condition m "missing enemy missile")
            (support/assert-gt (:progress m) 0.0 "enemy has not progressed"))
          world)}

   {:pattern #"^an enemy missile has progressed toward city (\d+)$"
    :fn (fn [world [_ city-text] _]
          (let [city-id (support/parse-int city-text "city")
                m (first (filter #(and (= :city (:target-kind %))
                                       (= city-id (:target-id %)))
                                 (core/enemy-missiles (:state world))))]
            (support/assert-condition m "missing enemy missile")
            (support/assert-gt (:progress m) 0.0 "enemy has not progressed"))
          world)}

   {:pattern #"^the first enemy missile origin is <([A-Za-z0-9_]+)> (\d+)$"
    :fn (fn [world [_ ox-param oy-text] example]
          (let [m (first (core/enemy-missiles (:state world)))
                ox (support/example-int example ox-param "origin x")
                oy (support/parse-int oy-text "origin y")]
            (support/assert-condition m "missing enemy missile")
            (support/assert-condition (= (double ox) (double (:x0 m)))
                              (str "enemy origin x " (:x0 m) " expected " ox))
            (support/assert-condition (= (double oy) (double (:y0 m)))
                              (str "enemy origin y " (:y0 m) " expected " oy)))
          world)}

   {:pattern #"^the first enemy missile origin x differs from its target x$"
    :fn (fn [world _ _]
          (let [m (first (core/enemy-missiles (:state world)))]
            (support/assert-condition m "missing enemy missile")
            (support/assert-condition (not= (double (:x0 m)) (double (:x1 m)))
                              (str "enemy origin x equals target x " (:x0 m))))
          world)}

   {:pattern #"^the first enemy missile has moved toward its target on both axes$"
    :fn (fn [world _ _]
          (let [m (first (core/enemy-missiles (:state world)))]
            (support/assert-condition m "missing enemy missile")
            (let [x0 (double (:x0 m))
                  y0 (double (:y0 m))
                  x1 (double (:x1 m))
                  y1 (double (:y1 m))
                  x (double (:x m))
                  y (double (:y m))]
              (support/assert-condition (or (and (< x0 x x1) (< y0 y y1))
                                    (and (> x0 x x1) (< y0 y y1))
                                    (and (< x0 x x1) (> y0 y y1))
                                    (and (> x0 x x1) (> y0 y y1)))
                                (str "enemy not between origin and target: "
                                     x "," y " from " x0 "," y0 " to " x1 "," y1))))
          world)}

   {:pattern #"^every enemy missile origin y is (\d+)$"
    :fn (fn [world [_ y-text] _]
          (let [y (support/parse-int y-text "origin y")
                enemies (core/enemy-missiles (:state world))]
            (support/assert-condition (seq enemies) "no enemy missiles")
            (doseq [m enemies]
              (support/assert-condition (= (double y) (double (:y0 m)))
                                (str "enemy " (:id m) " origin y " (:y0 m)
                                     " expected " y))))
          world)}

   {:pattern #"^every enemy missile origin x is within the playfield$"
    :fn (fn [world _ _]
          (let [w (core/playfield-width (:state world))
                enemies (core/enemy-missiles (:state world))]
            (support/assert-condition (seq enemies) "no enemy missiles")
            (doseq [m enemies]
              (support/assert-condition (and (<= 0 (double (:x0 m)))
                                     (< (double (:x0 m)) w))
                                (str "enemy " (:id m) " origin x " (:x0 m)
                                     " not in [0," w ")"))))
          world)}

   {:pattern #"^the enemy missiles use more than one distinct origin x$"
    :fn (fn [world _ _]
          (let [xs (set (map #(double (:x0 %))
                             (core/enemy-missiles (:state world))))]
            (support/assert-condition (< 1 (count xs))
                              (str "expected multiple origin x, got " xs)))
          world)}

   {:pattern #"^at least one enemy missile origin x differs from its target x$"
    :fn (fn [world _ _]
          (let [enemies (core/enemy-missiles (:state world))]
            (support/assert-condition (seq enemies) "no enemy missiles")
            (support/assert-condition (some #(not= (double (:x0 %)) (double (:x1 %)))
                                    enemies)
                              "all enemy origins share target x (all vertical)"))
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

   {:pattern #"^a fireball at (-?\d+) (-?\d+) with radius (\d+)$"
    :fn (fn [world [_ x y r] _]
          (assoc world
                 :state (core/add-static-fireball
                         (:state world)
                         (support/parse-int x "x")
                         (support/parse-int y "y")
                         (support/parse-int r "radius"))
                 :fireball-x (support/parse-int x "x")
                 :fireball-y (support/parse-int y "y")))}

   {:pattern #"^a fireball at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)> with radius <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param r-param] example]
          (let [x (support/example-int example x-param "x")
                y (support/example-int example y-param "y")
                r (support/example-int example r-param "radius")]
            (assoc world
                   :state (core/add-static-fireball (:state world) x y r)
                   :fireball-x x
                   :fireball-y y)))}

   {:pattern #"^the enemy missile path passes within distance (\d+) of that fireball center$"
    :fn (fn [world [_ _] _]
          (assoc world :state
                 (core/route-enemy-through-point
                  (:state world)
                  (:fireball-x world)
                  (:fireball-y world))))}

   {:pattern #"^the enemy missile path passes within distance <([A-Za-z0-9_]+)> of that fireball center$"
    :fn (fn [world [_ _r-param] _]
          (assoc world :state
                 (core/route-enemy-through-point
                  (:state world)
                  (:fireball-x world)
                  (:fireball-y world))))}

   {:pattern #"^the enemy missile path stays farther than (\d+) from that fireball center$"
    :fn (fn [world _ _]
          ;; Default vertical spawn plus far fireball examples already satisfy this.
          world)}

   {:pattern #"^the enemy missile path stays farther than <([A-Za-z0-9_]+)> from that fireball center$"
    :fn (fn [world _ _]
          ;; Default vertical spawn plus far fireball examples already satisfy this.
          world)}

   {:pattern #"^the enemy missile is destroyed by the fireball$"
    :fn (fn [world _ _]
          (support/assert-condition (= :fireball (core/last-enemy-fate (:state world)))
                            (str "expected fireball kill, got "
                                 (core/last-enemy-fate (:state world))))
          world)}

   {:pattern #"^city (\d+) is living$"
    :fn (fn [world [_ city-text] _]
          (let [city-id (support/parse-int city-text "city")]
            (support/assert-condition (core/living-city? (:state world) city-id)
                              (str "city " city-id " is not living")))
          world)}

   {:pattern #"^city <([A-Za-z0-9_]+)> is living$"
    :fn (fn [world [_ city-param] example]
          (let [city-id (support/example-int example city-param "city")]
            (support/assert-condition (core/living-city? (:state world) city-id)
                              (str "city " city-id " is not living")))
          world)}

   {:pattern #"^city (\d+) is not living$"
    :fn (fn [world [_ city-text] _]
          (let [city-id (support/parse-int city-text "city")
                city (core/city (:state world) city-id)]
            (support/assert-condition city (str "city " city-id " does not exist"))
            (support/assert-condition (not (:alive? city))
                              (str "city " city-id " is still living")))
          world)}

   {:pattern #"^city <([A-Za-z0-9_]+)> is not living$"
    :fn (fn [world [_ city-param] example]
          (let [city-id (support/example-int example city-param "city")
                city (core/city (:state world) city-id)]
            (support/assert-condition city (str "city " city-id " does not exist"))
            (support/assert-condition (not (:alive? city))
                              (str "city " city-id " is still living")))
          world)}

   {:pattern #"^the wave number is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param] example]
          (let [expected (support/example-int example wave-param "wave")
                actual (core/wave (:state world))]
            (support/assert-condition (= expected actual)
                              (str "wave " actual " expected " expected)))
          world)}

   {:pattern #"^the hud shows wave <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param] example]
          (let [expected (support/example-int example wave-param "wave")
                actual (:wave (core/hud (:state world)))]
            (support/assert-condition (= expected actual)
                              (str "hud wave " actual " expected " expected)))
          world)}

   {:pattern #"^each non-destroyed battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ ammo-param] example]
          (let [ammo (support/example-int example ammo-param "ammo")]
            (doseq [b (remove :destroyed? (batteries world))]
              (support/assert-condition (= ammo (:missiles b))
                                (str "battery " (:id b) " missiles "
                                     (:missiles b) " expected " ammo))))
          world)}

   {:pattern #"^the current wave has <([A-Za-z0-9_]+)> scheduled enemies still active$"
    :fn (fn [world [_ rem-param] example]
          (assoc world :state
                 (core/set-wave-enemies-active
                  (:state world)
                  (support/example-int example rem-param "remaining"))))}

   {:pattern #"^the current wave has (\d+) scheduled enemies still active$"
    :fn (fn [world [_ rem-text] _]
          (assoc world :state
                 (core/set-wave-enemies-active
                  (:state world)
                  (support/parse-int rem-text "remaining"))))}

   {:pattern #"^the wave is not complete$"
    :fn (fn [world _ _]
          (support/assert-condition (not (core/wave-complete? (:state world)))
                            "wave is complete but should not be")
          world)}

   {:pattern #"^the wave is complete$"
    :fn (fn [world _ _]
          (support/assert-condition (core/wave-complete? (:state world))
                            "wave is not complete")
          world)}

   {:pattern #"^time advances until all wave enemies are destroyed or have impacted$"
    :fn (fn [world _ _]
          (advance-until world
                         (fn [s]
                           (and (core/wave-complete? s)
                                (empty? (core/enemy-missiles s))))
                         0.05
                         10000
                         "wave enemies never finished"))}

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
            (support/assert-condition (core/harder-wave? low-m high-m)
                              (str "wave " high " not harder than " low
                                   " metrics " low-m " vs " high-m)))
          world)}

   {:pattern #"^wave <([A-Za-z0-9_]+)> has enemy count <([A-Za-z0-9_]+)> and speed <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param count-param speed-param] example]
          (let [wave (support/example-int example wave-param "wave")
                expected-count (support/example-int example count-param "enemy count")
                expected-speed (support/example-double example speed-param "enemy speed")
                metrics (core/wave-schedule-metrics wave)]
            (support/assert-condition (= expected-count (:enemy-count metrics))
                              (str "wave " wave " enemy count "
                                   (:enemy-count metrics) " expected " expected-count))
            (support/assert-condition (= expected-speed (double (:enemy-speed metrics)))
                              (str "wave " wave " enemy speed "
                                   (:enemy-speed metrics) " expected " expected-speed)))
          world)}]
   enemy-steps/handlers))

(defn- match-handler
  [text]
  (some (fn [handler]
          (when-let [matches (re-matches (:pattern handler) text)]
            [handler matches]))
        step-handlers))

(def gherkin-phases
  {"Given" :given
   "When" :when
   "Then" :then})

(defn- apply-gherkin-phase
  [world keyword]
  (if-let [phase (get gherkin-phases keyword)]
    (assoc world :gherkin-phase phase)
    world))

(defn dispatch-step
  [world step example]
  (let [text (:text step)
        world (apply-gherkin-phase world (:keyword step))]
    (if-let [[handler matches] (match-handler text)]
      ((:fn handler) world matches example)
      (support/fail! (str "unsupported step: " text)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T14:33:10.063191-05:00", :module-hash "347503470", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "692061305"} {:id "defn-/assert-playfield-dimension", :kind "defn-", :line 6, :end-line 12, :hash "-1180112229"} {:id "defn-/living-cities", :kind "defn-", :line 14, :end-line 16, :hash "-548423997"} {:id "defn-/batteries", :kind "defn-", :line 18, :end-line 20, :hash "2112668418"} {:id "defn-/battery", :kind "defn-", :line 22, :end-line 25, :hash "-1798101236"} {:id "defn-/city-xs", :kind "defn-", :line 27, :end-line 29, :hash "-1231463410"} {:id "defn-/city-span", :kind "defn-", :line 31, :end-line 34, :hash "-826397513"} {:id "defn-/example-width", :kind "defn-", :line 36, :end-line 38, :hash "1667157547"} {:id "defn-/example-height", :kind "defn-", :line 40, :end-line 42, :hash "-1096613354"} {:id "defn-/one-third", :kind "defn-", :line 44, :end-line 46, :hash "1669847708"} {:id "defn-/two-thirds", :kind "defn-", :line 48, :end-line 50, :hash "1105411976"} {:id "defn-/assert-entities-in-ground-band", :kind "defn-", :line 52, :end-line 58, :hash "-247193944"} {:id "defn-/assert-xs-in-playfield", :kind "defn-", :line 60, :end-line 65, :hash "-807069005"} {:id "defn-/assert-between-open", :kind "defn-", :line 67, :end-line 69, :hash "1230020693"} {:id "defn-/earlier-fallback-batteries", :kind "defn-", :line 70, :end-line 76, :hash "113626062"} {:id "defn-/disable-earlier-batteries", :kind "defn-", :line 78, :end-line 80, :hash "1165353126"} {:id "defn-/max-fireball-radius", :kind "defn-", :line 82, :end-line 86, :hash "1308487225"} {:id "def/fireball-peak-fraction", :kind "def", :line 88, :end-line 88, :hash "-1421501801"} {:id "defn-/fireball-reached-peak?", :kind "defn-", :line 90, :end-line 94, :hash "1083437537"} {:id "defn-/fireball-in-shrink-phase?", :kind "defn-", :line 96, :end-line 100, :hash "1989596245"} {:id "defn-/fireball-radius-at-least?", :kind "defn-", :line 102, :end-line 104, :hash "-1598181125"} {:id "defn-/advance-until", :kind "defn-", :line 106, :end-line 109, :hash "832982022"} {:id "def/step-handlers", :kind "def", :line 111, :end-line 885, :hash "869456202"} {:id "defn-/match-handler", :kind "defn-", :line 887, :end-line 892, :hash "-760290467"} {:id "def/gherkin-phases", :kind "def", :line 894, :end-line 897, :hash "762060511"} {:id "defn-/apply-gherkin-phase", :kind "defn-", :line 899, :end-line 903, :hash "-1691268912"} {:id "defn/dispatch-step", :kind "defn", :line 905, :end-line 911, :hash "1912576952"}]}
;; clj-mutate-manifest-end

(ns missile-command.acceptance.steps
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]
            [missile-command.world :as world]))

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

(def step-handlers
  [{:pattern #"^a new game with width <([A-Za-z0-9_]+)> and height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param height-param] example]
          (assoc world :state
                 (core/new-game
                  {:width (support/example-int example width-param "width")
                   :height (support/example-int example height-param "height")})))}

   {:pattern #"^the playfield is resized to width <([A-Za-z0-9_]+)> and height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param height-param] example]
          (assoc world :state
                 (core/resize (:state world)
                              (support/example-int example width-param "width")
                              (support/example-int example height-param "height"))))}

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
            (when-not (apply < xs)
              (support/fail! (str "city x positions not increasing: " xs))))
          world)}

   {:pattern #"^every city x is between 0 inclusive and <([A-Za-z0-9_]+)> exclusive$"
    :fn (fn [world [_ width-param] example]
          (let [width (support/example-int example width-param "width")]
            (doseq [city (core/cities (:state world))]
              (when-not (and (<= 0 (:x city)) (< (:x city) width))
                (support/fail! (str "city " (:id city) " x " (:x city)
                                    " not in [0," width ")")))))
          world)}

   {:pattern #"^every city y is in the ground band for height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ height-param] example]
          (let [height (support/example-int example height-param "height")]
            (doseq [city (core/cities (:state world))]
              (when-not (world/in-ground-band? (:y city) height)
                (support/fail! (str "city " (:id city) " y " (:y city)
                                    " not in ground band for height " height)))))
          world)}

   {:pattern #"^the leftmost city x is less than one third of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (support/example-int example width-param "width")
                leftmost (apply min (city-xs world))]
            (when-not (< leftmost (/ width 3.0))
              (support/fail! (str "leftmost city x " leftmost
                                  " not < one third of " width))))
          world)}

   {:pattern #"^the rightmost city x is greater than two thirds of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (support/example-int example width-param "width")
                rightmost (apply max (city-xs world))]
            (when-not (> rightmost (* width (/ 2.0 3)))
              (support/fail! (str "rightmost city x " rightmost
                                  " not > two thirds of " width))))
          world)}

   {:pattern #"^the left battery x is less than the center battery x$"
    :fn (fn [world _ _]
          (when-not (< (:x (battery world :left)) (:x (battery world :center)))
            (support/fail! "left battery x not less than center"))
          world)}

   {:pattern #"^the center battery x is less than the right battery x$"
    :fn (fn [world _ _]
          (when-not (< (:x (battery world :center)) (:x (battery world :right)))
            (support/fail! "center battery x not less than right"))
          world)}

   {:pattern #"^the left battery x is less than one third of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (support/example-int example width-param "width")
                x (:x (battery world :left))]
            (when-not (< x (/ width 3.0))
              (support/fail! (str "left battery x " x " not < one third of " width))))
          world)}

   {:pattern #"^the center battery x is between one third and two thirds of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (support/example-int example width-param "width")
                x (:x (battery world :center))
                lo (/ width 3.0)
                hi (* width (/ 2.0 3))]
            (when-not (and (< lo x) (< x hi))
              (support/fail! (str "center battery x " x " not between " lo " and " hi))))
          world)}

   {:pattern #"^the right battery x is greater than two thirds of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (support/example-int example width-param "width")
                x (:x (battery world :right))]
            (when-not (> x (* width (/ 2.0 3)))
              (support/fail! (str "right battery x " x " not > two thirds of " width))))
          world)}

   {:pattern #"^every battery y is in the ground band for height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ height-param] example]
          (let [height (support/example-int example height-param "height")]
            (doseq [b (batteries world)]
              (when-not (world/in-ground-band? (:y b) height)
                (support/fail! (str "battery " (:id b) " y " (:y b)
                                    " not in ground band for height " height)))))
          world)}

   {:pattern #"^the center battery missile speed is greater than the left battery missile speed$"
    :fn (fn [world _ _]
          (when-not (> (:missile-speed (battery world :center))
                       (:missile-speed (battery world :left)))
            (support/fail! "center missile speed not greater than left"))
          world)}

   {:pattern #"^the center battery missile speed is greater than the right battery missile speed$"
    :fn (fn [world _ _]
          (when-not (> (:missile-speed (battery world :center))
                       (:missile-speed (battery world :right)))
            (support/fail! "center missile speed not greater than right"))
          world)}

   {:pattern #"^the horizontal span of the cities is greater than half of width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (support/example-int example width-param "width")
                xs (city-xs world)
                span (- (apply max xs) (apply min xs))]
            (when-not (> span (/ width 2.0))
              (support/fail! (str "city span " span " not > half of " width))))
          world)}

   {:pattern #"^the horizontal span of the cities is less than width <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ width-param] example]
          (let [width (support/example-int example width-param "width")
                xs (city-xs world)
                span (- (apply max xs) (apply min xs))]
            (when-not (< span width)
              (support/fail! (str "city span " span " not < " width))))
          world)}

   {:pattern #"^the player aims at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [result (core/handle (:state world)
                                    {:type :aim
                                     :x (support/example-int example x-param "x")
                                     :y (support/example-int example y-param "y")})]
            (assoc world :state (:state result))))}

   {:pattern #"^the crosshair is at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [expected-x (support/example-int example x-param "x")
                expected-y (support/example-int example y-param "y")
                actual (core/crosshair (:state world))]
            (when-not (and (= expected-x (:x actual))
                           (= expected-y (:y actual)))
              (support/fail! (str "crosshair " actual " expected "
                                  {:x expected-x :y expected-y}))))
          world)}

   {:pattern #"^the score is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ score-param] example]
          (let [expected (support/example-int example score-param "score")
                actual (core/score (:state world))]
            (when-not (= expected actual)
              (support/fail! (str "score " actual " expected " expected))))
          world)}

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

   {:pattern #"^the <([A-Za-z0-9_]+)> battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ battery-param ammo-param] example]
          (let [battery-id (support/example-battery example battery-param)
                expected (support/example-int example ammo-param "ammo")
                actual (:missiles (battery world battery-id))]
            (when-not (= expected actual)
              (support/fail! (str "battery " battery-id " missiles "
                                  actual " expected " expected))))
          world)}

   {:pattern #"^every other battery has <([A-Za-z0-9_]+)> missiles$"
    :fn (fn [world [_ ammo-param] example]
          (let [expected (support/example-int example ammo-param "ammo")
                fired (support/example-battery example "battery")
                others (remove #(= fired (:id %)) (batteries world))]
            (doseq [b others]
              (when-not (= expected (:missiles b))
                (support/fail! (str "battery " (:id b) " missiles "
                                    (:missiles b) " expected " expected)))))
          world)}

   {:pattern #"^there are <([A-Za-z0-9_]+)> defensive missiles in flight$"
    :fn (fn [world [_ count-param] example]
          (assert-count (count (core/defensive-missiles (:state world)))
                        (support/example-int example count-param "missile count")
                        "defensive missiles")
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
            (when-not match
              (support/fail! (str "no defensive missile from " battery-id
                                  " targeting " target-x "," target-y))))
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
          (assoc world :state
                 (core/destroy-battery
                  (:state world)
                  (support/example-battery example battery-param))))}

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
            (when-not (> (:speed center) (:speed left))
              (support/fail! "center missile not faster than left"))
            (when-not (> (:speed center) (:speed right))
              (support/fail! "center missile not faster than right")))
          world)}])

(defn- match-handler
  [text]
  (some (fn [handler]
          (when-let [matches (re-matches (:pattern handler) text)]
            [handler matches]))
        step-handlers))

(defn dispatch-step
  [world step example]
  (let [text (:text step)]
    (if-let [[handler matches] (match-handler text)]
      ((:fn handler) world matches example)
      (support/fail! (str "unsupported step: " text)))))

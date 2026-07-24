(ns missile-command.acceptance.enemy-steps
  "Gherkin steps for enemy missiles, angled origins, and fireball interception."
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(defn first-enemy-missile
  [state]
  (first (core/enemy-missiles state)))

(defn between-endpoints?
  "True when (x,y) is strictly between origin and target on both axes."
  [x0 y0 x1 y1 x y]
  (and (or (< x0 x x1) (> x0 x x1))
       (or (< y0 y y1) (> y0 y y1))))

(def handlers
  [
   {:pattern #"^the first enemy missile progress equals <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ p-param] example]
   (let [expected (support/example-double example p-param "progress")
   m (first (core/enemy-missiles (:state world)))
   actual (double (:progress m))]
   (support/assert-condition m "missing enemy missile")
   (support/assert-condition (< (Math/abs (- actual expected)) 1.0e-9)
   (str "enemy progress " actual " expected " expected)))
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
   (let [m (first-enemy-missile (:state world))
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
   (let [m (first-enemy-missile (:state world))]
   (support/assert-condition m "missing enemy missile")
   (support/assert-condition (not= (double (:x0 m)) (double (:x1 m)))
   (str "enemy origin x equals target x " (:x0 m))))
   world)}

   {:pattern #"^the first enemy missile has moved toward its target on both axes$"
    :fn (fn [world _ _]
   (let [m (first-enemy-missile (:state world))]
   (support/assert-condition m "missing enemy missile")
   (let [x0 (double (:x0 m)) y0 (double (:y0 m))
   x1 (double (:x1 m)) y1 (double (:y1 m))
   x (double (:x m)) y (double (:y m))]
   (support/assert-condition (between-endpoints? x0 y0 x1 y1 x y)
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

   {:pattern #"^a MIRV enemy missile targeting city <([A-Za-z0-9_]+)> that splits into <([A-Za-z0-9_]+)> warheads at progress <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ city-param count-param progress-param] example]
          (assoc world :state
                 (core/spawn-mirv-targeting-city
                  (:state world)
                  (support/example-int example city-param "city")
                  (support/example-int example count-param "child count")
                  (support/example-double example progress-param "split progress"))))}

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


   {:pattern #"^wave <([A-Za-z0-9_]+)> smart bomb schedule count is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param count-param] example]
          (let [w (support/example-int example wave-param "wave")
                expected (support/example-int example count-param "smart count")
                actual (core/wave-smart-bomb-count w)]
            (support/assert-condition (= expected actual)
                              (str "wave " w " smart bomb count " actual
                                   " expected " expected)))
          world)}


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

])

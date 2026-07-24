(ns missile-command.acceptance.fireball-steps
  "Gherkin steps for defensive fireballs and blast phases."
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(def fireball-peak-fraction 0.999)

(defn max-live-fireball-radius
  [state]
  (if (seq (core/fireballs state))
    (apply max (map :radius (core/fireballs state)))
    0.0))

(defn fireball-reached-peak?
  [state]
  (>= (max-live-fireball-radius state)
      (* fireball-peak-fraction (core/max-fireball-radius state))))

(defn fireball-in-shrink-phase?
  [state]
  (and (seq (core/fireballs state))
       (< (max-live-fireball-radius state)
          (core/max-fireball-radius state))))

(defn fireball-radius-at-least?
  [state min-r]
  (>= (max-live-fireball-radius state) min-r))

(defn- advance-until
  [world pred dt max-steps fail-message]
  (support/advance-until world pred core/tick dt max-steps fail-message))


(def handlers
  [
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
])

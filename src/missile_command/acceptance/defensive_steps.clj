(ns missile-command.acceptance.defensive-steps
  "Gherkin steps for defensive missiles."
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(defn- advance-until
  [world pred dt max-steps fail-message]
  (support/advance-until world pred core/tick dt max-steps fail-message))

(def handlers
  [
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

   {:pattern #"^a defensive missile from the <([A-Za-z0-9_]+)> battery targets (-?\d+) (-?\d+)$"
    :fn (fn [world [_ battery-param x-text y-text] example]
          (let [battery-id (support/example-battery example battery-param)
                target-x (support/parse-int x-text "x")
                target-y (support/parse-int y-text "y")
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

   {:pattern #"^time advances until defensive missiles arrive$"
    :fn (fn [world _ _]
          (advance-until world
                         (comp empty? core/defensive-missiles)
                         0.05 5000 "missiles never arrived"))}

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
])

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T15:37:48.434246-05:00", :module-hash "1376549632", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "1510457847"} {:id "defn-/advance-until", :kind "defn-", :line 6, :end-line 8, :hash "1259810946"} {:id "def/handlers", :kind "def", :line 10, :end-line 101, :hash "437682581"}]}
;; clj-mutate-manifest-end

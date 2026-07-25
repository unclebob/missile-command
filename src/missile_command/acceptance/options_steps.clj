(ns missile-command.acceptance.options-steps
  "Gherkin steps for mute, difficulty, and key remapping options."
  (:require [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]
            [missile-command.options :as options]))

(def handlers
  [{:pattern #"^the player opens options from the title$"
    :fn (fn [world _ _]
          (assoc world :state (core/open-options (:state world))))}

   {:pattern #"^the player leaves options$"
    :fn (fn [world _ _]
          (assoc world :state (core/leave-options (:state world))))}

   {:pattern #"^the screen is options$"
    :fn (fn [world _ _]
          (support/assert-condition (core/options? (:state world))
                                    (str "screen is " (core/screen (:state world))
                                         " expected options"))
          world)}

   {:pattern #"^the player sets mute to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ mute-param] example]
          (assoc world :state
                 (core/set-mute
                  (:state world)
                  (options/parse-mute
                   (support/require-value example mute-param)))))}

   {:pattern #"^the player sets mute to (true|false)$"
    :fn (fn [world [_ mute-text] _]
          (assoc world :state
                 (core/set-mute (:state world)
                                (options/parse-mute mute-text))))}

   {:pattern #"^mute is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ mute-param] example]
          (let [expected (options/parse-mute
                          (support/require-value example mute-param))]
            (if (= :then (:gherkin-phase world))
              (let [actual (core/mute? (:state world))]
                (support/assert-condition (= expected actual)
                                          (str "mute " actual
                                               " expected " expected))
                world)
              (assoc world :state (core/set-mute (:state world) expected)))))}

   {:pattern #"^mute is (true|false)$"
    :fn (fn [world [_ mute-text] _]
          (let [expected (options/parse-mute mute-text)]
            (if (= :then (:gherkin-phase world))
              (let [actual (core/mute? (:state world))]
                (support/assert-condition (= expected actual)
                                          (str "mute " actual
                                               " expected " expected))
                world)
              (assoc world :state (core/set-mute (:state world) expected)))))}

   {:pattern #"^the player sets difficulty to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ diff-param] example]
          (assoc world :state
                 (core/set-difficulty
                  (:state world)
                  (support/require-value example diff-param))))}

   {:pattern #"^the player sets difficulty to (easy|normal|arcade)$"
    :fn (fn [world [_ diff] _]
          (assoc world :state (core/set-difficulty (:state world) diff)))}

   {:pattern #"^the difficulty is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ diff-param] example]
          (let [expected (options/parse-difficulty
                          (support/require-value example diff-param))
                actual (core/difficulty (:state world))]
            (support/assert-condition (= expected actual)
                                      (str "difficulty " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the difficulty is (easy|normal|arcade)$"
    :fn (fn [world [_ diff-text] _]
          (let [expected (options/parse-difficulty diff-text)
                actual (core/difficulty (:state world))]
            (support/assert-condition (= expected actual)
                                      (str "difficulty " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the player binds fire <([A-Za-z0-9_]+)> to key <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ battery-param key-param] example]
          (assoc world :state
                 (core/bind-fire-key
                  (:state world)
                  (support/example-battery example battery-param)
                  (support/require-value example key-param))))}

   {:pattern #"^the player binds fire (left|center|right) to key ([A-Za-z0-9]+)$"
    :fn (fn [world [_ battery-name key] _]
          (assoc world :state
                 (core/bind-fire-key
                  (:state world)
                  (support/parse-battery-id battery-name)
                  key)))}

   {:pattern #"^the fire key for (left|center|right) includes <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ battery-name key-param] example]
          (let [battery-id (support/parse-battery-id battery-name)
                key (support/require-value example key-param)]
            (support/assert-condition
             (core/fire-key-includes? (:state world) battery-id key)
             (str "fire key for " battery-id " does not include " key)))
          world)}

   {:pattern #"^the fire key for (left|center|right) includes ([A-Za-z0-9]+)$"
    :fn (fn [world [_ battery-name key] _]
          (let [battery-id (support/parse-battery-id battery-name)]
            (support/assert-condition
             (core/fire-key-includes? (:state world) battery-id key)
             (str "fire key for " battery-id " does not include " key)))
          world)}

   {:pattern #"^the pause key includes <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ key-param] example]
          (let [key (support/require-value example key-param)]
            (support/assert-condition
             (core/pause-key-includes? (:state world) key)
             (str "pause keys do not include " key)))
          world)}

   {:pattern #"^the pause key includes ([A-Za-z0-9]+)$"
    :fn (fn [world [_ key] _]
          (support/assert-condition
           (core/pause-key-includes? (:state world) key)
           (str "pause keys do not include " key))
          world)}

   {:pattern #"^the player presses key <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ key-param] example]
          (assoc world :state
                 (:state (core/press-key
                          (:state world)
                          (support/require-value example key-param)))))}

   {:pattern #"^the player presses key ([A-Za-z0-9]+)$"
    :fn (fn [world [_ key] _]
          (assoc world :state
                 (:state (core/press-key (:state world) key))))}

   {:pattern #"^wave (\d+) enemy count is (\d+)$"
    :fn (fn [world [_ wave-text count-text] _]
          (let [wave (support/parse-int wave-text "wave")
                expected (support/parse-int count-text "enemy count")
                metrics (core/wave-schedule-metrics-for (:state world) wave)
                actual (:enemy-count metrics)]
            (support/assert-condition (= expected actual)
                                      (str "wave " wave " enemy count "
                                           actual " expected " expected)))
          world)}

   {:pattern #"^wave (\d+) enemy speed is ([0-9.]+)$"
    :fn (fn [world [_ wave-text speed-text] _]
          (let [wave (support/parse-int wave-text "wave")
                expected (Double/parseDouble speed-text)
                metrics (core/wave-schedule-metrics-for (:state world) wave)
                actual (double (:enemy-speed metrics))]
            (support/assert-condition (= expected actual)
                                      (str "wave " wave " enemy speed "
                                           actual " expected " expected)))
          world)}

   {:pattern #"^wave <([A-Za-z0-9_]+)> enemy count is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param count-param] example]
          (let [wave (support/example-int example wave-param "wave")
                expected (support/example-int example count-param "enemy count")
                metrics (core/wave-schedule-metrics-for (:state world) wave)
                actual (:enemy-count metrics)]
            (support/assert-condition (= expected actual)
                                      (str "wave " wave " enemy count "
                                           actual " expected " expected)))
          world)}

   {:pattern #"^wave <([A-Za-z0-9_]+)> enemy speed is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ wave-param speed-param] example]
          (let [wave (support/example-int example wave-param "wave")
                expected (support/example-double example speed-param "enemy speed")
                metrics (core/wave-schedule-metrics-for (:state world) wave)
                actual (double (:enemy-speed metrics))]
            (support/assert-condition (= expected actual)
                                      (str "wave " wave " enemy speed "
                                           actual " expected " expected)))
          world)}])

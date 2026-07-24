(ns missile-command.acceptance.desktop-host-steps
  "Gherkin steps for desktop host docs, persistence, and arch check."
  (:require [clojure.string :as str]
            [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]
            [missile-command.options :as options]))

(def handlers
  [{:pattern #"^there are (\d+) non-destroyed batteries named left center and right$"
    :fn (fn [world [_ count-text] _]
          (let [expected (support/parse-int count-text "battery count")
                bats (filterv #(not (:destroyed? %)) (core/batteries (:state world)))]
            (support/assert-condition (= expected (count bats))
                                      (str "non-destroyed batteries " (count bats)
                                           " expected " expected))
            (support/assert-condition (= #{:left :center :right} (set (map :id bats)))
                                      (str "battery ids " (mapv :id bats))))
          world)}

   {:pattern #"^the player sets mute to (true|false)$"
    :fn (fn [world [_ mute-text] _]
          (assoc world :state
                 (core/set-mute
                  (:state world)
                  (options/parse-mute mute-text))))}

   {:pattern #"^the player sets difficulty to (easy|normal|arcade)$"
    :fn (fn [world [_ difficulty] _]
          (assoc world :state
                 (core/set-difficulty (:state world) difficulty)))}

   {:pattern #"^mute is (true|false)$"
    :fn (fn [world [_ mute-text] _]
          (let [expected (options/parse-mute mute-text)]
            (if (= :then (:gherkin-phase world))
              (let [actual (core/mute? (:state world))]
                (support/assert-condition (= expected actual)
                                          (str "mute " actual " expected " expected))
                world)
              (assoc world :state (core/set-mute (:state world) expected)))))}

   {:pattern #"^the difficulty is (easy|normal|arcade)$"
    :fn (fn [world [_ difficulty] _]
          (let [expected (options/parse-difficulty difficulty)
                actual (core/difficulty (:state world))]
            (support/assert-condition (= expected actual)
                                      (str "difficulty " actual
                                           " expected " expected)))
          world)}

   {:pattern #"^the documented desktop launch command is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ cmd-param] example]
          (let [expected (str/replace
                          (str (support/require-value example cmd-param))
                          #"_" " ")
                readme (slurp "README.md")]
            (support/assert-condition
             (str/includes? readme expected)
             (str "README missing desktop launch command: " expected)))
          world)}

   {:pattern #"^the desktop host options and high scores are persisted$"
    :fn (fn [world _ _]
          (assoc world :persisted-settings
                 (core/export-settings (:state world))))}

   {:pattern #"^the desktop host is restarted with width <([A-Za-z0-9_]+)> and height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ w-param h-param] example]
          (let [w (support/example-int example w-param "width")
                h (support/example-int example h-param "height")
                settings (or (:persisted-settings world) {})]
            (assoc world :state
                   (-> (core/new-game {:width w :height h})
                       (core/import-settings settings)))))}

   {:pattern #"^the architecture check passes for pure core isolation$"
    :fn (fn [world _ _]
          (let [proc (.. (ProcessBuilder.
                          ["bb" "arch-check"])
                         (redirectErrorStream true)
                         start)
                out (slurp (.getInputStream proc))
                code (.waitFor proc)]
            (support/assert-condition
             (zero? code)
             (str "architecture check failed (" code "): " out)))
          world)}])

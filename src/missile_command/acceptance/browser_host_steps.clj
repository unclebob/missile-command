(ns missile-command.acceptance.browser-host-steps
  "Gherkin steps for browser host docs and localStorage persistence path."
  (:require [clojure.string :as str]
            [missile-command.acceptance.step-support :as support]
            [missile-command.core :as core]))

(def handlers
  [{:pattern #"^the documented browser build command is <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ cmd-param] example]
          (let [expected (str/replace
                          (str (support/require-value example cmd-param))
                          #"_" " ")
                readme (slurp "README.md")]
            (support/assert-condition
             (str/includes? readme expected)
             (str "README missing browser build command: " expected)))
          world)}

   {:pattern #"^the documented browser open path is <([A-Za-z0-9_\\.]+)>$"
    :fn (fn [world [_ path-param] example]
          (let [expected (str (support/require-value example path-param))
                readme (slurp "README.md")]
            (support/assert-condition
             (str/includes? readme expected)
             (str "README missing browser open path: " expected)))
          world)}

   {:pattern #"^the browser host options and high scores are persisted to localStorage$"
    :fn (fn [world _ _]
          (assoc world :browser-persisted-settings
                 (core/export-settings (:state world))))}

   {:pattern #"^the browser host page is reloaded with width <([A-Za-z0-9_]+)> and height <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ w-param h-param] example]
          (let [w (support/example-int example w-param "width")
                h (support/example-int example h-param "height")
                settings (or (:browser-persisted-settings world) {})]
            (assoc world :state
                   (-> (core/new-game {:width w :height h})
                       (core/import-settings settings)))))}])

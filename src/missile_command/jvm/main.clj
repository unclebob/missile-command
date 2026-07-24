(ns missile-command.jvm.main
  "Documented JVM host entrypoint for manual / visual QA."
  (:require [missile-command.jvm.input :as input]
            [missile-command.jvm.sketch :as sketch]
            [missile-command.jvm.window :as window])
  (:gen-class))

(defn -main
  [& args]
  ;; Capture terminal screen + frontmost app *before* the sketch can steal focus.
  (let [prev-app (window/frontmost-app-name)
        anchor (window/capture-launch-anchor!)
        opts (-> (input/parse-cli-args args
                                       sketch/default-width
                                       sketch/default-height)
                 (assoc :launch-anchor anchor
                        :restore-focus-app prev-app))]
    (sketch/configure! opts)
    (sketch/run-sketch! (:width opts) (:height opts))))

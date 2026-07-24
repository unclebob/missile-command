(ns missile-command.jvm.main
  "Documented JVM host entrypoint for manual / visual QA."
  (:require [missile-command.jvm.input :as input]
            [missile-command.jvm.sketch :as sketch]
            [missile-command.jvm.window :as window])
  (:gen-class))

(defn -main
  [& args]
  ;; Capture terminal/frontmost window screen *before* the sketch opens.
  (let [anchor (window/capture-launch-anchor!)
        opts (-> (input/parse-cli-args args
                                       sketch/default-width
                                       sketch/default-height)
                 (assoc :launch-anchor anchor))]
    (sketch/configure! opts)
    (sketch/run-sketch! (:width opts) (:height opts))))

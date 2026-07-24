(ns missile-command.jvm.main
  "Documented JVM host entrypoint for manual / visual QA."
  (:require [missile-command.jvm.input :as input]
            [missile-command.jvm.sketch :as sketch])
  (:gen-class))

(defn -main
  [& args]
  (let [opts (input/parse-cli-args args
                                   sketch/default-width
                                   sketch/default-height)]
    (sketch/configure! opts)
    (sketch/run-sketch! (:width opts) (:height opts))))

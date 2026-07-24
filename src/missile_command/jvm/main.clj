(ns missile-command.jvm.main
  "Documented JVM host entrypoint for manual / visual QA."
  (:require [missile-command.jvm.input :as input]
            [missile-command.jvm.sketch :as sketch])
  (:gen-class))

(defn -main
  [& args]
  (let [[w h] (input/parse-window-size args
                                       sketch/default-width
                                       sketch/default-height)]
    (sketch/run-sketch! w h)))

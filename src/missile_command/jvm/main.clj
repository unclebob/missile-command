(ns missile-command.jvm.main
  "Documented JVM host entrypoint for manual / visual QA."
  (:gen-class))

(defn- request-non-activating-awt!
  [args]
  (when (some #{"--no-keyfocus"} args)
    (System/setProperty "apple.awt.UIElement" "true")
    (System/setProperty "apple.awt.noExtraMouseButtons" "true")))

(defn -main
  [& args]
  (request-non-activating-awt! args)
  (require '[missile-command.jvm.input :as input]
           '[missile-command.jvm.sketch :as sketch]
           '[missile-command.jvm.window :as window])
  ;; Capture terminal screen + frontmost app *before* the sketch can steal focus.
  (let [prev-app ((requiring-resolve 'missile-command.jvm.window/frontmost-app-name))
        anchor ((requiring-resolve 'missile-command.jvm.window/capture-launch-anchor!))
        default-width @(requiring-resolve 'missile-command.jvm.sketch/default-width)
        default-height @(requiring-resolve 'missile-command.jvm.sketch/default-height)
        opts (-> ((requiring-resolve 'missile-command.jvm.input/parse-cli-args)
                  args
                  default-width
                  default-height)
                 (assoc :launch-anchor anchor
                        :restore-focus-app prev-app))]
    ((requiring-resolve 'missile-command.jvm.sketch/configure!) opts)
    ((requiring-resolve 'missile-command.jvm.sketch/run-sketch!) (:width opts) (:height opts))))

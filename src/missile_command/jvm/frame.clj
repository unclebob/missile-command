(ns missile-command.jvm.frame
  "JVM host frame timing and fixed-substep policy.")

(def max-frame-seconds 0.25)
(def epsilon 1.0e-12)

(defn dt-seconds
  "Return clamped wall-clock dt, multiplied by speed."
  [now-ms prev-ms speed]
  (let [raw (/ (double (- now-ms prev-ms)) 1000.0)
        wall (max 0.0 (min raw max-frame-seconds))]
    (* wall (double (or speed 1.0)))))

(defn next-dt!
  [last-frame-ms opts]
  (let [now (System/currentTimeMillis)
        prev (or @last-frame-ms now)]
    (reset! last-frame-ms now)
    (dt-seconds now prev (:qa-speed opts))))

(defn advance-substeps
  "Advance state by budget seconds, using advance-one-step for each substep.
  advance-one-step returns [state completed?]."
  [state budget step-max advance-one-step]
  (loop [s state
         remaining budget
         completed-any? false]
    (if (<= remaining epsilon)
      [s completed-any?]
      (let [step (min remaining (double step-max))
            [s' completed?] (advance-one-step s step)]
        (recur s' (- remaining step) (or completed-any? completed?))))))

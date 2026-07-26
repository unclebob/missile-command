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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:25:47.524413-05:00", :module-hash "1420802958", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "407399086"} {:id "def/max-frame-seconds", :kind "def", :line 4, :end-line 4, :hash "1836813850"} {:id "def/epsilon", :kind "def", :line 5, :end-line 5, :hash "-790353900"} {:id "defn/dt-seconds", :kind "defn", :line 7, :end-line 12, :hash "447395772"} {:id "defn/next-dt!", :kind "defn", :line 14, :end-line 19, :hash "-1682625203"} {:id "defn/advance-substeps", :kind "defn", :line 21, :end-line 32, :hash "1057908267"}]}
;; clj-mutate-manifest-end

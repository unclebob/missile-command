#!/usr/bin/env bb
;; Executable QA for fire-click-zone (aligned with qa/procedures/fire-click-zone.qa.md).
;; Automated tests via documented README commands; host UI via bb play + --qa-events.

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(def procedure-path "qa/procedures/fire-click-zone.qa.md")
(def readme-path "README.md")
(def required-features ["features/fire-click-zone.feature"])

(defn die!
  [msg]
  (binding [*out* *err*]
    (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert!
  [ok? msg]
  (when-not ok?
    (die! msg)))

(defn readme [] (slurp readme-path))

(defn discover-unit-command [doc]
  (when-let [[_ cmd] (re-find #"(?ms)^### Unit tests\s*```sh\s*\n([^\n`]+)\n```" doc)]
    (str/trim cmd)))

(defn discover-accept-command [doc]
  (when-let [[_ cmd] (re-find #"(?ms)^### Acceptance tests[^\n]*\n+```sh\s*\n([^\n`]+)\n```" doc)]
    (str/trim cmd)))

(defn discover-arch-command [doc]
  (when-let [[_ cmd] (re-find #"(?ms)^### Architecture check\s*```sh\s*\n([^\n`]+)\n```" doc)]
    (str/trim cmd)))

(defn discover-launch-command [doc]
  (when-let [[_ cmd] (re-find #"(?ms)^### Launch[^\n]*\n+```sh\s*\n([^\n`]+)\n```" doc)]
    (str/trim cmd)))

(defn run-documented! [label cmd]
  (println (str "==> " label ": " cmd))
  (let [result (p/shell {:out :string :err :string :continue true} "bash" "-lc" cmd)
        out (str (:out result) (:err result))]
    (print out)
    (flush)
    {:exit (:exit result) :out out}))

(defn no-failures? [out]
  (and (not (re-find #"(?i)\b[1-9]\d*\s+failures?\b" out))
       (not (re-find #"(?i)\b[1-9]\d*\s+errors?\b" out))
       (not (re-find #"(?i)Architecture check FAILED" out))))

(defn parse-telemetry [out]
  (->> (str/split-lines out)
       (filter #(str/starts-with? (str/trim %) "qa-fire "))
       vec))

(defn field [line key]
  (when-let [[_ v] (re-find (re-pattern (str key "=([^\\s]+)")) line)]
    v))

(defn write-events! [path lines]
  (io/make-parents path)
  (spit path (str (str/join "\n" lines) "\n")))

(defn launch-with-events!
  "Run bb play with telemetry and optional destroy list + event file."
  [{:keys [events destroy width height timeout-ms]
    :or {timeout-ms 60000 width 900 height 600}}]
  (let [events-path "tmp/qa-fire-click-events.txt"
        _ (write-events! events-path (concat events ["quit"]))
        destroy-part (when (seq destroy)
                       (str "--destroy-batteries " destroy " "))
        cmd (str "bb play "
                 width " " height " "
                 "--qa-telemetry "
                 destroy-part
                 "--qa-events " events-path)]
    (println "==> host:" cmd)
    (let [result (p/shell {:out :string :err :string :continue true
                           :timeout timeout-ms}
                          "bash" "-lc" cmd)
          out (str (:out result) (:err result))]
      (print out)
      (flush)
      {:exit (:exit result) :out out :telemetry (parse-telemetry out)})))

(defn last-vector
  "Newest missile vector is the last origin/target group on the line."
  [line]
  (let [pairs (re-seq #"origin_x=(\S+)\s+origin_y=(\S+)\s+target_x=(\S+)\s+target_y=(\S+)" line)]
    (when (seq pairs)
      (let [[_ ox oy tx ty] (last pairs)]
        {:origin-x ox :origin-y oy :target-x tx :target-y ty}))))

(defn expect-telemetry!
  [lines idx battery-re target-x target-y]
  (assert! (< idx (count lines))
           (str "missing telemetry line index " idx " have " (count lines)))
  (let [line (nth lines idx)
        bat (field line "battery")
        vec (last-vector line)]
    (assert! (re-find battery-re (str bat))
             (str "expected battery matching " battery-re " got " bat " in " line))
    (when target-x
      (assert! vec (str "missing flight vector in " line))
      (assert! (= (str target-x) (str (:target-x vec)))
               (str "expected target_x=" target-x " got " (:target-x vec) " in " line)))
    (when target-y
      (assert! vec (str "missing flight vector in " line))
      (assert! (= (str target-y) (str (:target-y vec)))
               (str "expected target_y=" target-y " got " (:target-y vec) " in " line)))
    line))

(defn -main
  [& _]
  (assert! (.exists (io/file "bb.edn")) "run from project root")
  (assert! (.exists (io/file procedure-path)) (str "missing " procedure-path))
  (doseq [f required-features]
    (assert! (.exists (io/file f)) (str "missing " f)))
  (let [doc (readme)
        unit-cmd (discover-unit-command doc)
        accept-cmd (discover-accept-command doc)
        arch-cmd (discover-arch-command doc)
        launch-cmd (discover-launch-command doc)]
    (assert! (seq unit-cmd) "README missing unit command")
    (assert! (seq accept-cmd) "README missing accept command")
    (assert! (seq launch-cmd) "README missing launch command")
    (assert! (re-find #"(?m)--qa-telemetry" doc) "README missing --qa-telemetry")
    (assert! (re-find #"(?m)--destroy-batteries" doc) "README missing --destroy-batteries")
    (assert! (re-find #"(?m)--qa-events" doc) "README missing --qa-events")
    (println "Discovered unit:" unit-cmd)
    (println "Discovered accept:" accept-cmd)
    (println "Discovered launch:" launch-cmd)

    ;; A. unit / accept / arch
    (let [unit (run-documented! "unit" unit-cmd)
          accept (run-documented! "accept" accept-cmd)
          arch (when (seq arch-cmd) (run-documented! "arch" arch-cmd))]
      (assert! (zero? (:exit unit)) (str "unit exit " (:exit unit)))
      (assert! (no-failures? (:out unit)) "unit failures")
      (assert! (zero? (:exit accept)) (str "accept exit " (:exit accept)))
      (assert! (no-failures? (:out accept)) "accept failures")
      (assert! (re-find #"(?i)fire[-_]?click[-_]?zone" (:out accept))
               "accept missing fire-click-zone coverage")
      (when arch
        (assert! (zero? (:exit arch)) (str "arch exit " (:exit arch)))))

    ;; B1. stocked batteries — click each horizontal third
    (let [r (launch-with-events!
             {:events ["click 100 150" "click 450 150" "click 800 150"]
              :width 900 :height 600})
          t (:telemetry r)]
      (assert! (= 3 (count t)) (str "expected 3 telemetry lines, got " t "\n" (:out r)))
      (expect-telemetry! t 0 #"left" 100 150)
      (expect-telemetry! t 1 #"center" 450 150)
      (expect-telemetry! t 2 #"right" 800 150))

    ;; B2. empty left via keys, click left third falls back to center; empty key is none
    (let [empty-left (vec (concat ["aim 400 200"] (repeat 10 "key 1")))
          r (launch-with-events!
             {:events (concat empty-left
                              ["key 1"           ; none
                               "click 100 160"   ; center fallback
                               "aim 500 200"
                               "key 2"])         ; center key still works
              :width 900 :height 600})
          t (:telemetry r)]
      (assert! (>= (count t) 12) (str "empty-left session short: " (count t) "\n" (:out r)))
      (assert! (re-find #"battery=none" (nth t 10))
               (str "11th left key should be none: " (nth t 10)))
      (expect-telemetry! t 11 #"center" 100 160)
      (assert! (re-find #"battery=center" (nth t 12))
               (str "center key fire: " (nth t 12)))
      (assert! (re-find #"target_x=500" (nth t 12))
               (str "center key should aim 500,200: " (nth t 12))))

    ;; C. destroyed batteries via CLI
    (let [r (launch-with-events!
             {:destroy "left"
              :events ["key 1"           ; destroyed left -> none
                       "click 100 150"]  ; left third -> center
              :width 900 :height 600})
          t (:telemetry r)]
      (assert! (>= (count t) 2) (str "destroy-left telemetry short: " t))
      (assert! (re-find #"battery=none" (first t)) (str "key left destroyed: " (first t)))
      (expect-telemetry! t 1 #"center" 100 150))

    (let [r (launch-with-events!
             {:destroy "center"
              :events ["key 2"
                       "click 450 150"]
              :width 900 :height 600})
          t (:telemetry r)]
      (assert! (re-find #"battery=none" (first t)) (str "key center destroyed: " (first t)))
      (assert! (re-find #"battery=left" (second t)) (str "center click fallback: " (second t)))
      (assert! (re-find #"target_x=450" (second t)) (str "center click target: " (second t))))

    (let [r (launch-with-events!
             {:destroy "left,center"
              :events ["click 100 150"]
              :width 900 :height 600})
          t (:telemetry r)]
      (expect-telemetry! t 0 #"right" 100 150))

    (let [r (launch-with-events!
             {:destroy "left,center,right"
              :events ["click 450 150" "key 1"]
              :width 900 :height 600})
          t (:telemetry r)]
      (assert! (every? #(re-find #"battery=none" %) t)
               (str "all destroyed expected none: " t)))

    (println)
    (println "PASS: fire-click-zone automated QA (procedures A–C)")
    (println "MANUAL PENDING: procedure D look-and-feel requires explicit user approval")
    (System/exit 0)))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

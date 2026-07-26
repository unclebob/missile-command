(require '[babashka.process :as p]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(defn die!
  [msg]
  (binding [*out* *err*]
    (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert!
  [ok? msg]
  (when-not ok?
    (die! msg)))

(defn field
  [line key]
  (when-let [[_ v] (re-find (re-pattern (str key "=([^\\s]+)")) line)]
    v))

(defn fields
  [line key]
  (map second (re-seq (re-pattern (str key "=([^\\s]+)")) line)))

(defn long-field
  [line key]
  (when-let [v (field line key)]
    (Long/parseLong v)))

(defn double-field
  [line key]
  (when-let [v (field line key)]
    (Double/parseDouble v)))

(defn lines-starting
  [prefix out]
  (->> (str/split-lines out)
       (map str/trim)
       (filter #(str/starts-with? % prefix))
       vec))

(defn sims
  [out]
  (lines-starting "qa-sim " out))

(defn sfx-lines
  [out]
  (lines-starting "qa-sfx " out))

(defn qa-fire-lines
  [out]
  (lines-starting "qa-fire " out))

(defn write-edn!
  [path data]
  (io/make-parents path)
  (spit path (pr-str data)))

(defn write-events!
  [path lines]
  (io/make-parents path)
  (spit path (str (str/join "\n" lines) "\n")))

(defn run!
  [label cmd]
  (println (str "==> " label ": " cmd))
  (flush)
  (let [r (p/shell {:out :string :err :string :continue true} "bash" "-lc" cmd)
        out (str (:out r) (:err r))]
    (print out)
    (flush)
    {:exit (:exit r) :out out}))

(defn launch-cmd
  [{:keys [width height qa-speed qa-telemetry? no-keyfocus? scenario-path events-path
           scores-path destroy-batteries extra]
    :or {width 800 height 600 qa-speed 8 no-keyfocus? true}}]
  (str "bb play " width " " height
       (if qa-telemetry? " --qa-telemetry" " --qa")
       (when no-keyfocus? " --no-keyfocus")
       (when qa-speed (str " --qa-speed " qa-speed))
       (when scores-path (str " --scores-file " scores-path))
       (when scenario-path (str " --qa-scenario " scenario-path))
       (when destroy-batteries (str " --destroy-batteries " destroy-batteries))
       (when events-path (str " --qa-events " events-path))
       (when extra (str " " extra))))

(defn launch!
  [{:keys [timeout-ms include-sfx? include-fire?]
    :or {timeout-ms 45000}
    :as opts}]
  (let [cmd (launch-cmd opts)]
    (println "==> host:" cmd)
    (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out)
      (flush)
      (cond-> {:exit (:exit r) :out out :sims (sims out)}
        include-sfx? (assoc :sfx (sfx-lines out))
        include-fire? (assoc :telemetry (qa-fire-lines out))))))

(defn readme
  []
  (slurp "README.md"))

(defn discover-unit-command
  [doc]
  (when-let [[_ cmd] (re-find #"(?ms)^### Unit tests\s*```sh\s*\n([^\n`]+)\n```" doc)]
    (str/trim cmd)))

(defn discover-accept-command
  [doc]
  (when-let [[_ cmd] (re-find #"(?ms)^### Acceptance tests[^\n]*\n+```sh\s*\n([^\n`]+)\n```" doc)]
    (str/trim cmd)))

(defn discover-arch-command
  [doc]
  (when-let [[_ cmd] (re-find #"(?ms)^### Architecture check\s*```sh\s*\n([^\n`]+)\n```" doc)]
    (str/trim cmd)))

(defn discover-launch-command
  [doc]
  (when-let [[_ cmd] (re-find #"(?ms)^### Launch[^\n]*\n+```sh\s*\n([^\n`]+)\n```" doc)]
    (str/trim cmd)))

(defn no-failures?
  [out]
  (and (not (re-find #"(?i)\b[1-9]\d*\s+failures?\b" out))
       (not (re-find #"(?i)\b[1-9]\d*\s+errors?\b" out))
       (not (re-find #"(?i)Architecture check FAILED" out))))

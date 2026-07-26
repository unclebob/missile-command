#!/usr/bin/env bb
;; Executable QA for reset-scenario event batching.

(load-file "qa/scripts/lib/common.bb")

(defn -main
  [& _]
  (assert! (.exists (io/file "qa/procedures/reset-scenario.qa.md")) "missing procedure")
  (let [doc (readme)]
    (assert! (re-find #"reset-scenario <file>" doc) "README missing reset-scenario event"))

  (write-edn! "tmp/reset-a.edn" {:screen :playing :wave 2})
  (write-edn! "tmp/reset-b.edn" {:screen :playing :wave 5})
  (write-events! "tmp/reset-events.txt"
                 ["wait 0.1" "reset-scenario tmp/reset-b.edn" "wait 0.1" "quit"])

  (let [r (launch! {:scenario-path "tmp/reset-a.edn"
                    :events-path "tmp/reset-events.txt"
                    :scores-path "tmp/reset-scores.edn"
                    :timeout-ms 45000})
        all (:sims r)
        before (first (filter #(and (= "playing" (field % "screen"))
                                    (= "2" (field % "wave")))
                              all))
        after (first (filter #(and (= "playing" (field % "screen"))
                                   (= "5" (field % "wave")))
                             (drop-while #(not= before %) all)))]
    (assert! before (str "never saw initial scenario wave 2: " (map #(field % "wave") all)))
    (assert! after (str "never saw reset scenario wave 5: " (map #(field % "wave") all))))

  (println "\nPASS: reset-scenario automated QA")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

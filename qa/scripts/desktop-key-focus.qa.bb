#!/usr/bin/env bb
;; Executable QA for desktop-key-focus. macOS focus check plus host launch.

(require '[babashka.process :as p]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(defn die! [msg]
  (binding [*out* *err*] (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert! [ok? msg] (when-not ok? (die! msg)))

(defn run!
  [label & cmd]
  (println (str "==> " label ": " (str/join " " cmd))) (flush)
  (let [r (apply p/shell {:out :string :err :string :continue true} cmd)
        out (str (:out r) (:err r))]
    (print out) (flush)
    {:exit (:exit r) :out out}))

(defn osascript
  [source]
  (let [r (p/shell {:out :string :err :string :continue true}
                   "osascript" "-e" source)]
    (when (zero? (:exit r))
      (str/trim (:out r)))))

(defn frontmost-app
  []
  (osascript
   "tell application \"System Events\" to get name of first application process whose frontmost is true"))

(defn write-events!
  [path]
  (io/make-parents path)
  (spit path "wait 0.8\nquit\n"))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/desktop-key-focus.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/desktop-key-focus.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (str/includes? readme "--no-keyfocus") "README missing --no-keyfocus"))

  (assert! (= "Mac OS X" (System/getProperty "os.name"))
           "desktop focus automation currently requires macOS")
  (assert! (frontmost-app) "cannot read frontmost app through System Events")

  (write-events! "tmp/desktop-key-focus-events.txt")
  (let [before (frontmost-app)
        proc (p/process ["bb" "play" "800" "600" "--qa" "--no-keyfocus"
                         "--qa-events" "tmp/desktop-key-focus-events.txt"]
                        {:out :string :err :string})
        _ (Thread/sleep 900)
        during (frontmost-app)
        result @proc
        out (str (:out result) (:err result))]
    (print out) (flush)
    (assert! (= before during)
             (str "focus changed from " before " to " during))
    (assert! (zero? (:exit result))
             (str "host exited " (:exit result))))

  (println "\nPASS: desktop-key-focus automated QA"))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

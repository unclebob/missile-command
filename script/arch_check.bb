#!/usr/bin/env bb
;; Lightweight architecture boundary check for Missile Command.
;; Pure core (.cljc game rules) must not depend on hosts, acceptance, or IO libs.

(require '[babashka.fs :as fs]
         '[clojure.string :as str])

(def src-root "src")

(def forbidden-require-prefixes
  ["quil."
   "missile-command.acceptance."
   "missile-command.jvm."
   "missile-command.browser."
   "clojure.java.io"
   "clojure.data.json"
   "babashka."
   "java.io"
   "java.nio"])

;; Acceptance adapters must depend on the core public API, not layout internals.
(def acceptance-forbidden-require-prefixes
  ["missile-command.world"
   "quil."
   "missile-command.jvm."
   "missile-command.browser."])

;; The JVM input facade is testable host mapping. Keep it independent from
;; rendering, window placement, sound, and persistence adapters.
(def jvm-input-forbidden-require-prefixes
  ["quil."
   "missile-command.jvm.audio"
   "missile-command.jvm.persist"
   "missile-command.jvm.render"
   "missile-command.jvm.sketch"
   "missile-command.jvm.window"
   "java.awt"
   "javax.swing"])

(def jvm-production-host-forbidden-require-prefixes
  ["missile-command.testing"])

(def host-input-forbidden-source-patterns
  [#"\bjs/"
   #"\bq/"
   #"\bquil\."
   #"\bmissile-command\.jvm\."
   #"\bmissile-command\.browser\."
   #"\bmissile-command\.acceptance\."
   #"\bclojure\.java\.io\b"
   #"\bjava\.awt\b"
   #"\bjavax\.swing\b"])

(defn- balanced-form
  [content start]
  (loop [i (inc start) depth 1]
    (if (>= i (count content))
      nil
      (let [c (nth content i)]
        (cond
          (= c \() (recur (inc i) (inc depth))
          (and (= c \)) (= depth 1)) (subs content start (inc i))
          (= c \)) (recur (inc i) (dec depth))
          :else (recur (inc i) depth))))))

(defn- require-block
  [content]
  (when-let [start (str/index-of content "(:require")]
    (balanced-form content start)))

(defn- read-ns-and-requires
  [path]
  (let [content (slurp path)
        ns-m (re-find #"\(ns\s+([A-Za-z][A-Za-z0-9_.-]*)" content)
        req-block (require-block content)]
    {:path path
     :ns (some-> ns-m second)
     :requires (when req-block
                 (->> (re-seq #"(?:\[|\s)([A-Za-z][A-Za-z0-9_.-]*(?:\.[A-Za-z0-9_.-]+)+)"
                              req-block)
                      (map second)
                      distinct
                      vec))}))

(defn- pure-core-files
  []
  (->> (fs/glob src-root "**/*.cljc")
       (map str)
       (filter #(str/includes? % "/missile_command/"))
       (remove #(or (str/includes? % "/acceptance/")
                    (str/includes? % "/jvm/")
                    (str/includes? % "/browser/")))
       sort))

(defn- acceptance-files
  []
  (->> (fs/glob src-root "**/*.clj")
       (map str)
       (filter #(str/includes? % "/missile_command/acceptance/"))
       sort))

(defn- jvm-input-files
  []
  (->> ["src/missile_command/jvm/input.clj"
        "src/missile_command/jvm/cli.clj"
        "src/missile_command/jvm/telemetry.clj"]
       (filter fs/exists?)
       sort))

(defn- jvm-production-host-files
  []
  (->> (fs/glob src-root "missile_command/jvm/*.clj")
       (map str)
       (remove #(or (str/ends-with? % "/scenario.clj")
                    (str/ends-with? % "/input.clj")
                    (str/ends-with? % "/cli.clj")
                    (str/ends-with? % "/telemetry.clj")))
       sort))

(defn- host-input-files
  []
  (->> ["src/missile_command/host_input.cljc"]
       (filter fs/exists?)
       sort))

(defn- forbidden-require?
  [req prefixes]
  (some #(or (= req %) (str/starts-with? req (str % ".")) (str/starts-with? req %))
        prefixes))

(defn- violations-for
  [{:keys [path ns requires]} prefixes]
  (for [req (or requires [])
        :when (forbidden-require? req prefixes)]
    {:path path :ns ns :require req}))

(defn- report-section!
  [label violations]
  (when (seq violations)
    (println (str "Architecture check FAILED: " label))
    (doseq [{:keys [path ns require]} violations]
      (println (str "  " path " (" ns ") requires " require)))))

(defn- source-pattern-violations
  [paths patterns]
  (for [path paths
        :let [content (slurp path)]
        pattern patterns
        :when (re-find pattern content)]
    {:path path :pattern pattern}))

(defn- report-pattern-section!
  [label violations]
  (when (seq violations)
    (println (str "Architecture check FAILED: " label))
    (doseq [{:keys [path pattern]} violations]
      (println (str "  " path " matches " pattern)))))

(let [core-files (pure-core-files)
      acceptance (acceptance-files)
      jvm-input (jvm-input-files)
      jvm-production-host (jvm-production-host-files)
      host-input (host-input-files)]
  (when (empty? core-files)
    (println "Architecture check FAILED: no pure core .cljc files under src/missile_command")
    (System/exit 1))
  (let [core-violations (->> core-files
                             (map read-ns-and-requires)
                             (mapcat #(violations-for % forbidden-require-prefixes)))
        acceptance-violations (->> acceptance
                                   (map read-ns-and-requires)
                                   (mapcat #(violations-for % acceptance-forbidden-require-prefixes)))
        jvm-input-violations (->> jvm-input
                                  (map read-ns-and-requires)
                                  (mapcat #(violations-for % jvm-input-forbidden-require-prefixes)))
        jvm-production-host-violations
        (->> jvm-production-host
             (map read-ns-and-requires)
             (mapcat #(violations-for % jvm-production-host-forbidden-require-prefixes)))
        host-input-source-violations
        (source-pattern-violations host-input host-input-forbidden-source-patterns)]
    (if (or (seq core-violations)
            (seq acceptance-violations)
            (seq jvm-input-violations)
            (seq jvm-production-host-violations)
            (seq host-input-source-violations))
      (do
        (report-section! "pure core depends on forbidden namespaces" core-violations)
        (report-section! "acceptance depends on non-core internals/hosts"
                         acceptance-violations)
        (report-section! "JVM input facade depends on low-level host adapters"
                         jvm-input-violations)
        (report-section! "JVM production hosts depend on testing helpers"
                         jvm-production-host-violations)
        (report-pattern-section! "shared host input policy contains platform code"
                                 host-input-source-violations)
        (System/exit 1))
      (do
        (println "Architecture check OK")
        (println (str "  pure core files: " (count core-files)))
        (println (str "  acceptance files: " (count acceptance)))
        (println (str "  JVM input facade files: " (count jvm-input)))
        (println (str "  JVM production host files: " (count jvm-production-host)))
        (println (str "  shared host input policy files: " (count host-input)))
        (System/exit 0)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:26:01.698527-05:00", :module-hash "827440170", :forms [{:id "form/0/require", :kind "require", :line 5, :end-line 6, :hash "-1553196168"} {:id "def/src-root", :kind "def", :line 8, :end-line 8, :hash "1665740148"} {:id "def/forbidden-require-prefixes", :kind "def", :line 10, :end-line 19, :hash "1637753462"} {:id "def/acceptance-forbidden-require-prefixes", :kind "def", :line 22, :end-line 26, :hash "1569275813"} {:id "def/jvm-input-forbidden-require-prefixes", :kind "def", :line 30, :end-line 38, :hash "348784039"} {:id "def/jvm-production-host-forbidden-require-prefixes", :kind "def", :line 40, :end-line 41, :hash "1174037563"} {:id "def/host-input-forbidden-source-patterns", :kind "def", :line 43, :end-line 52, :hash "-1849294276"} {:id "defn-/balanced-form", :kind "defn-", :line 54, :end-line 64, :hash "-1660612329"} {:id "defn-/require-block", :kind "defn-", :line 66, :end-line 69, :hash "377659612"} {:id "defn-/read-ns-and-requires", :kind "defn-", :line 71, :end-line 83, :hash "269821848"} {:id "defn-/pure-core-files", :kind "defn-", :line 85, :end-line 93, :hash "1260643675"} {:id "defn-/acceptance-files", :kind "defn-", :line 95, :end-line 100, :hash "1849056367"} {:id "defn-/jvm-input-files", :kind "defn-", :line 102, :end-line 108, :hash "1395504156"} {:id "defn-/jvm-production-host-files", :kind "defn-", :line 110, :end-line 118, :hash "873000106"} {:id "defn-/host-input-files", :kind "defn-", :line 120, :end-line 124, :hash "1161570630"} {:id "defn-/forbidden-require?", :kind "defn-", :line 126, :end-line 129, :hash "-2057218087"} {:id "defn-/violations-for", :kind "defn-", :line 131, :end-line 135, :hash "398228845"} {:id "defn-/report-section!", :kind "defn-", :line 137, :end-line 142, :hash "-938263561"} {:id "defn-/source-pattern-violations", :kind "defn-", :line 144, :end-line 150, :hash "-116863786"} {:id "defn-/report-pattern-section!", :kind "defn-", :line 152, :end-line 157, :hash "-1989822893"} {:id "form/20/let", :kind "let", :line 159, :end-line 205, :hash "1074871494"}]}
;; clj-mutate-manifest-end

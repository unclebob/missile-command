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

(let [core-files (pure-core-files)
      acceptance (acceptance-files)
      jvm-input (jvm-input-files)]
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
                                  (mapcat #(violations-for % jvm-input-forbidden-require-prefixes)))]
    (if (or (seq core-violations)
            (seq acceptance-violations)
            (seq jvm-input-violations))
      (do
        (report-section! "pure core depends on forbidden namespaces" core-violations)
        (report-section! "acceptance depends on non-core internals/hosts"
                         acceptance-violations)
        (report-section! "JVM input facade depends on low-level host adapters"
                         jvm-input-violations)
        (System/exit 1))
      (do
        (println "Architecture check OK")
        (println (str "  pure core files: " (count core-files)))
        (println (str "  acceptance files: " (count acceptance)))
        (println (str "  JVM input facade files: " (count jvm-input)))
        (System/exit 0)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:04:23.464696-05:00", :module-hash "2101988818", :forms [{:id "form/0/require", :kind "require", :line 5, :end-line 6, :hash "-1553196168"} {:id "def/src-root", :kind "def", :line 8, :end-line 8, :hash "1665740148"} {:id "def/forbidden-require-prefixes", :kind "def", :line 10, :end-line 19, :hash "1637753462"} {:id "def/acceptance-forbidden-require-prefixes", :kind "def", :line 22, :end-line 26, :hash "1569275813"} {:id "def/jvm-input-forbidden-require-prefixes", :kind "def", :line 30, :end-line 38, :hash "348784039"} {:id "defn-/balanced-form", :kind "defn-", :line 40, :end-line 50, :hash "-1660612329"} {:id "defn-/require-block", :kind "defn-", :line 52, :end-line 55, :hash "377659612"} {:id "defn-/read-ns-and-requires", :kind "defn-", :line 57, :end-line 69, :hash "269821848"} {:id "defn-/pure-core-files", :kind "defn-", :line 71, :end-line 79, :hash "1260643675"} {:id "defn-/acceptance-files", :kind "defn-", :line 81, :end-line 86, :hash "1849056367"} {:id "defn-/jvm-input-files", :kind "defn-", :line 88, :end-line 94, :hash "1395504156"} {:id "defn-/forbidden-require?", :kind "defn-", :line 96, :end-line 99, :hash "1212181509"} {:id "defn-/violations-for", :kind "defn-", :line 101, :end-line 105, :hash "398228845"} {:id "defn-/report-section!", :kind "defn-", :line 107, :end-line 112, :hash "-938263561"} {:id "form/14/let", :kind "let", :line 114, :end-line 144, :hash "-1455508239"}]}
;; clj-mutate-manifest-end

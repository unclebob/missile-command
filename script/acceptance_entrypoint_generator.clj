#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[babashka.process :refer [shell]]
         '[clojure.string :as str])

(defn usage! []
  (println "usage: acceptance-entrypoint-generator <json-ir> <generated-test-output>")
  (System/exit 2))

(when-not (= 2 (count *command-line-args*))
  (usage!))

(let [[ir-path output-dir] *command-line-args*]
  (when (or (not (fs/exists? ir-path)) (str/blank? output-dir))
    (println "error: missing input ir or output directory")
    (System/exit 1))

  (let [ir-name (fs/strip-ext (fs/file-name ir-path))
        feature-path (or (first (map str (fs/glob "features" (str "**/" ir-name ".feature"))))
                         (str "features/" ir-name ".feature"))
        metadata-base (-> feature-path
                          str/lower-case
                          (str/replace #"[^a-z0-9]+" "-")
                          (str/replace #"^-|-$" ""))
        slug (str/replace metadata-base "-" "_")
        test-file-name (str slug "_test.clj")
        test-ns (str metadata-base "-test")
        test-path (fs/path output-dir test-file-name)
        metadata-dir (fs/path output-dir "metadata")
        metadata-path (fs/path metadata-dir (str metadata-base ".json"))
        test-source (str "(ns " test-ns "\n"
                         "  (:require [clojure.test :refer [deftest is]]\n"
                         "            [missile-command.acceptance.runtime :as runtime]))\n\n"
                         "(deftest " slug "_acceptance\n"
                         "  (doseq [{:keys [pass name index error]} (runtime/run-feature-file \"" ir-path "\")]\n"
                         "    (is pass (str name \" example_\" (inc index) \": \" error))))\n")]
    (fs/create-dirs output-dir)
    (fs/create-dirs metadata-dir)
    (spit (str test-path) test-source)
    (let [hash-input (:out (shell {:out :string} "sh" "-c" (str "shasum -a 256 " (pr-str (str test-path)))))
          implementation-hash (str "sha256:" (first (str/split hash-input #"\s+")))]
      (spit (str metadata-path)
            (str "{\n"
                 "  \"schema_version\": 1,\n"
                 "  \"feature_path\": " (pr-str feature-path) ",\n"
                 "  \"ir_path\": " (pr-str ir-path) ",\n"
                 "  \"implementation_hash\": " (pr-str implementation-hash) ",\n"
                 "  \"hash_scope\": \"generated_files\",\n"
                 "  \"generated_files\": [" (pr-str (str "acceptance/generated/" test-file-name)) "]\n"
                 "}\n"))
      (println "generated" test-path)
      (System/exit 0))))

(ns missile-command.generated-artifacts-policy
  "Generated build outputs that are disposable and never source-controlled.")

(def disposable-paths
  ["acceptance/generated"
   "build/acceptance"
   "resources/public/js"])

(def disposable-cache-paths
  [".cpcache"
   ".shadow-cljs"])

(defn all-disposable-paths
  []
  (concat disposable-paths disposable-cache-paths))

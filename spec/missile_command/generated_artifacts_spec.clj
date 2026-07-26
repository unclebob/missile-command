(ns missile-command.generated-artifacts-spec
  (:require [speclj.core :refer :all]
            [missile-command.generated-artifacts-policy :as generated-artifacts-policy]))

(describe "generated artifact policy"
  (it "names the disposable outputs produced by normal local tasks"
    (should= ["acceptance/generated"
              "build/acceptance"
              "resources/public/js"]
             generated-artifacts-policy/disposable-paths))

  (it "names local tool caches separately from generated build outputs"
    (should= [".cpcache" ".shadow-cljs"]
             generated-artifacts-policy/disposable-cache-paths)))

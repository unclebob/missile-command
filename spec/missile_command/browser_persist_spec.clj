(ns missile-command.browser-persist-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.browser.persist :as persist]))

(describe "browser settings encoding"
  (it "encodes and decodes export blobs"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-mute true)
                    (core/set-difficulty :normal))
          raw (persist/encode (core/export-settings state))
          settings (persist/decode raw)]
      (should (string? raw))
      (should (true? (get-in settings [:options :mute])))
      (should= :normal (get-in settings [:options :difficulty]))))

  (it "round-trips through the in-memory JVM store used by tests"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-mute true)
                    (core/set-difficulty :easy))
          _ (persist/save-settings! state)
          restored (-> (core/new-game {:width 800 :height 600})
                       persist/load-into)]
      (should (core/mute? restored))
      (should= :easy (core/difficulty restored)))))

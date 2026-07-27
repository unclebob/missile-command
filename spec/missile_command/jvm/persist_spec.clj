(ns missile-command.jvm.persist-spec
  (:require [speclj.core :refer :all]
            [clojure.java.io :as io]
            [missile-command.core :as core]
            [missile-command.jvm.persist :as persist]))

(describe "desktop settings persistence"
  (it "round-trips mute difficulty and high scores through a file"
    (let [path (.getAbsolutePath
                (io/file "tmp" (str "persist-spec-" (System/nanoTime) ".edn")))
          state (-> (core/new-game {:width 800 :height 600})
                    (core/set-mute true)
                    (core/set-difficulty :easy)
                    (core/add-high-score-entry "ACE" 1200))
          _ (persist/save-settings! state path)
          restored (-> (core/new-game {:width 800 :height 600})
                       (persist/load-into path))]
      (should (core/mute? restored))
      (should= :easy (core/difficulty restored))
      (should= "ACE" (:initials (first (core/high-score-table restored))))
      (should= 1200 (:score (first (core/high-score-table restored))))
      (.delete (io/file path))))

  (it "preserves host-owned settings while saving core settings"
    (let [path (.getAbsolutePath
                (io/file "tmp" (str "persist-spec-" (System/nanoTime) ".edn")))
          state (-> (core/new-game {:width 800 :height 600})
                    (core/set-mute true))]
      (spit path (pr-str {:local-player-code "AB12CD"}))
      (persist/save-settings! state path)
      (should= "AB12CD" (:local-player-code (persist/load-settings path)))
      (should= true (:mute (:options (persist/load-settings path))))
      (.delete (io/file path)))))

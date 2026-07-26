(ns missile-command.jvm.telemetry-emitter-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.jvm.telemetry-emitter :as telemetry-emitter]))

(describe "sfx-line"
  (it "formats namespaced events without changing QA field names"
    (should= "qa-sfx type=impact/city played=true mute=false"
             (telemetry-emitter/sfx-line {:type :impact/city} false)))

  (it "reports muted playback"
    (should= "qa-sfx type=launch played=false mute=true"
             (telemetry-emitter/sfx-line {:type :launch} true))))

(describe "emit-new-sfx!"
  (it "advances the host cursor so old sounds are not replayed"
    (let [played (atom [])
          cursor (atom 0)
          launch-options (atom {:qa-telemetry? false})
          ctx {:launch-options launch-options
               :initials-draft (atom "")
               :fireball-phases (atom {})
               :sfx-emitted-count cursor
               :stop-title! (fn [])
               :play-events! (fn [events _] (swap! played conj (mapv :type events)))}
          before (-> (core/new-game {:width 800 :height 600})
                     core/start-game)
          after (:state (core/handle before {:type :fire :battery :left}))]
      (telemetry-emitter/emit-new-sfx! ctx before after)
      (telemetry-emitter/emit-new-sfx! ctx before after)
      (should= [[:sfx/launch] []] @played)
      (should= (count (core/sfx-events after)) @cursor))))

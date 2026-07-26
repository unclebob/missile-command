(ns missile-command.jvm.qa-runner-spec
  (:require [speclj.core :refer :all]
            [missile-command.jvm.qa-runner :as qa-runner]))

(defn- ctx
  [pending now]
  {:pending-events pending
   :now-ns (fn [] @now)
   :qa-telemetry? false
   :emit-sim! (fn [_])
   :persist-settings! identity
   :exit! (fn [])
   :apply-handle (fn [state command] (update state :commands conj command))
   :apply-destroy-options identity
   :apply-enemy-spec (fn [state spec] (assoc state :enemy spec))
   :apply-qa-fireballs (fn [state specs] (assoc state :fireballs specs))
   :toggle-pause (fn [state] (assoc state :paused true))
   :initials-draft (atom "")})

(describe "drain-one-event"
  (it "converts decimal seconds to nanoseconds"
    (should= 1500000000 (qa-runner/seconds->nanos 1.5)))

  (it "keeps a wait event pending until its deadline"
    (let [pending (atom [{:type :wait :seconds 0.5}])
          now (atom 1000000000)
          context (ctx pending now)
          state {:commands []}]
      (should= state (qa-runner/drain-one-event context state))
      (should= [{:type :wait :seconds 0.5 :until-ns 1500000000}] @pending)
      (reset! now 1499999999)
      (should= state (qa-runner/drain-one-event context state))
      (should= [{:type :wait :seconds 0.5 :until-ns 1500000000}] @pending)
      (reset! now 1500000000)
      (should= state (qa-runner/drain-one-event context state))
      (should= [] @pending)))

  (it "does not scale wait deadlines by qa-speed"
    (let [pending (atom [{:type :wait :seconds 2.0}])
          now (atom 1000000000)
          context (assoc (ctx pending now) :qa-speed 10.0)
          state {:commands []}]
      (should= state (qa-runner/drain-one-event context state))
      (should= [{:type :wait :seconds 2.0 :until-ns 3000000000}] @pending)))

  (it "dispatches command events through the supplied host handler"
    (let [pending (atom [{:type :pause}])
          now (atom 0)]
      (should= {:commands [{:type :pause}]}
               (qa-runner/drain-one-event (ctx pending now) {:commands []}))
      (should= [] @pending))))

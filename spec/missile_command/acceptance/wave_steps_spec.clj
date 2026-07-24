(ns missile-command.acceptance.wave-steps-spec
  (:require [speclj.core :refer :all]
            [missile-command.acceptance.steps :as steps]
            [missile-command.core :as core]))

(defn- fresh-world
  ([] (fresh-world 800 600))
  ([w h]
   {:state (core/start-game (core/new-game {:width w :height h}))}))

(defn- dispatch
  [world text example]
  (steps/dispatch-step world {:text text} example))

(describe "wave battery-target acceptance steps"
  (it "schedules enemies that target cities and batteries"
    (let [world (dispatch (fresh-world)
                          "the current wave has <remaining> scheduled enemies still active"
                          {"remaining" "9"})
          only-batteries (assoc-in world [:state :enemy-missiles]
                                   (mapv #(assoc % :target-kind :battery
                                                   :target-id :left)
                                         (core/enemy-missiles (:state world))))
          only-cities (assoc-in world [:state :enemy-missiles]
                                (mapv #(assoc % :target-kind :city
                                                :target-id 0)
                                      (core/enemy-missiles (:state world))))]
      (should= 9 (count (core/enemy-missiles (:state world))))
      (should= world
               (dispatch world "at least one enemy missile targets a city" {}))
      (should= world
               (dispatch world "at least one enemy missile targets a battery" {}))
      (should= only-batteries
               (dispatch only-batteries
                         "at least one enemy missile targets a battery" {}))
      (should= only-cities
               (dispatch only-cities
                         "at least one enemy missile targets a city" {}))
      (should= world
               (dispatch world "enemy missile targets include every living city" {}))
      (should= world
               (dispatch world
                         "enemy missile targets include every non-destroyed battery"
                         {}))
      (should= world
               (dispatch world
                         "every enemy missile targets a living city or a non-destroyed battery"
                         {}))
      (should-throw Exception #"no enemy targets a city"
        (dispatch only-batteries
                  "at least one enemy missile targets a city" {}))
      (should-throw Exception #"no enemy targets a battery"
        (dispatch only-cities
                  "at least one enemy missile targets a battery" {}))))

  (it "spawns a wave enemy at a battery and skips destroyed batteries"
    (let [world (dispatch (fresh-world)
                          "a wave enemy missile targeting battery <battery>"
                          {"battery" "left"})
          enemies (core/enemy-missiles (:state world))]
      (should= 1 (count enemies))
      (should= :battery (:target-kind (first enemies)))
      (should= :left (:target-id (first enemies)))
      (let [without-left (-> (fresh-world)
                             (dispatch "the <battery> battery has been destroyed"
                                       {"battery" "left"})
                             (dispatch
                              "the current wave has <remaining> scheduled enemies still active"
                              {"remaining" "8"}))]
        (should= without-left
                 (dispatch without-left
                           "no enemy missile targets battery <battery>"
                           {"battery" "left"}))
        (should= without-left
                 (dispatch without-left
                           "every enemy missile targets a living city or a non-destroyed battery"
                           {}))
        (should-throw Exception #"enemy targets destroyed battery"
          (dispatch world
                    "no enemy missile targets battery <battery>"
                    {"battery" "left"}))))))

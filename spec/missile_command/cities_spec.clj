(ns missile-command.cities-spec
  (:require [speclj.core :refer :all]
            [missile-command.cities :as cities]))

(describe "living and by-id"
  (it "finds a city and filters living ones"
    (let [cs [{:id 0 :alive? true}
              {:id 1 :alive? false}
              {:id 2 :alive? true}]]
      (should= 0 (:id (cities/by-id cs 0)))
      (should= [0 2] (mapv :id (cities/living cs))))))

(describe "destroy"
  (it "marks a city not alive"
    (should-not (:alive? (cities/destroy {:id 1 :alive? true})))))

(describe "update-city"
  (it "transforms only the matching city"
    (let [cs [{:id 0 :alive? true} {:id 1 :alive? true}]
          updated (cities/update-city cs 1 cities/destroy)]
      (should (:alive? (first updated)))
      (should-not (:alive? (second updated))))))

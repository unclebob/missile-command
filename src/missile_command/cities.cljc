(ns missile-command.cities
  (:require [missile-command.entities :as entities]))

(defn by-id
  [cities city-id]
  (first (filter #(= city-id (:id %)) cities)))

(defn living
  [cities]
  (filterv :alive? cities))

(def update-city
  "Apply f to the city with the given id; leave others unchanged."
  entities/update-by-id)

(defn destroy
  [city]
  (assoc city :alive? false))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T14:35:28.496352-05:00", :module-hash "1411269972", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "-617642269"} {:id "defn/by-id", :kind "defn", :line 4, :end-line 6, :hash "-1107865897"} {:id "defn/living", :kind "defn", :line 8, :end-line 10, :hash "1391336402"} {:id "defn/update-city", :kind "defn", :line 12, :end-line 15, :hash "-117934058"} {:id "defn/destroy", :kind "defn", :line 17, :end-line 19, :hash "1123802606"}]}
;; clj-mutate-manifest-end

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

(defn restore
  "Bring a destroyed city back to living."
  [city]
  (assoc city :alive? true))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T14:38:33.981013-05:00", :module-hash "1753940386", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "-617642269"} {:id "defn/by-id", :kind "defn", :line 4, :end-line 6, :hash "-1107865897"} {:id "defn/living", :kind "defn", :line 8, :end-line 10, :hash "1391336402"} {:id "def/update-city", :kind "def", :line 12, :end-line 14, :hash "1533337839"} {:id "defn/destroy", :kind "defn", :line 16, :end-line 18, :hash "1123802606"} {:id "defn/restore", :kind "defn", :line 20, :end-line 23, :hash "-351301480"}]}
;; clj-mutate-manifest-end

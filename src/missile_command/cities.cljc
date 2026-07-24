(ns missile-command.cities)

(defn by-id
  [cities city-id]
  (first (filter #(= city-id (:id %)) cities)))

(defn living
  [cities]
  (filterv :alive? cities))

(defn update-city
  "Apply f to the city with the given id; leave others unchanged."
  [cities city-id f]
  (mapv (fn [city]
          (if (= city-id (:id city))
            (f city)
            city))
        cities))

(defn destroy
  [city]
  (assoc city :alive? false))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T12:47:13.940755-05:00", :module-hash "-1221222833", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-1685034011"} {:id "defn/by-id", :kind "defn", :line 3, :end-line 5, :hash "-1107865897"} {:id "defn/living", :kind "defn", :line 7, :end-line 9, :hash "1391336402"} {:id "defn/update-city", :kind "defn", :line 11, :end-line 18, :hash "-1889747980"} {:id "defn/destroy", :kind "defn", :line 20, :end-line 22, :hash "1123802606"}]}
;; clj-mutate-manifest-end

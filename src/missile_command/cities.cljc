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

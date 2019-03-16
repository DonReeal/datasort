(ns datasort.core
    (:require [cljs.reader]
              [datasort.criteriasort :as csort]
              [reagent.core :as reagent]
              [reagent.ratom :as rg.ratom]))

;; Given a dataset in EDN - let the user sort by all keys that dataset contains
;; (currenly importing is not implemented yet)
(def dataset
  (map-indexed
    (fn [i v] (assoc v :id (inc i)))
    [{:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 1   :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 711  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 74 :sessionid "xxx-2"}
     {:api     "POST /foo"   :duration 74 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 77  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 14  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 74  :sessionid "xxx-3"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 15  :sessionid "xxx-4"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-3"}
     {:eventid "foo-created" :duration 15  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 85  :sessionid "xxx-6"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-6"}
     {:eventid "foo-created" :duration 22  :sessionid "xxx-7"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-7"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 22  :sessionid "xxx-7"}
     {:api     "POST /bar"   :duration 28  :sessionid "xxx-4"}
     {:eventid "bar-created" :duration 58  :sessionid "xxx-5"}
     {:eventid "foo-created" :duration 3386 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 789 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 74  :sessionid "xxx-8"}
     {:api     "POST /bar"   :duration 1258  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 782  :sessionid "xxx-6"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 88 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 23  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 17  :sessionid "xxx-8"}
     {:eventid "foo-created" :duration 74 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 96  :sessionid "xxx-9"}
     {:api     "POST /bar"   :duration 85  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 12  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 3588 :sessionid "xxx-8"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 1   :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 711  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 74 :sessionid "xxx-2"}
     {:api     "POST /foo"   :duration 74 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 77  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 14  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 74  :sessionid "xxx-3"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 15  :sessionid "xxx-4"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-3"}
     {:eventid "foo-created" :duration 15  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 85  :sessionid "xxx-6"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-6"}
     {:eventid "foo-created" :duration 22  :sessionid "xxx-7"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-7"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 22  :sessionid "xxx-7"}
     {:api     "POST /bar"   :duration 28  :sessionid "xxx-4"}
     {:eventid "bar-created" :duration 58  :sessionid "xxx-5"}
     {:eventid "foo-created" :duration 3386 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 789 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 74  :sessionid "xxx-8"}
     {:api     "POST /bar"   :duration 1258  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 782  :sessionid "xxx-6"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 88 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 23  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 17  :sessionid "xxx-8"}
     {:eventid "foo-created" :duration 74 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 96  :sessionid "xxx-9"}
     {:api     "POST /bar"   :duration 85  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 12  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 3588 :sessionid "xxx-8"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 1   :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 711  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 74 :sessionid "xxx-2"}
     {:api     "POST /foo"   :duration 74 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 77  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 14  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 74  :sessionid "xxx-3"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 15  :sessionid "xxx-4"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-3"}
     {:eventid "foo-created" :duration 15  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 85  :sessionid "xxx-6"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-6"}
     {:eventid "foo-created" :duration 22  :sessionid "xxx-7"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-7"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 22  :sessionid "xxx-7"}
     {:api     "POST /bar"   :duration 28  :sessionid "xxx-4"}
     {:eventid "bar-created" :duration 58  :sessionid "xxx-5"}
     {:eventid "foo-created" :duration 3386 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 789 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 74  :sessionid "xxx-8"}
     {:api     "POST /bar"   :duration 1258  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 782  :sessionid "xxx-6"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 88 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 23  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 17  :sessionid "xxx-8"}
     {:eventid "foo-created" :duration 74 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 96  :sessionid "xxx-9"}
     {:api     "POST /bar"   :duration 85  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 12  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 3588 :sessionid "xxx-8"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 1   :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 711  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 74 :sessionid "xxx-2"}
     {:api     "POST /foo"   :duration 74 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 77  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 14  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 74  :sessionid "xxx-3"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 15  :sessionid "xxx-4"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-3"}
     {:eventid "foo-created" :duration 15  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 85  :sessionid "xxx-6"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-6"}
     {:eventid "foo-created" :duration 22  :sessionid "xxx-7"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-7"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 22  :sessionid "xxx-7"}
     {:api     "POST /bar"   :duration 28  :sessionid "xxx-4"}
     {:eventid "bar-created" :duration 58  :sessionid "xxx-5"}
     {:eventid "foo-created" :duration 3386 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 789 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 74  :sessionid "xxx-8"}
     {:api     "POST /bar"   :duration 1258  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 782  :sessionid "xxx-6"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 88 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 23  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 17  :sessionid "xxx-8"}
     {:eventid "foo-created" :duration 74 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 96  :sessionid "xxx-9"}
     {:api     "POST /bar"   :duration 85  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 12  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 3588 :sessionid "xxx-8"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 1   :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 711  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 74 :sessionid "xxx-2"}
     {:api     "POST /foo"   :duration 74 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 77  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 14  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 74  :sessionid "xxx-3"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 15  :sessionid "xxx-4"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-3"}
     {:eventid "foo-created" :duration 15  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 85  :sessionid "xxx-6"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-6"}
     {:eventid "foo-created" :duration 22  :sessionid "xxx-7"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-7"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 22  :sessionid "xxx-7"}
     {:api     "POST /bar"   :duration 28  :sessionid "xxx-4"}
     {:eventid "bar-created" :duration 58  :sessionid "xxx-5"}
     {:eventid "foo-created" :duration 3386 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 789 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 74  :sessionid "xxx-8"}
     {:api     "POST /bar"   :duration 1258  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 782  :sessionid "xxx-6"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 88 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 23  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 17  :sessionid "xxx-8"}
     {:eventid "foo-created" :duration 74 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 96  :sessionid "xxx-9"}
     {:api     "POST /bar"   :duration 85  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 12  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 3588 :sessionid "xxx-8"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 1   :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 711  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 74 :sessionid "xxx-2"}
     {:api     "POST /foo"   :duration 74 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 77  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 14  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 74  :sessionid "xxx-3"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 15  :sessionid "xxx-4"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-3"}
     {:eventid "foo-created" :duration 15  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 85  :sessionid "xxx-6"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-6"}
     {:eventid "foo-created" :duration 22  :sessionid "xxx-7"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-7"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 22  :sessionid "xxx-7"}
     {:api     "POST /bar"   :duration 28  :sessionid "xxx-4"}
     {:eventid "bar-created" :duration 58  :sessionid "xxx-5"}
     {:eventid "foo-created" :duration 3386 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 789 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 74  :sessionid "xxx-8"}
     {:api     "POST /bar"   :duration 1258  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 782  :sessionid "xxx-6"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 88 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 23  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 17  :sessionid "xxx-8"}
     {:eventid "foo-created" :duration 74 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 96  :sessionid "xxx-9"}
     {:api     "POST /bar"   :duration 85  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 12  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 3588 :sessionid "xxx-8"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 1   :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 711  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 74 :sessionid "xxx-2"}
     {:api     "POST /foo"   :duration 74 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 77  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 14  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 74  :sessionid "xxx-3"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 15  :sessionid "xxx-4"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-3"}
     {:eventid "foo-created" :duration 15  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 85  :sessionid "xxx-6"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-6"}
     {:eventid "foo-created" :duration 22  :sessionid "xxx-7"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-7"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 22  :sessionid "xxx-7"}
     {:api     "POST /bar"   :duration 28  :sessionid "xxx-4"}
     {:eventid "bar-created" :duration 58  :sessionid "xxx-5"}
     {:eventid "foo-created" :duration 3386 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 789 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 74  :sessionid "xxx-8"}
     {:api     "POST /bar"   :duration 1258  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 782  :sessionid "xxx-6"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 88 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 23  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 17  :sessionid "xxx-8"}
     {:eventid "foo-created" :duration 74 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 96  :sessionid "xxx-9"}
     {:api     "POST /bar"   :duration 85  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 12  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 3588 :sessionid "xxx-8"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 1   :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 711  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 74 :sessionid "xxx-2"}
     {:api     "POST /foo"   :duration 74 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 77  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 14  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 74  :sessionid "xxx-3"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 15  :sessionid "xxx-4"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-3"}
     {:eventid "foo-created" :duration 15  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-5"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 85  :sessionid "xxx-6"}
     {:api     "POST /bar"   :duration 15  :sessionid "xxx-6"}
     {:eventid "foo-created" :duration 22  :sessionid "xxx-7"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-7"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
     {:api     "POST /foo"   :duration 22  :sessionid "xxx-7"}
     {:api     "POST /bar"   :duration 28  :sessionid "xxx-4"}
     {:eventid "bar-created" :duration 58  :sessionid "xxx-5"}
     {:eventid "foo-created" :duration 3386 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 789 :sessionid "xxx-6"}
     {:api     "POST /foo"   :duration 74  :sessionid "xxx-8"}
     {:api     "POST /bar"   :duration 1258  :sessionid "xxx-2"}
     {:eventid "bar-created" :duration 782  :sessionid "xxx-6"}
     {:eventid "bar-created" :duration 3386 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 88 :sessionid "xxx-4"}
     {:api     "POST /foo"   :duration 23  :sessionid "xxx-1"}
     {:api     "POST /bar"   :duration 71  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 17  :sessionid "xxx-8"}
     {:eventid "foo-created" :duration 74 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 200 :sessionid "xxx-9"}
     {:api     "POST /foo"   :duration 96  :sessionid "xxx-9"}
     {:api     "POST /bar"   :duration 85  :sessionid "xxx-1"}
     {:eventid "bar-created" :duration 12  :sessionid "xxx-2"}
     {:eventid "foo-created" :duration 3588 :sessionid "xxx-8"}]))

;; ==============================================================================
;; simulating json-import - currently a bunch of helpers to init table state
;; ==============================================================================

(defn unique-index
  "Creates a map to access the values in coll by its index.
   Currently throws error if the index-fn does not return a unique value for each entry in coll
   - that might change!"
  [index-fn coll]
  (reduce
    (fn [map item] (let [item-index-value (index-fn item)]
                     (if (contains? map item-index-value)
                       (throw (js/Error "NON-UNIQUE-IDENT-FN"))
                       (conj map [item-index-value item]))))
    (sorted-map-by <)
    coll))

;; TODO: dont trust this code - might blow up on huge datasets
;; (reduce clojure.set/union #{} (repeat 44000000 #{:a :b :c :d})) ;; => browser times out
;; (into #{} conj (repeat 44000000 '(:a :b :c :d)))                ;; => just about works
(defn reduce-keys
  "Returns all unique keys present in all records passed."
  [records]
  (into #{} conj (mapcat keys records)))

(defn initial-colspec [ident-fn records]
  (let [first-colspec {:col-id (name ident-fn) :resolver-fn ident-fn :order :asc}
        otherkeys (disj (reduce-keys records) ident-fn)]
    (into
      [first-colspec]
      (->> otherkeys
           (map #(hash-map :col-id (name %) :resolver-fn % :order :asc))
           (sort-by :col-id)))))

(defn initial-state
  "Initializes the app state"
  [ident-fn records]
  {:columns       (initial-colspec ident-fn records)
   :records-by-id (unique-index ident-fn records)})


;; ==============================================================================
;; initial app state -- built from records with arbitrary fields
;; ==============================================================================

(defonce state (reagent/atom (initial-state :id dataset)))

(add-watch state 
           :trace-app-state
           (fn [key atom old-state new-state]
             (println (str "active element after transition:  " (.-id (.-activeElement js/document))))))


;; to serve arbitrary data use (initial-state ident-fn records)
(comment
  ;; initial-app-state typed out for readability
  (def denormalized-table-state-model
    {;; initial columns definition for table row order and sort order definition
     :columns
     [{:col-id "id"        :resolver-fn :id        :order :asc}
      {:col-id "api"       :resolver-fn :api       :order :asc}
      {:col-id "duration"  :resolver-fn :duration  :order :asc}
      {:col-id "eventid"   :resolver-fn :eventid   :order :asc}
      {:col-id "sessionid" :resolver-fn :sessionid :order :asc}]
     ;; read index for rendering
     :records-by-id
     {1 {:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
      2 {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
      3 {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
      4 {:id 4                  :duration 150 :sessionid "xxx-2" :eventid "foo-created"}}})
  ;; TODO: move this to a test when writing importer
  (= denormalized-table-state-model (initial-state :id dataset)))


;; ==============================================================================
;; actions
;; ==============================================================================

(defn toggle-col-order!
  "Invert sort order for sort criterion at column index col-index"
  [col-index]
  (let [columns-0   (:columns @state)
        colspec-i-0 (nth columns-0 col-index)
        colspec-i-1 (assoc colspec-i-0 :order ({:asc :desc :desc :asc} (:order colspec-i-0)))
        columns-1   (assoc columns-0 col-index colspec-i-1)]
    (swap! state assoc :columns columns-1)))

;; to swap the displayed columns order and to change order criteria
(defn- swap [v index1 index2]
  (assoc v index2 (v index1) index1 (v index2)))

(defn col-shift-left!
  "Swaps the columns index position within the table with the column to its left"
  [col-index]
  (let [columns (:columns @state)
        shifted-columns (swap columns col-index (dec col-index))]
    (swap! state assoc :columns shifted-columns)))

(defn col-shift-right!
  "Swaps the columns index position within the table with the column to its right"
  [col-index]
  (let [columns (:columns @state)
        shifted-columns (swap columns col-index (inc col-index))]
    (swap! state assoc :columns shifted-columns)))


;; ==============================================================================
;; cursors
;; ==============================================================================

(def *columns       (reagent/cursor state [:columns]))
(def *records-by-id (reagent/cursor state [:records-by-id]))


;; ==============================================================================
;; ui components
;; ==============================================================================

;; TODO: make accessible header
;; https://www.w3.org/TR/wai-aria-practices/examples/grid/dataGrids.html

;; key utils see spec: https://developer.mozilla.org/de/docs/Web/API/KeyboardEvent/key

(defn when-enter [e effect-fn]
  (when (= (.-key e) "Enter")
        (effect-fn)
        (.preventDefault e)))

(defn when-space [e effect-fn]
  (when (let [key (.-key e)]
          (or (= key " ")
              (= key "Spacebar")))
        (effect-fn)
        (.preventDefault e)))

;; TODO add arrow navigation?

;; TODO: drag and drop
;; when selecting drag source
;; implement radio button like behaviour
;; with arrow left + up and arrow top and right + space for navigating selections
;; 

;; FIXME when using swap right button on last possible right swap
;; focus is not restored as expected, tabindex 
;; why is the focus even on the same value, and not on the same dom-element after swapping????
;; wtf is react doing here?
;; could i hack it by using onblur?
;; MAYBE TRY: completely different implementation with roving tabindex
;; <-> all but active have tabindex -1 which allows for arrow navigation
;; 

(defn when-left-arrow [e effect-fn]
  (when (let [key (.-key e)]
          (or (= key "LeftArrow")
              (= key "Left")))
        (effect-fn)
        (.preventDefault e)))

(defn when-right-arrow [e effect-fn]
  (when (let [key (.-key e)]
          (or (= key "RightArrow")
              (= key "Right")))
        (effect-fn)
        (.preventDefault e)))

(defn when-up-arrow [e effect-fn]
  (when (let [key (.-key e)]
          (or (= key "UpArrow")
              (= key "Up")))
        (effect-fn)
        (.preventDefault e)))

(defn when-down-arrow [e effect-fn]
  (when (let [key (.-key e)]
          (or (= key "DownArrow")
              (= key "Down")))
        (effect-fn)
        (.preventDefault e)))

(defn render-table
  [{:keys [columns
           on-toggle-col-order
           on-col-shift-left
           on-col-shift-right
           records]}]
  [:table {:role "grid"}
    [:thead
      [:tr
        (for [[index {:keys [col-id order]}] (map-indexed vector columns)
              :let [column-tabindex (* (+ index 1) 10)]]
          ^{:key col-id}
          [:th
           {:aria-sort ({:asc "ascending" :desc "descending"} order)
            :width "200px"}
           ;; left nav
           (or (= index 0)
             nil
             [:span
              {:id (str col-id "-leftNav")
               :tab-index (+ column-tabindex 1)
               :role "button"
               :aria-label "Swap this column with the column to its left. Will change sort order of the table."
               :on-click #(on-col-shift-left index)
               :on-key-press (fn [e] (when-enter e #(on-col-shift-left index)))
               :on-focus (fn [e] (println (str "Got focus: " (.-id (.-target e)))))
               :on-blur (fn [e] (println (str "Lost focus: " (.-id (.-target e)))))}
              "< "])
           ;; column main control
           [:span
            {:id (str col-id "-columnControl")
             :tab-index (+ column-tabindex 2)
             :role "button"
             :on-click  #(on-toggle-col-order index)
             :on-key-press (fn [e] (when-enter e #(on-toggle-col-order index))
                                   (when-space e #(.alert js/window "TODO: Drag source selected! Should render selection of drag targets here!")))  
             :on-focus (fn [e] (println (str "Got focus: " (.-id (.-target e)))))
             :on-blur (fn [e] (println (str "Lost focus: " (.-id (.-target e)))))}
            col-id]
           ;; right nav
           (or (= index (- (count columns) 1))
             nil
             [:span
              {:id (str col-id "-rightNav")
               :tab-index (+ column-tabindex 3)
               :role "button"
               :aria-label "Swap this column with the column to its right. Will change sort order of the table."
               :on-click #(on-col-shift-right index)
               :on-key-press (fn [e] (when-enter e #(on-col-shift-right index)))
               :on-focus (fn [e] (println (str "Got focus: " (.-id (.-target e)))))
               :on-blur (fn [e] (println (str "Lost focus: " (.-id (.-target e)))))}
              " >"])])]]
    [:tbody
      (for [record records]
        ^{:key (:id record)} 
        [:tr
          (for [{:keys [col-id resolver-fn]} columns]
            ^{:key col-id}
            [:td (resolver-fn record)])])]])


(defn colspec->sortcriterion [{:keys [resolver-fn order]}]
  (let [criterion-comparator-fn
        ({:asc  #(csort/cmp ::csort/nils-last ::csort/asc %1 %2)
          :desc #(csort/cmp ::csort/nils-last ::csort/asc %2 %1)}
         order)]
    [resolver-fn criterion-comparator-fn]))

(defn- sort-by-colspec [columns records]
    (csort/sort-by-criteria
      (map colspec->sortcriterion columns)
      records))

(defn component-table []
  (let [columns @*columns
        *sorted-records (rg.ratom/reaction (sort-by-colspec @*columns (vals @*records-by-id)))]
    [render-table
      {:columns             columns
       :records             @*sorted-records
       :on-toggle-col-order toggle-col-order!
       :on-col-shift-left   col-shift-left!
       :on-col-shift-right  col-shift-right!}]))

(defn home []
  [:div {:style {:margin "auto"
                 :padding-top "30px"
                 :width "600px"}}
    [:h1 "Datasort demo"]
    ;; [:p "Sample uses log data - records with arbitrary fields"]
    [component-table]])
    
    
(defn ^:export main []
  (reagent/render [home] (.getElementById js/document "app")))

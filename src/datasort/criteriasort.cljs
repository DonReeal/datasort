(ns datasort.criteriasort
  (:require [goog.array]
            [clojure.spec.alpha :as clj.spec]))

;; TODO naming: criteria/criterion vs partition-by vs propsort / propertycompare (sort by data properties ...)
;; 
;; * ALTERNATIVE: maybe sufficient: skip global sorting, use group-by directly?
;; * ALIKE PROBLEM: composite indexes in db-systems, they require ASC DESC to be declared up front!
;;                  (kibana doens not support arbitrary coposite keys for sorting <-> they would require composite indexes)
;; * ALIKE PROBLEM: (exploratory) data analysis - e.g python.panda [DataFrames Indexes Series]
;;                  ! think of this as using a sample data set without cleaning, transforming and putting it into a dataframe upfront !

;; fn names nicked from https://grokbase.com/t/gg/clojure/138zq4ch8z/sorting-a-collection-on-multiple-fields

(defn compare-many [criteria]
  ;; TODO: API ok if stable sort will be needed for large collections?
  (fn [a b] ;; returns a comparator for a and b
    (loop [criteria criteria]
      (if (empty? criteria)
        0 ;; simple case: allow equally sorted records
        (let [criterion (first criteria)            ;; TODO #lib #spec criteria must not be empty (no need (sort-by default-key-fn coll) is easy)
              [resolver-fn comparator-fn] criterion ;; TODO #lib #spec Each criterion must contain a resolver-fn (returns nil or deterministic/single, sortable type) and a comparator-fn
              criteria-value-a (resolver-fn a)
              criteria-value-b (resolver-fn b)
              comparison (comparator-fn criteria-value-a criteria-value-b)]
          (if-not (zero? comparison)
            comparison
            (recur (rest criteria))))))))

(defn sort-by-many [criteria coll]
  (sort (compare-many criteria) coll))

;; TODO #lib using compare many for this is kinda lazy - but enough for demo
(defn cmp-fn [order & [nil-sorting]] ;; TODO spec ::order #{::asc ::desc} ::nil-sorting #{::nils-last ::nils-first}, 
  (let [nil-sorting (or nil-sorting ::nils-last)]
    (cond
      (and           (= ::nils-last nil-sorting)  (= ::asc order))
      (compare-many [[nil? compare]               [identity compare]])
      (and           (= ::nils-first nil-sorting) (= ::asc order))
      (compare-many [[some? compare]              [identity compare]])
      (and           (= ::nils-last nil-sorting)  (= ::desc order))
      (compare-many [[nil? compare]               [identity #(compare %2 %1)]])      
      (and           (= ::nils-first nil-sorting) (= ::desc order))
      (compare-many [[some? compare]              [identity #(compare %2 %1)]])
      :else (throw (js/Error. (str "Not supported keyword combination: " [order nil-sorting]))))))

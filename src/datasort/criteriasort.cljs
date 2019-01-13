(ns datasort.criteriasort
  (:require [goog.array]
            [clojure.spec.alpha :as clj.spec]))

;; TODO naming: criteria/criterion vs partition-by vs propsort / propertycompare (sort by data properties ...)
;; 
;; * ALTERNATIVE: maybe sufficient: skip global sorting, use group-by directly?
;; * ALIKE PROBLEM: composite indexes in db-systems, they require ASC DESC to be declared up front!
;;                  (kibana doens not support arbitrary coposite keys for sorting <-> they would require composite indexes)
;; * ALIKE PROBLEM: (exploratory) data analysis
;;                  e.g what you do before you shove data into python.panda [DataFrames Indexes Series]

;; fn names nicked from https://grokbase.com/t/gg/clojure/138zq4ch8z/sorting-a-collection-on-multiple-fields

;; functions based api ========================================================
(defn compare-many [criteria]
  (fn [a b] ;; returns a comparator for a and b
    (loop [criteria criteria]
      (if (empty? criteria)
        ;; general implementation - allow equally sorted records
        0 ;; consider throwing a dev-error - this would be useful for ordering deterministically
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

(defn cmp-fn2 [natural-order? nils-last?] ;; TODO spec only bools allowed
  (let [handle-nils-criterion  ({true  [nil? compare]
                                 false [nil? #(compare %2 %1)]}
                                nils-last?)
        handle-order-criterion ({true  [identity compare]
                                 false [identity #(compare %2 %1)]}
                                natural-order?)]
    (compare-many [handle-nils-criterion handle-order-criterion])))


(defn compile-comparator
  [{::keys [nils order]}]
  (let [nils-criterion ({::last [nil? compare]
                         ::nils-last [nil? compare]
                         ::first [some? compare]
                         ::nils-first [some? compare]} 
                        nils)
        order-criterion ({::asc [identity compare] 
                          ::ascending [identity compare]
                          ::desc [identity #(compare %2 %1)]
                          ::descending [identity #(compare %2 %1)]}
                         order)]
    (if (or (nil? nils-criterion) (nil? order-criterion))
      (throw (js/Error. (str "No mapping known for: " [order nils])))
      (compare-many [nils-criterion order-criterion]))))

(defn compile-criteria [criteria]
  (map (fn [[resolver-fn comparator-spec]]
         [resolver-fn (compile-comparator comparator-spec)])
       criteria))

(defn sort-by-criteria [criteria-spec coll]
    (sort-by-many (compile-criteria criteria-spec) coll))
  

(comment 
  (in-ns 'datasort.criteriasort)

  (sort (compile-comparator {::order ::desc ::nils ::nils-first}) [nil 1 2 3 3 2 1 nil])
  
  (def dataset
    [{:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
     {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
     {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
     {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}])
  
  (sort-by-many
    (compile-criteria
      [[:sessionid {::order ::asc}]
       [:duration {::order ::asc}]])
    dataset)
  
  (sort-by-criteria
    [[:api {::order ::asc ::nils ::nils-first}]
     [:duration {::order ::desc}]]
    dataset))
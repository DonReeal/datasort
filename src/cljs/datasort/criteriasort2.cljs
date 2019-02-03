(ns datasort.criteriasort2) ;; functions based api

(defn by-criteria
  "Compares v1 and v2 based on all declarations in criteria-seq
   - criteria-seq must be a sequence of resolver-fn, comparator-fn pairs.
   - criteria are applied for comparison in the order specified in criteria-seq"
  [criteria-seq v1 v2] ;; TODO: spec
  (loop [criteria criteria-seq]
    (if (empty? criteria)
      0 ;; none of the criteria made v1 difference
      (let [[resolver-fn comparator-fn] (first criteria)
            comparison (comparator-fn (resolver-fn v1) (resolver-fn v2))]
        (if-not (zero? comparison)
          comparison ;; current criterion made v1 difference
          (recur (rest criteria)))))))

(defn sort-by-criteria
  "Sorts a collection based on the criteria-seq. Criteria must be a sequence of resolver-fn, comparator-fn pairs."
  [criteria-seq coll] ;; TODO: spec
  (sort #(by-criteria criteria-seq %1 %2) coll))

(defn- criteria [nils order] ;; TODO spec ::order in #{::order-asc ::order-desc}, ::nils in #{::nils-last ::nils-a}
  (let [nils-criterion
        ({::nils-last  [nil? compare]
          ::nils-first [some? compare]} nils)
        order-criterion
        ({::asc  [identity compare]
          ::desc [identity #(compare %2 %1)]} order)]
    [nils-criterion order-criterion]))

(defn cmp
  "Compares v1 and v2 based on
   how to sort nil values ::nils-last or ::nils-v1
   and in what order to sort ::asc or ::desc."
  [nils order v1 v2] ;; TODO: spec
  (by-criteria (criteria nils order) v1 v2))

(defn cmp-fn
  "Builds a compare function by passing
   how to sort nil values ::nils-last or ::nils-first
   and in what order to sort ::asc or ::desc."
  [nils order] ;; TODO: spec
  (fn [v1 v2] (cmp nils order v1 v2)))

(comment
  (in-ns 'datasort.criteriasort2)

  (def dataset
    [{:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
     {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
     {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
     {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}])

  (sort-by-criteria [[:id #(compare %2 %1)]] dataset)

  (sort-by-criteria [[:api #(cmp ::nils-last ::asc %1 %2)]] dataset)

  ;; fully blow examples

  (sort-by-criteria [[:api      (cmp-fn ::nils-last ::asc)]
                     [:eventid  (cmp-fn ::nils-last ::asc)]
                     [:duration (cmp-fn ::nils-last ::desc)]]
                    dataset)

  (sort-by-criteria [[:api      #(cmp ::nils-last ::asc  %1 %2)]
                     [:eventid  #(cmp ::nils-last ::asc  %1 %2)]
                     [:duration #(cmp ::nils-last ::desc %1 %2)]]
                    dataset)

  ;; TODO: check performance and possibly use lower level code for cmp, cmp-fn that uses native js ifs for null checking - and <,> for comparison - currently the above expands to the code below
  (sort
    (fn [a b] (by-criteria [[:api      (fn [a b] (by-criteria [[nil? compare] [identity compare]]                  a b))]
                            [:eventid  (fn [a b] (by-criteria [[nil? compare] [identity compare]]                  a b))]
                            [:duration (fn [a b] (by-criteria [[nil? compare] [identity (fn [a b] (compare a b))]] a b))]]
                           a b))
    dataset))
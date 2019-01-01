(ns datasort.comparators
  (:require [goog.array :as garray]))

;; TODO naming: criteria/criterion vs partition-by
;; * ALTERNATIVE: maybe sufficient implementation see: https://grokbase.com/t/gg/clojure/138zq4ch8z/sorting-a-collection-on-multiple-fields
;; * ALTERNATIVE: maybe sufficient: skip global sorting, use group-by directly?
;; * ALIKE PROBLEM: composite indexes in db-systems, they require ASC DESC to be declared up front!
;;                  (kibana doens not support arbitrary coposite keys for sorting <-> they would require composite indexes)
;; * ALIKE PROBLEM: (exploratory) data analysis - e.g python.panda [DataFrames Indexes Series]
;;                  ! think of this as using a sample data set without cleaning, transforming and putting it into a dataframe upfront !

;; compare-many
;; sort-by-many
;; sort-by-map


(defn nils-last ;; hmm this is not a comparators its a compare implementation
  "Sorts nils last and delegates to compare when both values are not nil."
  [a b]
  (if (nil? a)
    (if (nil? b) 0 1)
    (if (nil? b) -1 (compare a b))))

(defn nils-last-cmp
  [comparator-fn]
  (fn [a b]
     (if (nil? a)
       (if (nil? b) 0 1)
       (if (nil? b) -1 (comparator-fn a b)))))

(defn- nils-last3
  [comparator-fn a b]
  (if (nil? a)
    (if (nil? b) 0 1)
    (if (nil? b) -1 (comparator-fn a b))))

(defn- nils-first3
  [comparator-fn a b]
  (if (nil? a)
    (if (nil? b) 0 -1)
    (if (nil? b) 1 (comparator-fn a b))))

(defn rev-compare [a b]
  (compare b a))

(defn reverse-cmp
  [comparator-fn]
  (fn [a b] (comparator-fn b a)))

(defn reverse3
  [comparator-fn a b]
  (comparator-fn b a))

;; comparator with modfiers for natural ordered data types
(defn partition-comparator [{::keys [reverse? nils-first?] :or {reverse? false nils-first? false}}]
  (println (str "reverse?: " reverse?))
  (println (str "nils-first?: " nils-first?))
  (if reverse?
    (if nils-first?
      (fn [a b] compare b a)
      (fn [a b] (nils-last3 compare b a)))
    (if nils-first?
      (do (println "natural! nils first!")
          (fn [a b] compare a b))
      (do (println "natural! nils last!")  
          (fn [a b] (nils-last3 compare a b))))))

(defn compare-variant [& [reverse? nils-first?]]
  (println (str "reverse?: " reverse?))
  (println (str "nils-first?: " nils-first?))
  (if reverse?
    (if nils-first?
      (fn [a b] (nils-first3 compare b a))
      (fn [a b] (nils-last3 compare b a)))
    (if nils-first?
      (fn [a b] (nils-first3 compare a b))
      (fn [a b] (nils-last3 compare a b)))))



(defn compare-partitioned
  [sort-partitions v1 v2]
  (loop [sc sort-partitions]
    (if (empty? sc) ;; TODO check if thats a reasonably behaviour - special case for UI-Data the must always be rendered in deterministic order
      (do (println) "[datasort.core/WARNING] unable to compare " v1 " and " v2 " using: " sort-partitions ". Falling back to garray/defaultCompare!"
          (garray/defaultCompare v1 v2))
      (let [[resolver-fn comparator-fn] (first sc)
            partitionvalue1 (resolver-fn v1)
            partitionvalue2 (resolver-fn v2)
            comparison (comparator-fn partitionvalue1 partitionvalue2)]
        (if-not (zero? comparison) 
          comparison ;; done - comparator made a difference
          (recur (rest sc)))))))
          
(defn partitioned-comparator ;;naming ... criteria vs partition
  "Returns a 3-way-comparator that sorts the values v1 and v2 in partitions as declared by sort-partitions.
   * sort-partitions must be sequence of 2-tuples containing each a resolver function and a comparator function.
   * all resolver functions must be a function that extracts a compararable value or nil out of v1 and v2
   * all comparator-fns must be 3-way-comparator functions
   * the order of to sort-partitions declares the order in which the sorting will be done
   example usage - comparing maps by map-entry values:
   (sort (by-partitions [[:metric nils-last] [:operation nils-last] [:level nils-last] [:id compare]]) dataset)
   example usage - comparing maps by transformed nested value:
   (sort (by-partitions [[#(str->inst (get-in % [:create-info :timestamp])) #(compare %2 %1)] [:id compare]]) dataset)"
  [sort-partitions] 
  (fn [v1 v2] (compare-partitioned sort-partitions v1 v2)))





  

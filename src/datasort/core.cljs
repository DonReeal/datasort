(ns datasort.core
    (:require [cljs.reader :as reader]
              [clojure.spec.alpha :as spec]
              [goog.array :as garray]
              [datasort.comparables :as comparables]
              [datasort.comparators :as comparators]))

(enable-console-print!)

(comment
 "Nullfriendly sorting experiment in Clojure/ClojureScript
  Example for implementation: sorting a smaller set of kibana logs that are semi-structured")

;; POSSIBLE SOLUTIONS ::::::::::::::::::::::::::::::::::::::::
;; 1. Reifiy as comparable with special null treatment => see datasort.comparables
;;    consider using "decorate-sort-undecorate" technique to reify only once
;;    WELL THIS WAS HARD TOO AND requires java/javascript specific integration
;;    :/ but kind of done - performance wont be too good i guess
;; 2. Build custom datasort function (dont use core.sort / core.compare) 
;;    which is hard to do as we need to implement cljs too then
;;    1. understand cljs.core sort
;;    2. resuse the same primitives but try replace used raw functions by nullsafe alternatives
;; 3. Use garray/defaultCompare


(def dataset
 [{:id 1
   :localtime "2018-12-20T14:58:00.000-00:00"
   :level "INFO"
   :serviceid "tablezz-svc"
   :event "session-initialized"
   :api "/session"
   :success true}
  {:id 2
   :localtime "2018-12-20T14:58:10.000-00:00"
   :level "INFO"
   :serviceid "tablezz-svc"
   :event "table-read"
   :api "/read-table"}
  {:id 3
   :api "/read-table"
   :operation "remote:tablezz-kassandra/get"
   :localtime "2018-12-20T14:58:11.001-00:00"
   :level "WARN"
   :event "remote-unavailable"
   :success true
   :serviceid "tablezz-svc"
   :detail "Could not reach remote - Retrying ..."}
  {:id 4
   :localtime "2018-12-20T14:58:11.101-00:00"
   :level "INFO"
   :serviceid "tablezz-svc"
   :event "performance-monitoring"
   :operation "remote:tablezz-kassandra/get"
   :metric 1100
   :api "/read-table"}
  {:id 5
   :localtime "2018-12-20T14:58:11.113-00:00",
   :level "INFO",
   :serviceid "tablezz-svc",
   :event "table-read-ok",
   :api "/read-table",
   :metric 1112,
   :success true}
  {:id 6
   :localtime "2018-12-20T14:58:11.113+01:00"
   :level "INFO"
   :serviceid "tablezz-svc"
   :event "table-read-ok"
   :api "/read-table"
   :metric 115
   :success true}])

;; single place for app state
(def state (atom {}))

;; selfmade mini debugging tool: traces all changes in state
(add-watch state :trace-state-changed
  (fn [key atom old-state new-state]
    (println (str "== " key " =="))
    (cljs.pprint/pprint new-state)))

;; set initial state
(let [logs (into (sorted-map) (map #(vector (:id %) %) dataset))]
  (swap! state assoc 
    :logs logs
    :logs-sorted-ids (apply vector (keys logs))))


;; should ensure that all types getting indexed can be sorted https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html
;; text, numeric, boolean, date

;; Multi fields comparators ? possible with nulls-last / anything but natural order ?
;; 
;; (sort-by :localtime dataset)
;; (sort-by (juxt :localtime :level) dataset)
;;
;; does not WORK - :( ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
;; (sort-by (juxt :metric) #(nils-last %1 %2) dataset)
;; HMMM - that works
;; (sort-by :metric #(nils-last %1 %2) dataset)
;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
;; OKAY i got it:
;; Sorting it breaks on comparing vectors
;; the extracted vector has the content nil - comparing [nil] [1 2 3] compares nil to 1 in natural order)
;; [nil 1 1]
;; [1   1 1] 

;; http://clojurescriptkoans.com/#lazy-sequences/1
;; http://clojurescriptkoans.com/#destructuring/4

(defn nils-last [a b]
  (if (nil? a) 
    (if (nil? b) 0 1) (if (nil? b) -1 (compare a b))))

;; how can i suppress delegating to default sort when dealing with complex types?
;; => not at all, just dont allow complex inputs ...

;; comparator implementation
;; OK     nil records should be sorted last when compared with actual records
;; XX!!! vectors with nil values at i should be sorted last compared to vectors with values at i 
;;       not possible with implementing comparator, compare will recurse into values and use cljs.core/compare on "primitives"
;;       well i called compare so thats my fault,
;;       what could i call instead?

;; hmm maybe that is not a good idea on "primitives"
;; because any extending type (e.g. #inst) will break the sorting behaviour
;; could i define it on a record level?

;; the story is okay on typed records ..
;; but it gets ugly for unstructured data ...
;; well what are the primitives of unstructured data?
;;   text, numeric, boolean, instant, // pretty exotic ... refs?


;;
;; #datasort/text
;; #datasort/numeric
;; #datasort/binary
;; #datasort/chronologic
;;


;; considered:
;; * add less-is-more boolean flag? To let user configure nil and unequal length arrays treatment
;; * comparison amongs different types?
;; => no, only same length vectors supported. in contrast to the example in https://clojure.org/guides/comparators#_comparators_that_work_between_different_types
;;    I do care about the order; 
;;    The order guarantee that might be at risk when skipping those kind of checks could be to catch possible exception an fallback to 
;;    garray/defaultCompare.
;; * add nils-first/nils-last flag globally?
;; => no, nils should be decided  on each sort-property individually

;; [<[resolver-fn, comparator-fn]>]

(defn vector-comparator
  ([comparators]
   "Creates a comparator-fn that compares two vectors of equal length.
    The vectors used must have the same length.
    The value pairs at the same index position of the two vectors must be of the same datatype."
   (fn [sortvec1 sortvec2]
     (loop [comparators comparators
            sortvec1 sortvec1
            sortvec2 sortvec2]
       (if (empty? comparators) 
        0 ;; done - none of the loops had different value pair
        (let [c ((first comparators) (first sortvec1) (first sortvec2))]
          (if (not= c 0)
            c ;; done - this loop had different value pair
            (recur (rest comparators)
                   (rest sortvec1) 
                   (rest sortvec2)))))))))

;; GOAL: make user define resolver and comparator pairwise
;; IMPL: procedural/ lazy / low memory ... no vectors are created
(defn by-criteria ;; name 'by-partitions'?
  "Returns a 3-way-comparator that sorts the values v1 and v2.
   * sort-criteria must be sequence of resolver-fn, comparator-fn 2-tuples
   * all resolver-fns must be function that extracts a compararable value or nil out of v1 and v2
   * all comparator-fns must be a 3-way-comparator function
   example usage:
   (sort (by-criteria [[:metric nils-last] 
                       [:operation nils-last] 
                       [:level nils-last]])
                       [:id compare]])
         dataset)"
  [sort-criteria]
  (fn [v1 v2]
    (loop [sc sort-criteria]
      (if (empty? sc)
        (do (println "[datasort.core/WARNING] unable to compare " v1 " and " v2 " using: " sort-criteria ". Falling back to garray/defaultCompare!")
            (garray/defaultCompare v1 v2))
        (let [[resolver-fn comparator-fn] (first sc)
              comparison (comparator-fn (resolver-fn v1) (resolver-fn v2))]
          (if (not= comparison 0) 
            comparison ;; done - comparator made a difference
            (recur (rest sc))))))))


(defn arraycomp
  "this should work as a core! This is not a any-order that at least is guaranteed - its precise deterministic - comes at the cost of being verbose
   Nullfriendly comparator
   value1 and values2 must be vectors of equal length that may contain only nil or naturally comparable values
   TODO:
    * is defaulting to garray/defaultCompare correct when no comparators passed? (sort garray/defaultCompare [[nil] [1] [2]])
    * what should be supported parameter datastructures: lists, vectors, maps, sets?
    * nil values for parameters supported?"
  [comparators values1 values2]
  (loop [v1 values1
         v2 values2 
         comparators comparators]
    (if (= (count comparators) 0)
      ((or (first comparators) compare) (first v1) (first v2))
      (let [comparison ((or (first comparators) compare) (first v1) (first v2))]
        (if (not= 0 comparison) comparison
          (recur (rest v1)
                 (rest v2)
                 (rest comparators)))))))
                 

(defn- decorate-sort-undecorate [resolvers comparators coll]
  (->> coll
       (map (fn [record] [((apply juxt resolvers) record) record])) ;; decorate   | could be used as a first class function as it should be equal to the table columns
       (sort-by first (partial arraycomp comparators))              ;; sort       | 
       (mapv second)))                                               ;; undecorate | 

(comment 
  "API - how i want to use this code ...
   Example usage:
   (datasort [:metric :operation :level] dataset)
   (datasort [:metric   :operation :level] 
             [nils-last nils-last  nils-last] 
             dataset)")

(defn datasort
  ([resolvers seq] (datasort resolvers nil seq))
  ([resolvers comparators seq]
   (decorate-sort-undecorate resolvers comparators seq)))


;; considering those public apis:
;; "1. Raw configurable generic comparator API - provide own namespace?
;; <-> No mix of resolve and compare
;; <-> User can decide on himself when to use comparing";; 
;;    (arraycomp [property-array1 property-array2])
;;    (arraycomp [comparators property-array1 property-array2])
;;    (arraycomp [more-is-less comparators property-array1 property-array2])
;; "2. Convenience API for tabular views on data ..."
;;    decorate-sort-undecorate only useful as behaviour if
;;    data is not just a temporary projection
;; "3. Convenience API for taking all user input for sorting
;;    (datasort [sorting-criteria-vec backing-seq]
;;       ()
;;    * inline decorate-sort-undecorate
;;    * project temporary while sorting based on edn data tree
;;    - wont be any useful for e.g. datascript ui-backend
;;    + easy to use - no wrong use of arraycomp possible, as passing the resolvers will automatically fulfil ::eq-length-vec :sortable-values-only invariants"
;; "4. Provide a custom comparator on any property selected" 
;; "5. Provide a comparator-modifier on any property selected
;;       dont use raw-seq at all?
;;     (XX [comp v1 v2])"
;; 6. Comparator-logic only allowed on visible fields
;;    start with context [:name :firstname]  <- the header row must preserve the original resolver-fn (some frameworks will call this query)
;;                       ["Kim"  "Chi"]
;;                       ["Kim"  "Jong-un"]
;; UESCASE: User can specify dynamic multi-criteria-sort-order of rows in a table
;;          1 user selects multiple sort-criteria
;;          2 program applies sort-criteria to comparator function
;; USECASE: programmer can conveniently declare sort-order of table-rows contained in a table-cell
;; 
;; 7. All inputs to this lib must be vectors of <comparables | nil>
;;
;;

;; testing ...
;; (= (sort (partial arraycomp [nils-last nils-last]) [[1 nil] [1 1]]) [[1 1] [1 nil]])
;; (= (sort (partial arraycomp nil) [[1 [nil]] [1 [1]]]) [[1 [1]] [1 [nil]]])
;; => it only works for vectors with atomic values .. (not lists vectors etc)
;; => that is FINE - at least i think so atm :D

;; (= (sort (partial arraycomp [nils-last nils-last]) [[1 1 1] [1 1]]) [[1 1 1] [1 1]])
;; well this is what (sort garray/defaultCompare [[1 1 1] [1 1]]) will produce
;; "more content before less content!"

;; (= (sort (partial arraycomp [nils-last nils-last]) [[1 [nil]] [1 [1]]]) [[1 [1]] [1 [nil]]])
;; well this is what (sort garray/defaultCompare [[1 [nil]] [1 [1]]]) will produce
;; 


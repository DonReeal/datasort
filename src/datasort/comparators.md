# datasort.comparables - A DSL for declaring multi-field, nil friendly comparators in ClojureScript

## Sorting - A primitive for data discovery

## TLDR

```clojure
(require '[datasort.comparators :as dsc])

(def dataset
  [{:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
   {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
   {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
   {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}])

(sort ;; reproducing sort behaviour of (sort-by (juxt :api :eventid :duration) dataset)
  (dsc/partitioned-comparator
    [[:api compare]
     [:eventid compare]
     [:duration compare]])
  dataset)

(sort ;; nil as partition-value last
  (dsc/partitioned-comparator
    [[:api      dsc/nils-last)]
     [:eventid  dsc/nils-last]
     [:duration dsc/nils-last]]
  dataset)

(sort
  (dsc/partitioned-comparator
    [[:api      (dsc/nils-last-cmp #(compare %2 %1))]
     [:eventid  dsc/nils-last]
     [:duration dsc/nils-last]])
  dataset)

(sort ;; if dataset contains nil sort it last - not a very likely usecase for ui-data
  (dsc/partitioned-comparator
    [[nil? compare]
     [:id  compare]])
  (into [nil] dataset))

(sort ;; if dataset contains nil sort it last - not a very likely usecase for ui-data
  (dsc/partitioned-comparator
    [[nil? compare]
     [:id  compare]])
  (into [nil] dataset))
```

TODO: clean this up to document my journey to that little custom dsl

## Declarative multi-field sorting in ClojureScript

Consider you just exported some logs via a timestamp range and want to explore them. Typically you would start playing around with the records **by sorting them to group them visually** to find similarities and differences. So imagine you were interested in request times montoring for each api-endpoint that is running in production. Simplest core API to use  to implement a sorting UI that I know of would be to use sort-by.

## Limits of sort-by

```clojure
(def dataset0
  [{:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-xxx-xxx-1"}
   {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-xxx-xxx-1"}
   {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-xxx-xxx-2"}])

(map :id (sort-by :duration dataset0))             ;; => (2 3 1)
(map :id (sort-by (juxt :api :duration) dataset0)) ;; => (3 2 1)

(sort-by (juxt :api :duration) dataset0)
;; =>
;; [{:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
;;  {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
;;  {:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}]

;; For sorting the records are replaced by with a vector representation that contains naturally sortable values.
;; Sort partitions are represented by the index position in the vector
;; The order of occurence defines the order of the sort criteria that shall be applied
(map (juxt :api :duration) dataset0)
;; =>
;; (["POST /foo" 200]
;;  ["POST /foo" 150]
;;  ["POST /bar" 150])
```

### Sorting nils last requires custom comparator that is coupled to the structure of keyfn

The above shows a really simple way to sort in partitions - using this api you cannot control sorting within the vector.
When introducing nils as possible values - things get harder. Considering the log example again: in the real world your logs are not perfect and structure keeps evolving. This is where you will have to deal with nils:

```clojure
(def dataset1
  [{:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
   {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
   {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
   ;; new deployment changed the logged properties - :api will be nil
   {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}])

;; this is where sort-by does not work as I would like to ... *nils must be sorted last*
(sort-by (juxt :api :duration) dataset1)
;; =>
;;({:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}
;; {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
;; {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
;; {:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"})
```

### Declaring either reverse or natural order by partition requires custom comparator by partition

## Limits of representing data as vectors of sort fields

In Clojure and ClojureScript equal-length vectors are compared lexicographically and might be suited to
represent sort order of arbitrary data. But as described in [Clojure Comparators Guide](https://clojure.org/guides/comparators "Clojure Comparator Guide") this approach is limited to the natural order that *clojure.core/compare* privides. That means **sorting nils last is not supported**.
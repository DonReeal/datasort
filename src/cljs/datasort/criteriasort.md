# criteriasort -  a DSL for multi-field comparators

## TLDR

```clojure
(require '[datasort.criteriasort :as csort])

(def dataset
  [{:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
   {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
   {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
   {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}])

(csort/sort-by-many
  [[:api      (csort/cmp-fn ::csort/asc)]
   [:eventid  (csort/cmp-fn ::csort/asc)]
   [:duration (csort/cmp-fn ::csort/desc)]
   [:id       (csort/cmp-fn ::csort/asc)]]
  dataset)
```

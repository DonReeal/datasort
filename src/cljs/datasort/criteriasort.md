# criteriasort -  a DSL for multi-field comparators

## TLDR

```clojure
(require '[datasort.criteriasort :as csort])
  
(def dataset
  [{:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
   {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
   {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
   {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}])
   

;; sort-by-criteria takes a seq of resolver-fn, compare-fn pairs 
;; to build a compare function dynamically
;; and sorts the values in the passed collection
(let [criterion0 [:sessionid #(compare %1 %2)]
      criterion1 [:id        #(compare %2 %1)]
      criteria [criterion0 criterion1]]
  (mapv 
    :id 
    (csort/sort-by-criteria criteria dataset))) ;; => [2 1 4 3]
  
;; csort has cmp util to declare order of nil values
(sort #(csort/cmp ::csort/nils-las**t ::csort/asc %1 %2) [nil 3 2 nil 2 3 nil]) ;; => (2 2 3 3 nil nil nil)
  
;; as passing ascending as ::asc and also passing in order  of comparison via %1 %2 ... a version easier to the eye
(sort (csort/cmp-fn ::csort/nils-last ::csort/asc) [nil 3 2 nil 2 3 nil]) ;; => (2 2 3 3 nil nil nil)
 
;; the utils are not meant for declaring order of the data to sort
;; nils usually do not make sense there ...
;; they are meant to declare compare functions within a criterion
;; see dataset - it contains nils in properties :api and :eventid
(csort/sort-by-criteria
  [[:api      (csort/cmp-fn ::csort/nils-last ::csort/asc)]
   [:eventid  (csort/cmp-fn ::csort/nils-last ::csort/asc)]
   [:duration (csort/cmp-fn ::csort/nils-last ::csort/desc)]
   [:id       (csort/cmp-fn ::csort/nils-last ::csort/asc)]]
  dataset)

```

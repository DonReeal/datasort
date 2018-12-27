(ns datasort.comparables
    (:require [cljs.reader :as reader]))

;; probably not a very good idea to wrap all "primitives" in an object 
;; but lets acutally decide upon benchmarking it
;; I think this should be pretty slow

(def dataset
  [{:localtime "2018-12-20T14:58:00.000-00:00"
    :level "INFO"
    :serviceid "tablezz-svc"
    :event "session-initialized"
    :api "/session"
    :success true}
   {:localtime "2018-12-20T14:58:10.000-00:00"
    :level "INFO"
    :serviceid "tablezz-svc"
    :event "table-read"
    :api "/read-table"}
   {:localtime "2018-12-20T14:58:11.001-00:00"
    :level "WARN"
    :serviceid "tablezz-svc"
    :event "remote-unavailable"
    :operation  "remote:tablezz-kassandra/get"
    :detail "Could not reach remote - Retrying ..."
    :api "/read-table"
    :success true}
   {:localtime "2018-12-20T14:58:11.101-00:00"
    :level "INFO"
    :serviceid "tablezz-svc"
    :event "performance-monitoring"
    :operation "remote:tablezz-kassandra/get"
    :metric 1100
    :api "/read-table"}
   {:localtime "2018-12-20T14:58:11.113-00:00"
    :level "INFO"
    :serviceid "tablezz-svc"
    :event "table-read-ok"
    :api "/read-table"
    :metric 1112
    :success true}
   {:localtime "2018-12-20T14:58:11.113+01:00"
    :level "INFO"
    :serviceid "tablezz-svc"
    :event "table-read-ok"
    :api "/read-table"
    :metric 115
    :success true}])


;; this might be the only comparator i need at all
;; at least when  data-import-phase prevents mixing types
(defn nils-last [a b]
  (if (nil? a)
    (if (nil? b) 
        0 
        1)
    (if (nil? b) 
        -1 
        (compare a b))))

;; ClojureScript extension (in java I could just user Comparators.(...))
(defprotocol IDecorator
  (raw-value [this] "Returns decorated value"))

(defrecord ComparableDecorator [value]
  IDecorator
  (raw-value [_] value)
  IComparable
  (-compare [this other]
    (if identical? this other) 0
      (let [this-value value
            other-value (raw-value other)]
        (nils-last this-value other-value))))

(defn compose-sort-vector [resolvers record]
  (->> resolvers 
    (map (fn [resolver-fn] (resolver-fn record)))
    (mapv (fn [sort-value] (->ComparableDecorator sort-value)))))

;; example usage solution 1
(->> dataset
     (map #(vector (compose-sort-vector [:metric :operation :level] %) %))
     (sort-by first)
     (map second))

;; example with higher order resolver function
(->> dataset 
     (map (fn [record] [(compose-sort-vector [#(reader/read-date (:localtime %))] record) record]))
     (sort-by first)
     (map second))

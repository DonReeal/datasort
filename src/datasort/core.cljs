(ns datasort.core
    (:require [cljs.reader]
              [cljs.pprint]
              [clojure.spec.alpha :as spec]
              [goog.array :as garray]
              [datasort.comparators :as comparators]
              [datasort.criteriasort :as csort]))

(enable-console-print!)

;; 
;; 1. pick sort criteria
;;    => 1.1 build index -> render visible records
;; 2. select "visualize groups"
;;    => render visible records in groups; if this becomes a major feature; maybe indexing as group-by would be useful to begin with ...
;; 

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
(defonce state (atom {}))

;; selfmade mini console debugger: traces all changes in state
(add-watch state :trace-state-changed
  (fn [key atom old-state new-state]
    (println (str "== " key " =="))
    (cljs.pprint/pprint new-state)))

;; set initial state
(let [logs (into (sorted-map) (map #(vector (:id %) %) dataset))]
  (swap! state assoc 
    :logs-by-id logs
    :logs-sorted-ids (apply vector (keys logs))))

;; sort logs
(let [criteria-cmp (comparators/partitioned-comparator
                     [[:metric comparators/nils-last]
                      [:operation comparators/nils-last]
                      [:level comparators/nils-last]
                      [:id compare]])]  ; falling back to id to sort deterministically, not all records do have one of [:metric :operation :level]
  (->> (sort criteria-cmp (vals (:logs-by-id @state)))
       (mapv :id)
       (swap! state assoc :logs-sorted-ids)))

(let [state @state
      sorted-ids (:logs-sorted-ids state)
      logs-by-id (:logs-by-id state)]
  (println "dataset in order:")
  (->> sorted-ids
      (map #(get logs-by-id %))
      (cljs.pprint/pprint)))

(ns datasort.core
    (:require [cljs.reader :as reader]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "Hello world!"}))

(println "This text is printed from src/datasort/core.cljs. Go ahead and edit it and see reloading in action.")

(defn on-js-reload [])
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

;; 1 import - user must be presented the possible selections on import csv
;; 2 config - user should chose the index types to be used for rendering table
;; 3 edit, enjoy ...


;; this should be preconfigured by heuristic
;; user might want to accept or change that
(def index-config
  {:localdate :index/inst
   :level :index/text
   :serviceid :index/text
   :event :index/text
   :api :index/text
   :success :index/bool
   :metric :index/num})


(def table-cols ;; order as is defined in the vec
  [{:resolver :localdate :index :index/inst}
   {:resolver :level     :index :index/text}
   {:resolver :serviceid :index :index/text}
   {:resolver :event     :index :index/text}
   {:resolver :api       :index :index/text}
   {:resolver :success   :index :index/bool}
   {:resolver :metric    :index :index/num}])

;; this might be the only comparator i need at all
;; at least when  data-import-phase prevents mixing types
(defn nils-last [a b]
  (if (nil? a)
    (if (nil? b) 0 1)
    (if (nil? b) -1 (compare a b))))

;; defmulti required? maybe cond better ...
(defmulti resolve-value :index)
(defmethod resolve-value :index/text [m k] (str (get m k nil)))

(def record0
  {:localtime "2018-12-20T14:58:00.000-00:00"
   :level "INFO"
   :serviceid "tablezz-svc"
   :event "session-initialized"
   :api "/session"
   :success true})

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
  

;; should ensure that all types getting indexed can be sorted
;; https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html
;; string, numeric, boolean, date



;; (sort #(nils-last %1 %2) [nil "alice" "aa" "ab" "b" "a"])


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
;; the extracted vector hast the content nil - comparing [nil] [123] compares the values in natural order)
;;

;; POSSIBLE SOLUTIONS THEN::::::::::::::::::::::::::::::::::::::::
;; 1. Reifiy as comparable with special null treatment
;;    consider using "decorate-sort-undecorate" technique to reify only once
;;    WELL THIS WAS HARD TOO AND requires java/javascript specific integration
;;    :/ but kind of done - performance wont be too good i guess
;; 2. Build custom datasort function (dont use core.sort / core.compare) 
;;    which is hard to do as we need to implement cljs too then


(->> dataset
    (map (fn [record] [((juxt :metric :localtime) record) record]))
    (sort-by first #(nils-last %1 %2) ,,,)
    (map second))

    
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
      
;; HAHA it works
(cljs.pprint/pprint          
  (->> dataset
    (map (fn [record] 
          [((juxt #(->ComparableDecorator (:metric %))
                  #(->ComparableDecorator (:operation %))
                  #(->ComparableDecorator (:level %)))
            record)
           record]))
    (sort-by first)
    (map second)))
    

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
     (map (fn [record] 
            [(compose-sort-vector [#(reader/read-date (:localtime %))] record)
             record]))
     (sort-by first)
     (map second))
          
     

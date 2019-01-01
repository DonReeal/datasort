(ns datasort.core
    (:require [cljs.reader]
              [cljs.pprint]
              [clojure.spec.alpha :as spec]
              [goog.array :as garray]
              [datasort.comparators :as comparators]
              [datasort.criteriasort :as csort]
              [reagent.core :as reagent]))

(enable-console-print!)

;; 
;; 1. user selects sort sort criteria
;;    => 1.1 build index -> render visible records
;; 2. select "visualize groups"
;;    => render visible records in groups; if this becomes a major feature; maybe indexing as group-by would be useful to begin with ...
;; 

;; ==============================================================================
(def state (reagent/atom {}))

;; selfmade mini console debugger: traces all changes in state
(add-watch state :trace-state-changed
  (fn [key atom old-state new-state]
    (println (str "== " key " =="))
    (cljs.pprint/pprint new-state)))

;; ==============================================================================
;; set initial app state

(def dataset
  [{:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
   {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
   {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
   {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}])

(let [logs (into (sorted-map) 
                 (map #(vector (:id %) %) dataset))]
  (swap! state assoc 
    :logs-by-id logs
    :sort-criteria [[:id (csort/cmp-fn ::csort/asc)]]))
    
(defn update-sort-criteria [new-value]
  (swap! state assoc :sort-criteria new-value))

;; ==============================================================================
;; actions:
(defn update-sort-criterion [resolver-kw]
  (println (str "TODO: implement updating sort order by clicking on sort-criterion - " resolver-kw)))

(defn sorted-records []
  (let [state @state
        index (:logs-by-id state)
        sort-criteria (:sort-criteria state)]
      (csort/sort-by-many sort-criteria (vals index))))

;; ==============================================================================
;; see https://github.com/reagent-project/reagent-cookbook/blob/master/recipes/sort-table/src/cljs/sort_table/core.cljs
(defn table []
  [:table
    [:thead
      [:tr
        [:th {:width "200" :on-click #(update-sort-criterion :id)} "id"]
        [:th {:width "200" :on-click #(update-sort-criterion :api) } "api"]
        [:th {:width "200" :on-click #(update-sort-criterion :eventid) } "eventid"]
        [:th {:width "200" :on-click #(update-sort-criterion :duration) } "duration"]
        [:th {:width "200" :on-click #(update-sort-criterion :sessionid) } "sessionid"]]]
    [:tbody
      (for [record (sorted-records)]
        ^{:key (:id record)} 
        [:tr
          [:td (:id record)]
          [:td (:api record)]
          [:td (:eventid record)]
          [:td (:duration record)] 
          [:td (:sessionid record)]])]])
    
(defn home []
  [:div {:style {:margin "auto"
                 :padding-top "30px"
                 :width "600px"}}
    [:h1 "Datasort demo"]
    [:p "sample represents log data - records with arbitrary fields"]
    [:h4 "Try resorting by using datasort.core/update-sort-criteria"]
    [:p "E.g. type in your repl:"]
    [:ul
      [:li "(in-ns 'datasort.core)"]
      [:li "(update-sort-criteria [[:sessionid (csort/cmp-fn ::csort/desc)][:duration (csort/cmp-fn ::csort/desc)]])"]
      [:li "(update-sort-criteria [[:sessionid (csort/cmp-fn ::csort/asc)][:duration (csort/cmp-fn ::csort/desc)]])"]]
    [table]])
    
(defn ^:export main []
  (reagent/render [home] (.getElementById js/document "app")))

  

(update-sort-criteria [[:api (csort/cmp-fn ::csort/asc)]])
   ;; [:api (csort(cmp-fn ::csort/desc))]])
   


(ns datasort.core
    (:require [cljs.reader]
              [cljs.pprint]
              [clojure.spec.alpha :as spec]
              [datasort.criteriasort :as csort]
              [reagent.core :as reagent]))

(enable-console-print!)

;; ==============================================================================
;; set initial app state
(def dataset
  [{:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
   {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
   {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
   {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}])

(defonce state
  (reagent/atom 
    {:logs-by-id (into (sorted-map) (map #(vector (:id %) %) dataset))
     ;; the rows selected to display
     :columns [{:col-id "id"        :resolver-fn :id} 
               {:col-id "api"       :resolver-fn :api} 
               {:col-id "eventid"   :resolver-fn :eventid} 
               {:col-id "duration"  :resolver-fn :duration} 
               {:col-id "sessionid" :resolver-fn :sessionid}]
     ;; default sort-criteria to be rendered
     :sort-criteria [["id"        [:id {}]] 
                     ["api"       [:api {}]] 
                     ["eventid"   [:eventid {}]] 
                     ["duration"  [:duration {}]]
                     ["sessionid" [:sessionid {}]]]

     :sort-criteria2 [["id"        :id ::asc]
                      ["api"       :api ::asc]
                      ["eventid"   :eventid ::asc]
                      ["duration"  :duration ::asc]
                      ["sessionid" :sessionid ::asc]]}))

      

(defn asc-comparator []
  csort/compile-comparator {::csort/nils ::csort/last ::csort/order ::csort/asc })

(defn desc-comparator []
  (comp -1 asc-comparator))
                     

(add-watch state :trace-app-state
  (fn [key atom old-state new-state]
    (println (str "== " key " =="))
    (cljs.pprint/pprint new-state)))

;; ==============================================================================
;; actions
(defn update-sort-criteria [new-value]
  (swap! state assoc :sort-criteria new-value))

(defn toggle-sort-criterion [col-id]
  @state
  (let [{:keys [resolver-fn]} (->> (:columns @state)
                                   (filter #(= col-id (:col-id %)))
                                   (first))
        current-order-spec (->> (:sort-criteria @state)
                                (filter #(= col-id (first %)))
                                (first))]
    ;; TODO: to implement asc/desc toggle 
    ;; need to refactor sort-criteria ...
    (swap! state assoc 
      :sort-criteria 
      [[col-id [resolver-fn {}]]])))


;; ==============================================================================
;; queries
(defn q-columns []
  (:columns @state))

(defn q-sorted-records []
  (let [state @state
        index (:logs-by-id state)
        sort-criteria (map second (:sort-criteria state))]
      (csort/sort-by-criteria sort-criteria (vals index))))

;; ==============================================================================
;; see https://github.com/reagent-project/reagent-cookbook/blob/master/recipes/sort-table/src/cljs/sort_table/core.cljs

(defn table []
  [:table
    [:thead
      [:tr
        (for [{:keys [col-id]} (q-columns)]
          ^{:key col-id}
          [:th {:width "200" :on-click #(toggle-sort-criterion col-id)} col-id])]]
    [:tbody
      (for [record (q-sorted-records)] 
        ^{:key (:id record)} 
        [:tr ;; todo render based on selected columns
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
    [:p "Sample uses log data - records with arbitrary fields"]
    [table]])
    
    
(defn ^:export main []
  (reagent/render [home] (.getElementById js/document "app")))

(comment
  ;; Play around resorting the table in your repl:
  (in-ns 'datasort.core)
  
  ;; sorting by a row ascending
  (toggle-sort-criterion "id")
  (toggle-sort-criterion "eventid")

  ;; TODO this does not work atm ...
  (update-sort-criteria 
    [["sessionid" [true true]
      "api"       [true true]]])

  (update-sort-criteria [[:sessionid (csort/cmp-fn ::csort/asc)][:duration (csort/cmp-fn ::csort/desc)]]))
   


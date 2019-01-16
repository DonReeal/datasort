(ns datasort.core
    (:require [cljs.reader]
              [cljs.pprint :as pp]
              [clojure.spec.alpha :as spec]
              [datasort.criteriasort :as csort]
              [datasort.criteriasort2 :as csort2]
              [reagent.core :as reagent]))

(enable-console-print!)

;; ==============================================================================
;; app state, init

(def dataset
  [{:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
   {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
   {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
   {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}])

(defonce state
  (reagent/atom 
    {:logs-by-id (into (sorted-map) (map #(vector (:id %) %) dataset))
     :columns ;; the columns selected to display
     [{:col-id "id"        :resolver-fn :id} 
      {:col-id "api"       :resolver-fn :api} 
      {:col-id "eventid"   :resolver-fn :eventid} 
      {:col-id "duration"  :resolver-fn :duration} 
      {:col-id "sessionid" :resolver-fn :sessionid}]
     :columns-by-id ;; table model - TODO is associative access required or would a vector do?
     {"id"        {:resolver-fn :id} 
      "api"       {:resolver-fn :api} 
      "eventid"   {:resolver-fn :eventid} 
      "duration"  {:resolver-fn :duration} 
      "sessionid" {:resolver-fn :sessionid}}
     ;; default sort-criteria to be rendered
     :sort-criteria ;; 
     [["id"        ::asc]
      ["api"       ::asc]
      ["eventid"   ::asc]
      ["duration"  ::asc]
      ["sessionid" ::asc]]}))


(add-watch state :trace-app-state
  (fn [key atom old-state new-state]
    (println (str "== " key " =="))
    (pp/pprint new-state)))

;; ==============================================================================
;; actions
(defn update-sort-criteria [new-value]
  (swap! state assoc :sort-criteria new-value))

(defn toggle-sort-criterion [col-id]
  @state
  (let [current-col-order-spec (->> (:sort-criteria @state)
                                    (filter #(= col-id (first %)))
                                    (first))]
    (println current-col-order-spec)
    (swap! state assoc 
      :sort-criteria
      (if (= ::asc (second current-col-order-spec))
        [[col-id ::desc]]
        [[col-id ::asc]]))))

;; ==============================================================================
;; queries


;; ==============================================================================

;; TODO: split in stateful component and render-fn

;; column-id, column-label, column-index

;; in order selection of the record properties to be displayed in the table
;; the order specifies the order of the columns in the table to be rendered in the table
(def colspec [{:col-id "id"        :label "id"        :resolver-fn :id}
              {:col-id "api"       :label "api"       :resolver-fn :api}
              {:col-id "eventid"   :label "eventid"   :resolver-fn :eventid}
              {:col-id "duration"  :label "duration"  :resolver-fn :duration}
              {:col-id "sessionid" :label "sessionid" :resolver-fn :sessionid}])

;; component local state impl

(defn sort-records [colspec orders records] ;; todo add orders
  (-> colspec
      (mapv
        (fn [{:keys [col-id resolver-fn order]}]
          ({::asc  {::csort/order ::csort/asc 
                    ::csort/nils ::csort/last}  
            ::desc {::csort/order ::csort/desc 
                    ::csort/nils ::csort/first}}
           order)))
      (csort/sort-by-criteria records)))

;; pure render
(defn render-table [{:keys [colspec records on-toggle-sort]}]
  [:table
    [:thead
      [:tr
        (for [[index {:keys [col-id label]}] (map-indexed vector colspec)]
          ^{:key col-id}
          [:th {:width "200" :on-click #(on-toggle-sort col-id)} label])]]
    [:tbody
      (for [record records]
        ^{:key (:id record)} 
        [:tr ;; todo render based on selected columns
          [:td (:id record)]
          [:td (:api record)]
          [:td (:eventid record)]
          [:td (:duration record)] 
          [:td (:sessionid record)]])]])

(defn component-table [records]
  (let [colspec (reagent/atom colspec)
        orders (reagent/atom (into {} (map #(vector (:col-id %) ::asc) @colspec)))]
    [render-table 
     {:colspec @colspec 
      :records records ;; todo sort em
      :on-toggle-sort (fn [col-id]
                        (let [newval (if (= ::asc (get @orders col-id)) ::desc ::asc)]
                          (swap! orders assoc col-id newval)         
                          (println orders)))}]))

(defn home []
  [:div {:style {:margin "auto"
                 :padding-top "30px"
                 :width "600px"}}
    [:h1 "Datasort demo"]
    [:p "Sample uses log data - records with arbitrary fields"]
    [component-table dataset]])
    
    
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
   


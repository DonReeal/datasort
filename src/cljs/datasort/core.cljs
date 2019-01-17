(ns datasort.core
    (:require [cljs.reader]
              [cljs.pprint :as pp]
              [clojure.spec.alpha :as spec]
              [datasort.criteriasort2 :as csort2]
              [reagent.core :as reagent]))

(enable-console-print!)

;; simulating import of some data into our table ui
(def dataset
  [{:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
   {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
   {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
   {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}])

(def colspec
  ;; need a dialogue for the user to specify this
  ;; minimal version:
  ;; 1. let user chose ident, or provide default ident by line number (e.g in csv)
  ;; 2. render ident only
  [{:col-id "id"        :label "id"        :resolver-fn :id}
   {:col-id "api"       :label "api"       :resolver-fn :api}
   {:col-id "eventid"   :label "eventid"   :resolver-fn :eventid}
   {:col-id "duration"  :label "duration"  :resolver-fn :duration}
   {:col-id "sessionid" :label "sessionid" :resolver-fn :sessionid}])


;; helpers to init table state from dataset
(defn init-records-by-id [ident-fn dataset]
  (into (sorted-map) (map #(vector (ident-fn %) %) dataset)))
;; (dataset-by-id :id dataset)

(defn reduce-keys [records]
  (into #{} conj (mapcat keys records)))
(defn reduce-keys2 [records]
  (reduce clojure.set/union #{} (map #(set (keys %)) records)))
;; (reduce clojure.set/union #{} (repeat 44000000 #{:a :b :c :d})) ;; => times js out
;; (into #{} conj (repeat 44000000 '(:a :b :c :d)))                ;; just about works

(defn init-colspec [ident-fn records]
  (let [first-colspec {:col-id (name ident-fn) :resolver-fn ident-fn :order :asc}
        otherkeys (disj (reduce-keys records) ident-fn)]
    (into
      [first-colspec]
      (->> otherkeys
           (map #(hash-map :col-id (name %) :resolver-fn % :order :asc))
           (sort-by :col-id)))))

;; (init-colspec :id dataset)
(defn init [ident-fn records]
  {:colspec (init-colspec  ident-fn records)
   :records-by-id (init-records-by-id ident-fn records)})


;; ==============================================================================
;; table app state, init
(def denormalized-table-state-model
  {;; initial columns definition for table row order and sort order definition
   :colspec ;; (init-colspec :id dataset)
   [{:col-id "id" :resolver-fn :id :order :asc}
    {:col-id "api" :resolver-fn :api :order :asc}
    {:col-id "duration" :resolver-fn :duration :order :asc}
    {:col-id "eventid" :resolver-fn :eventid :order :asc}
    {:col-id "sessionid" :resolver-fn :sessionid :order :asc}]
   ;; read index for rendering
   :records-by-id ;; (dataset-by-id :id dataset)
   {1 {:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
    2 {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
    3 {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
    4 {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}}})

;; nice: (= denormalized-table-state-model (init :id dataset))


(defonce state (reagent/atom denormalized-table-state-model))

(add-watch state :trace-app-state
  (fn [key atom old-state new-state]
    (println (str "== " key " =="))
    (pp/pprint new-state)))

;; ==============================================================================
;; actions

(defn toggle-col-order! [state col-index]
  (let [colspec0 (:colspec @state)
        col-colspec0 (nth colspec0 col-index)
        col-colspec1 (assoc col-colspec0 :order ({:asc :desc :desc :asc} (:order col-colspec0)))
        colspec1 (assoc colspec0 col-index col-colspec1)]
    (swap! state assoc :colspec colspec1)))


;; to swap the displayed columns order, change order criteria
(defn swap [v index1 index2]
  (assoc v index2 (v index1) index1 (v index2)))
;; (swap [:id :eventid :bla] 0 2)



;; ==============================================================================
;; queries
;; ==============================================================================


;; column-id, column-label, column-index

;; component local state impl =================================================================

;; pure render
(defn render-table [{:keys [colspec
                            records
                            on-toggle-col-order]}]
  [:table
    [:thead
      [:tr
        (for [[index {:keys [col-id label]}] (map-indexed vector colspec)]
          ^{:key col-id}
          [:th {:width "200" :on-click #(on-toggle-col-order col-id)} label])]]
    [:tbody
      (for [record records]
        ^{:key (:id record)} 
        [:tr ;; todo render based on selected columns
          [:td (:id record)]
          [:td (:api record)]
          [:td (:eventid record)]
          [:td (:duration record)] 
          [:td (:sessionid record)]])]])

;; TODO db, vs local state ....
(defn component-table [records]
  (let [colspec (reagent/atom colspec)
        orders (reagent/atom (into {} (map #(vector (:col-id %) ::asc) @colspec)))]
    [render-table 
     {:colspec
      @colspec
      :records
      records ;; todo sort em
      :on-toggle-col-order
      (fn [col-id]
        (let [vvv (get @orders col-id)
              newval ({:asc :desc
                       :desc :asc} vvv)]
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
  (in-ns 'datasort.core))
   


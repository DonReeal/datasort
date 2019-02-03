(ns datasort.core
    (:require [cljs.reader]
              [cljs.pprint :as pp]
              [clojure.spec.alpha :as spec]
              [datasort.criteriasort2 :as csort2]
              [reagent.core :as reagent]
              [reagent.ratom :as rg.ratom]))

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

;; (reduce clojure.set/union #{} (repeat 44000000 #{:a :b :c :d})) ;; => times js out
;; (into #{} conj (repeat 44000000 '(:a :b :c :d)))                ;; just about works
(defn reduce-keys
  "Returns all unique keys present in all records passed."
  [records]
  (into #{} conj (mapcat keys records)))


(defn init-colspec [ident-fn records]
  (let [first-colspec {:col-id (name ident-fn) :resolver-fn ident-fn :order :asc}
        otherkeys (disj (reduce-keys records) ident-fn)]
    (into
      [first-colspec]
      (->> otherkeys
           (map #(hash-map :col-id (name %) :resolver-fn % :order :asc))
           (sort-by :col-id)))))


(defn initial-state
  "Initializes the app state"
  [ident-fn records]
  {:colspec (init-colspec ident-fn records)
   :records-by-id (init-records-by-id ident-fn records)})


;; ==============================================================================
;; initial app state -- typed out for readability
;; to serve arbitraty data use (initial-state ident-fn records)
(def denormalized-table-state-model
  {;; initial columns definition for table row order and sort order definition
   :colspec
   [{:col-id "id"        :resolver-fn :id        :order :asc}
    {:col-id "api"       :resolver-fn :api       :order :asc}
    {:col-id "duration"  :resolver-fn :duration  :order :asc}
    {:col-id "eventid"   :resolver-fn :eventid   :order :asc}
    {:col-id "sessionid" :resolver-fn :sessionid :order :asc}]
   ;; read index for rendering
   :records-by-id
   {1 {:id 1 :api "POST /foo" :duration 200 :sessionid "xxx-1"}
    2 {:id 2 :api "POST /foo" :duration 150 :sessionid "xxx-1"}
    3 {:id 3 :api "POST /bar" :duration 150 :sessionid "xxx-2"}
    4 {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}}})

(println
  (str "initial-state and typed out denormalized-table-state-model equal? -- "
       (= denormalized-table-state-model (initial-state :id dataset))))


(defonce state (reagent/atom (initial-state :id dataset)))

(add-watch state :trace-app-state
  (fn [key atom old-state new-state]
    (println (str "== " key " =="))
    (pp/pprint new-state)))


;; ==============================================================================
;; cursors
;; ==============================================================================
(def *colspec (reagent/cursor state [:colspec]))
(def *records-by-id (reagent/cursor state [:records-by-id]))

;; ==============================================================================
;; actions

(defn toggle-col-order!
  "Invert sort order for sort criterion at column index col-index"
  [col-index]
  (let [colspec0 @*colspec
        col-colspec0 (nth colspec0 col-index)
        col-colspec1 (assoc col-colspec0 :order ({:asc :desc :desc :asc} (:order col-colspec0)))
        colspec1 (assoc colspec0 col-index col-colspec1)]
    (swap! state assoc :colspec colspec1)))


;; to swap the displayed columns order and to change order criteria
(defn swap [v index1 index2]
  (assoc v index2 (v index1) index1 (v index2)))


;; component local state impl =================================================================
;; pure render
(defn render-table [{:keys [colspec records on-toggle-col-order]}]
  [:table
    [:thead
      [:tr
        (for [[index {:keys [col-id]}] (map-indexed vector colspec)]
          ^{:key col-id}
          [:th {:width "200" :on-click #(on-toggle-col-order index)} col-id])]]
    [:tbody
      (for [record records]
        ^{:key (:id record)} 
        [:tr
          (for [{:keys [col-id resolver-fn]} colspec]
            ^{:key col-id}
            [:td (resolver-fn record)])])]])

(defn coltosort-criterion [{:keys [resolver-fn order]}]
  (let [criterion-comparator-fn
        ({:asc  #(csort2/cmp ::csort2/nils-last ::csort2/asc %1 %2)
          :desc #(csort2/cmp ::csort2/nils-last ::csort2/asc %2 %1)}
         order)]
    [resolver-fn criterion-comparator-fn]))


(defn sort-by-colspec [colspec records]
    (csort2/sort-by-criteria
      (map coltosort-criterion colspec)
      records))


(defn component-table []
  (let [colspec @*colspec
        *sorted-records (rg.ratom/reaction (sort-by-colspec @*colspec (vals @*records-by-id)))]
    [render-table
      {:colspec colspec
       :records @*sorted-records
       :on-toggle-col-order toggle-col-order!}]))


(defn home []
  [:div {:style {:margin "auto"
                 :padding-top "30px"
                 :width "600px"}}
    [:h1 "Datasort demo"]
    [:p "Sample uses log data - records with arbitrary fields"]
    [component-table]])
    
    
(defn ^:export main []
  (reagent/render [home] (.getElementById js/document "app")))

(ns datasort.core
    (:require [cljs.reader]
              [cljs.pprint :as pp]
              [datasort.criteriasort :as csort2]
              [reagent.core :as reagent]
              [reagent.ratom :as rg.ratom]))

(enable-console-print!)

;; Given a dataset in EDN - let the user sort by all keys that dataset contains
;; (currenly importing is not implemented yet)
(def dataset
  [{:id 1 :api     "POST /foo"   :duration 200 :sessionid "xxx-1"}
   {:id 2 :api     "POST /foo"   :duration 150 :sessionid "xxx-1"}
   {:id 3 :api     "POST /bar"   :duration 150 :sessionid "xxx-2"}
   {:id 4 :eventid "foo-created" :duration 150 :sessionid "xxx-2"}])

;; ==============================================================================
;; simulating json-import - currently a bunch of helpers to init table state
;; ==============================================================================

(defn unique-index
  "Creates a map to access the values in coll by its index.
   Currently throws error if the index-fn does not return a unique value for each entry in coll
   - that might change!"
  [index-fn coll]
  (reduce
    (fn [map item] (let [item-index-value (index-fn item)]
                     (if (contains? map item-index-value)
                       (throw (js/Error "NON-UNIQUE-IDENT-FN"))
                       (conj map [item-index-value item]))))
    (sorted-map-by <)
    coll))

;; TODO: dont trust this code - might blow up on huge datasets
;; (reduce clojure.set/union #{} (repeat 44000000 #{:a :b :c :d})) ;; => browser times out
;; (into #{} conj (repeat 44000000 '(:a :b :c :d)))                ;; => just about works
(defn reduce-keys
  "Returns all unique keys present in all records passed."
  [records]
  (into #{} conj (mapcat keys records)))

(defn initial-colspec [ident-fn records]
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
  {:columns       (initial-colspec ident-fn records)
   :records-by-id (unique-index ident-fn records)})


;; ==============================================================================
;; initial app state -- built from records with arbitrary fields
;; ==============================================================================

(defonce state (reagent/atom (initial-state :id dataset)))

(add-watch state :trace-app-state
           (fn [key atom old-state new-state]
             (println (str "== " key " =="))
             (pp/pprint new-state)))

;; to serve arbitrary data use (initial-state ident-fn records)
(comment
  ;; initial-app-state typed out for readability
  (def denormalized-table-state-model
    {;; initial columns definition for table row order and sort order definition
     :columns
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
      4 {:id 4                  :duration 150 :sessionid "xxx-2" :eventid "foo-created"}}})
  ;; TODO: move this to a test when writing importer
  (= denormalized-table-state-model (initial-state :id dataset)))


;; ==============================================================================
;; actions
;; ==============================================================================

(defn toggle-col-order!
  "Invert sort order for sort criterion at column index col-index"
  [col-index]
  (let [columns-0   (:columns @state)
        colspec-i-0 (nth columns-0 col-index)
        colspec-i-1 (assoc colspec-i-0 :order ({:asc :desc :desc :asc} (:order colspec-i-0)))
        columns-1   (assoc columns-0 col-index colspec-i-1)]
    (swap! state assoc :columns columns-1)))

;; to swap the displayed columns order and to change order criteria
(defn- swap [v index1 index2]
  (assoc v index2 (v index1) index1 (v index2)))

(defn col-shift-left!
  "Swaps the columns index position within the table with the column to its left"
  [col-index]
  (let [columns (:columns @state)
        shifted-columns (swap columns col-index (dec col-index))]
    (swap! state assoc :columns shifted-columns)))

(defn col-shift-right!
  "Swaps the columns index position within the table with the column to its right"
  [col-index]
  (let [columns (:columns @state)
        shifted-columns (swap columns col-index (inc col-index))]
    (swap! state assoc :columns shifted-columns)))


;; ==============================================================================
;; cursors
;; ==============================================================================

(def *columns       (reagent/cursor state [:columns]))
(def *records-by-id (reagent/cursor state [:records-by-id]))


;; ==============================================================================
;; ui components
;; ==============================================================================

;; TODO: make accessible header
;; https://www.w3.org/TR/wai-aria-practices/examples/grid/dataGrids.html

(defn render-table
  [{:keys [columns
           on-toggle-col-order
           on-col-shift-left
           on-col-shift-right
           records]}]
  [:table {:role "grid"}
    [:thead
      [:tr
        (for [[index {:keys [col-id order]}] (map-indexed vector columns)]
          ^{:key col-id}
          [:th {:aria-sort ({:asc "ascending" :desc "descending"} order)
                :width "200px"}
           ;; left nav
           (or (= index 0) nil [:span {:on-click #(on-col-shift-left index)} "< "])
           [:span {:role "button"
                   :on-click  #(on-toggle-col-order index)}
            col-id]
           ;; right nav
           (or (= index (- (count columns) 1)) nil [:span {:on-click #(on-col-shift-right index)} " >"])])]]
    [:tbody
      (for [record records]
        ^{:key (:id record)} 
        [:tr
          (for [{:keys [col-id resolver-fn]} columns]
            ^{:key col-id}
            [:td (resolver-fn record)])])]])


(defn colspec->sortcriterion [{:keys [resolver-fn order]}]
  (let [criterion-comparator-fn
        ({:asc  #(csort2/cmp ::csort2/nils-last ::csort2/asc %1 %2)
          :desc #(csort2/cmp ::csort2/nils-last ::csort2/asc %2 %1)}
         order)]
    [resolver-fn criterion-comparator-fn]))

(defn- sort-by-colspec [columns records]
    (csort2/sort-by-criteria
      (map colspec->sortcriterion columns)
      records))

(defn component-table []
  (let [columns @*columns
        *sorted-records (rg.ratom/reaction (sort-by-colspec @*columns (vals @*records-by-id)))]
    [render-table
      {:columns             columns
       :records             @*sorted-records
       :on-toggle-col-order toggle-col-order!
       :on-col-shift-left   col-shift-left!
       :on-col-shift-right  col-shift-right!}]))

(defn home []
  [:div {:style {:margin "auto"
                 :padding-top "30px"
                 :width "600px"}}
    [:h1 "Datasort demo"]
    [:p "Sample uses log data - records with arbitrary fields"]
    [component-table]])
    
    
(defn ^:export main []
  (reagent/render [home] (.getElementById js/document "app")))

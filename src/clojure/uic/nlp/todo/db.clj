(ns uic.nlp.todo.db
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [zensols.actioncli.util :refer (trunc defnlock)]
            [zensols.actioncli.dynamic :as dyn]
            [zensols.actioncli.resource :as res]
            [zensols.nlparse.parse :as p]
            [zensols.dataset.db :as db :refer (with-connection)]
            [uic.nlp.todo.corpus :as corp]))

(def ^:dynamic *low-class-count-threshold* 15)

(defonce ^:private conn-inst (atom nil))
;(ns-unmap *ns* 'conn-inst)

(def ^:dynamic *corpus-read-limit* Integer/MAX_VALUE)

(defn- read-corpus [add-fn]
  (->> (corp/read-anons :limit *corpus-read-limit*)
       (map (fn [{:keys [bgid board-id board-name id source short-url
                         class utterance] :as dmap}]
              (log/tracef "corp map: %s, utterance: %s" dmap utterance)
              (let [panon (p/parse utterance)
                    id (str id)
                    class (get dmap :class)
                    inst (-> [bgid board-id board-name id source short-url]
                             (#(select-keys dmap %))
                             (assoc :panon panon))]
                (log/debugf "adding class: %s, inst: <%s>" class (trunc inst))
                (add-fn id inst class))))
       doall))

(defn connection []
  (swap! conn-inst #(or % (db/elasticsearch-connection
                           "todo"
                           :url "http://localhost:10200"
                           :create-instances-fn read-corpus))))

(defn reset-instances []
  (reset! conn-inst nil))

(dyn/register-purge-fn reset-instances)

(defnlock class-labels-keep []
  (with-connection (connection)
    (->> (db/distribution)
         (filter (fn [{:keys [count]}]
                   (> count *low-class-count-threshold*)))
         (map :class-label)
         set)))

(defn reset-labels-keep []
  (-> (meta #'class-labels-keep) :init-resource (reset! nil)))

(dyn/register-purge-fn reset-labels-keep)

(defn- filter-low-class-counts [id]
  (contains? (class-labels-keep)
             (:class-label (db/instance-by-id id))))

(defn load-corpora
  "Load the corups."
  []
  (with-connection (connection)
    (db/instances-load)))

(defn anons
  "Return all annotations"
  [& opts]
  (with-connection (connection)
    (apply db/instances opts)))

(defn anon-by-id
  "Return an annotation using its ID."
  [& opts]
  (with-connection (connection)
    (apply db/instance-by-id opts)))

(defn divide-by-set
  "Create a test/train dataset."
  [train-ratio]
  (with-connection (connection)
    (db/divide-by-set train-ratio
                      :dist-type 'even
                      :filter-fn filter-low-class-counts)))

(defn divide-by-fold [& opts]
  (with-connection (connection)
    (apply db/divide-by-fold opts)))

(defn set-fold [fold]
  (with-connection (connection)
    (db/set-fold fold)))

(defn write-dataset []
  (with-connection (connection)
    (db/write-dataset :output-file "resources/todo-dataset.xls")))

(defn freeze-dataset []
  (with-connection (connection)
    (db/freeze-dataset :output-file "resources/todo-dataset.json")))

(defn stats
  "Return all annotations"
  [& opts]
  (with-connection (connection)
    (apply db/stats opts)))

(defn distribution
  "Get the label distribution across all todos."
  []
  (->> (anons :set-type :train-test)
       (map :class-label)
       (reduce (fn [res a]
                 (assoc res a (+ (or (get res a) 0) 1)))
               {})))

(ns uic.nlp.todo.thaw-db
  (:require [zensols.actioncli.dynamic :as dyn]
            [zensols.actioncli.util :refer (defnlock)]
            [zensols.dataset.thaw :as db :refer (with-connection)]))

(defnlock connection []
  (db/thaw-connection "todo" "resources/todo-dataset.json"))

(defn reset-connection []
  (-> (meta #'connection) :init-resource (reset! nil)))

(dyn/register-purge-fn reset-connection)

(defn instances-count []
  (with-connection (connection)
    (db/instances-count)))

(defn anon-by-id
  [id]
  (with-connection (connection)
    (db/instance-by-id id)))

(defn anons
  "Return all annotations"
  [& opts]
  (with-connection (connection)
    (apply db/instances opts)))

(defn distribution
  "Return a distribution on class label as list of vectors.  The first position
  is the label and the second the count for that respective label."
  []
  (with-connection (connection)
    (->> (anons)
         (map :class-label)
         (reduce (fn [res c]
                   (assoc res c (+ 1 (or (get res c) 0))))
                 {})
         (sort #(compare (second %2) (second %1))))))

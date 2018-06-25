(ns ^{:doc "This namespace is REPL prototyping fodder and *not* not real unit
    test cases."}  uic.nlp.todo.db-test
  (:require [clojure.test :refer :all]
            [uic.nlp.todo.db :refer :all]
            [zensols.actioncli.dynamic :as dyn]
            [zensols.dataset.db :as db :refer (with-connection)]))

(defn- main [& actions]
  (->> actions
       (map (fn [action]
              (case action
                -2 (dyn/purge)
                -1 (reset-instances)
                0 (load-corpora)
                1 (divide-by-set 0.8)
                2 (do
                    (dyn/purge)
                    (load-corpora)
                    (Thread/sleep (* 2 1000))
                    (divide-by-set 0.8)
                    (with-connection (connection)
                      (db/write-dataset :instance-fn #(-> % :panon :text))))
                3 (with-connection (connection)
                    (db/instance-count))
                4 (with-connection (connection)
                    (db/stats))
                5 (clojure.pprint/pprint (connection))
                6 (with-connection (connection)
                    (db/write-dataset :instance-fn #(-> % :panon :text)))
                7 (distribution))))
       doall))

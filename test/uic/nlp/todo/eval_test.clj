(ns ^{:doc "This namespace is REPL prototyping fodder and *not* not real unit
    test cases."}
    uic.nlp.todo.eval-test
  (:require [zensols.actioncli.dynamic :as dyn]
            [zensols.model.classifier :as cl]
            [zensols.model.execute-classifier :as ex :refer (with-model-conf)]
            [zensols.model.eval-classifier :as ec :refer (with-two-pass)]
            [uic.nlp.todo.feature :as f :refer (with-feature-context)]
            [uic.nlp.todo.db :as edb]
            [uic.nlp.todo.eval :refer :all]
            [uic.nlp.todo.db :as edb]))

(defn- main [& actions]
  (let [classifiers [:fast :tree :meta :lazy]
        meta-set :set-compare]
    (binding [ec/*default-set-type* :train-test
              cl/*rand-fn* (fn [] (java.util.Random. 1))
              edb/*low-class-count-threshold* 10]
      (with-model-conf (create-model-config)
        (with-feature-context
            (f/create-context :anons-fn f/instance-deref-anons-fn
                              :set-type :train)
          (->> (map (fn [action]
                      (case action
                        0 (dyn/purge)
                        1 (do (edb/divide-by-set 0.9)
                              (edb/stats))
                        2 (ec/print-best-results classifiers meta-set)
                        3 (ec/terse-results classifiers meta-set :only-stats? true)
                        4 (-> (ec/create-model classifiers meta-set)
                              (ec/train-model :set-type :train)
                              ec/write-model)
                        5 (-> (ex/read-model)
                              (ex/print-model-info :results? true))
                        6 (->> (ex/read-model)
                               ex/prime-model
                               ex/predict
                               ex/write-predictions)
                        7 (-> (ex/read-model)
                              ex/prime-model
                              ex/predict)
                        8 (ec/eval-and-write classifiers meta-set)
                        9 (write-arff)
                        10 (-> (ec/create-model classifiers meta-set)
                               (ec/train-model :set-type :train)
                               ex/prime-model
                               ex/predict
                               ex/write-predictions
                               )))
                    actions)
               doall))))))

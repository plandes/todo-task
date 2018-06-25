(ns ^{:doc "Evaluation of the model using features generated
by [[uic.nlp.todo.feature]]."
      :author "Paul Landes"}
    uic.nlp.todo.eval
  (:require [clojure.tools.logging :as log]
            [clojure.set :refer (union)]
            [zensols.actioncli.dynamic :as dyn]
            [zensols.model.classifier :as cl]
            [zensols.model.execute-classifier :refer (with-model-conf) :as ex]
            [zensols.model.eval-classifier :as ec :refer (with-two-pass)]
            [uic.nlp.todo.feature :as f :refer (with-feature-context)]
            [uic.nlp.todo.db :as adb]
            [uic.nlp.todo.eval :as ev]))

(defonce ^:private cross-fold-instances-inst (atom nil))
(defonce ^:private train-test-instances-inst (atom nil))

(defn feature-sets-set
  "Feature sets to use in the various evaluations of the model."
  []
  {:set-compare (list (concat (f/word-count-features)
                              '(elected-verb-id
                                token-average-length
                                pos-first-tag
                                pos-last-tag
                                similarity-top-label
                                similarity-score
                                pos-tag-ratio-noun))
                      (concat (f/word-count-features)
                              '(elected-verb-id
                                similarity-top-label
                                similarity-score
                                pos-tag-ratio-noun))
                      (concat (f/word-count-features)
                              '(elected-verb-id
                                token-average-length
                                pos-first-tag
                                pos-last-tag
                                pos-tag-ratio-noun))
                      (concat (f/word-count-features)
                              '(elected-verb-id
                                token-average-length
                                similarity-top-label
                                pos-first-tag
                                pos-last-tag
                                pos-tag-ratio-noun))
                      (concat '(elected-verb-id
                                token-average-length
                                pos-first-tag
                                pos-last-tag
                                similarity-top-label
                                similarity-score
                                pos-tag-ratio-noun))
                      '(similarity-top-label
                        pos-last-tag
                        word-count-contact
                        word-count-call
                        word-count-buy
                        word-count-calendar
                        word-count-pay-bill-online
                        pos-first-tag
                        word-count-plan-meal
                        word-count-email
                        word-count-postal
                        word-count-school-work
                        word-count-print))
   :set-sel (list '(similarity-top-label
                    pos-last-tag
                    word-count-contact
                    word-count-call
                    word-count-buy
                    word-count-calendar
                    word-count-pay-bill-online
                    pos-first-tag
                    word-count-plan-meal
                    word-count-email
                    word-count-postal
                    word-count-school-work
                    word-count-print))
   :set-1 (list (concat (f/word-count-features)
                        '(elected-verb-id
                          token-average-length
                          pos-first-tag
                          pos-last-tag
                          similarity-top-label
                          similarity-score
                          pos-tag-ratio-noun)))
   :set-2 (list (concat (f/word-count-features)
                        '(elected-verb-id
                          token-average-length
                          similarity-top-label
                          pos-first-tag
                          pos-last-tag
                          pos-tag-ratio-noun)))
   :set-3 (list (concat (f/word-count-features)
                        '(elected-verb-id
                          similarity-top-label
                          similarity-score
                          pos-tag-ratio-noun)))
   :set-4 (list (concat (f/word-count-features)
                        '(similarity-top-label
                          pos-last-tag
                          pos-first-tag)))
   :set-best '((similarity-top-label
                pos-last-tag
                pos-first-tag
                word-count-contact
                word-count-call
                word-count-buy
                word-count-calendar
                word-count-pay-bill-online
                word-count-plan-meal
                word-count-email
                word-count-postal
                word-count-school-work
                word-count-print))})

(defn reset-instances []
  (reset! cross-fold-instances-inst nil)
  (reset! train-test-instances-inst nil))

(dyn/register-purge-fn reset-instances)

(defn create-model-config
  "Create the model configuration for this evalation."
  []
  (letfn [(divide-by-set [divide-ratio]
            (adb/divide-by-set divide-ratio :shuffle? false)
            (reset! train-test-instances-inst nil))]
    (merge (f/create-model-config)
           {:cross-fold-instances-inst cross-fold-instances-inst
            :train-test-instances-inst train-test-instances-inst
            :feature-sets-set (feature-sets-set)
            :divide-by-set divide-by-set})))

(defmacro with-single-pass
  "Create bindings and contexts for a single pass train/test model evaluation."
  {:style/indent 0}
  [& body]
  `(binding [cl/*rand-fn* (fn [] (java.util.Random. 1))
             ec/*default-set-type* :train-test
             adb/*low-class-count-threshold* 10]
     (with-model-conf (create-model-config)
       (with-feature-context
           (f/create-context :anons-fn f/instance-deref-anons-fn
                             :set-type :train)
         (do ~@body)))))

(defn write-arff
  "Write a Weka ARFF file (handy for importing in R/scikit-learn etc)."
  []
  (binding [cl/*rand-fn* (fn [] (java.util.Random. 1))
            ec/*default-set-type* :train-test
            adb/*low-class-count-threshold* 0
            f/anons adb/anons
            f/anon-by-id adb/anon-by-id]
    (with-model-conf (create-model-config)
      (with-feature-context (f/create-context :anons-fn f/instance-deref-anons-fn
                                              :set-type :train)
        (dyn/purge)
        (ec/write-arff)))))

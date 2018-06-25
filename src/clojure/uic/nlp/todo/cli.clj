(ns uic.nlp.todo.cli
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp]
            [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.util :refer (trunc)]
            [zensols.model.eval-classifier :as ec]
            [zensols.model.execute-classifier :as ex]
            [uic.nlp.todo.db :as db]
            [uic.nlp.todo.feature :as fe]
            [uic.nlp.todo.eval :as ev :refer (with-single-pass)]
            [clojure.string :as s]))

(defn config-file-option []
  ["-c" "--config" "the configuration file path"
   :default "todocorp.conf"
   :required "<file>"
   :parse-fn io/file
   :validate [(fn [file]
                 (if (.exists file)
                   (do (->> (.getAbsolutePath file)
                            (System/setProperty "zensols.todocorp-config"))
                       true)
                   false))
              "Must be an existing file"]])

;; ["-s" "--step" "create a moving train/test split with increment step (0.03 is a good start)"
;;  :required "<double>"
;;  :parse-fn edn/read-string]

(defn output-file-option [default]
  ["-o" "--output" "output file name or '-' to print results"
   :default default
   :required "<file name|->"])

(defn metaset-option []
  ["-m" "--metaset" "features set as defined in eval.clj"
   :default :set-compare
   :required "<string>"
   :parse-fn keyword])

(defn classifiers-option []
  ["-a" "--classifiers" "comma separated classifier list"
   :default [:fast :lazy :tree :meta :slow]
   :required "<list>"
   :parse-fn (fn [classifiers]
               (->> classifiers
                    (#(s/split % #"\s*,\s*"))
                    (map (fn [csym]
                           (if (s/index-of csym ".")
                             (.newInstance (Class/forName csym))
                             (keyword csym))))
                    vec))])

(def load-corpora-command
  "CLI command to load the corpora into elastic search"
  {:description "load corpus data into ElasticSearch"
   :options
   [(lu/log-level-set-option)
    (config-file-option)]
   :app (fn [& _]
          (db/load-corpora))})

(def split-dataset-command
  "CLI command to split the dataset"
  {:description "split the data into train and test sets and dump the JSON representation to disk"
   :options [(lu/log-level-set-option)
             (config-file-option)
              ["-s" "--split" "number (0-1) to leave for training, remaining will be used for test"
               :default 0.9
               :required "<double>"
               :parse-fn edn/read-string
               :validate [#(and (> % 0.0) (< % 1.0)) "Must be a number between (0-1)"]]]
   :app (fn [{:keys [split]} & _]
          (log/infof "spliting data: %.2f" split)
          (db/divide-by-set split)
          (println "statistics:")
          (->> {:split (db/stats)
                :distribution (db/distribution)}
               pp/pprint)
          (db/freeze-dataset))})

(def features-command
  "CLI command to show features"
  {:description "show features"
   :options [(lu/log-level-set-option)
             (config-file-option)
             ["-f" "--features" "the number of features to display"
              :default 100
              :required "<integer>"
              :parse-fn edn/read-string]]
   :app (fn [{:keys [features]} & _]
          (println "Press CONTROL-C to quit")
          (fe/display-features :num-features features))})

(def print-evaluate-command
  "CLI command to evaluate the model"
  {:description "evaluate the model using a cross fold validation across feature sets"
   :options [(lu/log-level-set-option)
             (config-file-option)
             (classifiers-option)
             (metaset-option)]
   :app (fn [{:keys [metaset classifiers]} & _]
          (with-single-pass
            (ec/print-best-results classifiers metaset)))})

(def evaluates-spreadsheet-command
  "CLI command to evaluate and output results"
  {:description "evaluate the model and output the results to a spreadsheet"
   :options [(lu/log-level-set-option)
             (output-file-option (io/file "evaluation.xls"))
             (config-file-option)
             (classifiers-option)
             (metaset-option)]
   :app (fn [{:keys [output metaset classifiers]} & _]
          (with-single-pass
            (ec/eval-and-write classifiers metaset output)))})

(def predict-spreadsheet-command
  "CLI command to predict the model"
  {:description "evaluate the model, classify, and output the test set"
   :options [(lu/log-level-set-option)
             (output-file-option (io/file "predictions.csv"))
             (config-file-option)
             (classifiers-option)
             (metaset-option)]
   :app (fn [{:keys [output metaset classifiers]} & _]
          (with-single-pass
            (try
              (-> (ec/create-model classifiers metaset)
                  (ec/train-model :set-type :train)
                  ex/prime-model
                  ex/predict
                  (#(ex/write-predictions % output)))
              (catch Exception e
                (println (trunc e))))))})

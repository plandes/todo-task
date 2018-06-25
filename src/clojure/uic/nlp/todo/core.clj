(ns uic.nlp.todo.core
  (:require [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.parse :as p])
  (:require [uic.nlp.todo.version :as ver])
  (:gen-class :main true))

(defn- version-info []
  (println (format "%s (%s)" ver/version ver/gitref)))

(defn- create-action-context []
  (p/multi-action-context
   '((:repl zensols.actioncli.repl repl-command)
     (:load uic.nlp.todo.cli load-corpora-command)
     (:dsprep uic.nlp.todo.cli split-dataset-command)
     (:features uic.nlp.todo.cli features-command)
     (:print uic.nlp.todo.cli print-evaluate-command)
     (:evaluate uic.nlp.todo.cli evaluates-spreadsheet-command)
     (:predict uic.nlp.todo.cli predict-spreadsheet-command))
   :version-option (p/version-option version-info)))

(defn -main [& args]
  (lu/configure "todotask-log4j2.xml")
  (p/set-program-name "todotask")
  (-> (create-action-context)
      (p/process-arguments args)))

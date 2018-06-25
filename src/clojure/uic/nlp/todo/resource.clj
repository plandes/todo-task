(ns uic.nlp.todo.resource
  (:require [clojure.tools.logging :as log])
  (:require [zensols.model.classifier :as c])
  (:require [zensols.actioncli.resource :refer (resource-path) :as res]))

(defn initialize
  []
  (log/debug "initializing")
  (c/initialize)
  (res/register-resource :todocorp-config-file
                         :system-property "todocorp-config"))

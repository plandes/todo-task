(ns ^{:doc "Feature createion"
      :author "Paul Landes"}
    uic.nlp.todo.feature
  (:require [clojure.tools.logging :as log]
            [zensols.actioncli.dynamic :as dyn]
            [zensols.actioncli.util :refer (defnlock trunc)]
            [zensols.nlparse.parse :as p]
            [zensols.nlparse.feature.lang :as fe]
            [zensols.nlparse.feature.word :as fw]
            [zensols.nlparse.feature.word-count :as wc]
            [zensols.nlparse.feature.word-similarity :as ws]
            [zensols.model.weka :as weka]
            [zensols.model.execute-classifier :refer (with-model-conf)]
            [zensols.model.eval-classifier :as ec]
            [uic.nlp.todo.thaw-db :as tdb]
            [uic.nlp.todo.db :as edb]))

(def id-key :id)
(def class-key :agent)
(def ^{:dynamic true :private true} *context* nil)
(def ^:private wc-config (merge wc/*word-count-config*
                                {:words-by-label-count 10}))
(def ^:dynamic anons tdb/anons)
(def ^:dynamic anon-by-id tdb/anon-by-id)

(defnlock classes
  []
  (->> (anons :set-type :train-test)
       (map :class-label)
       distinct
       vec))

(defn reset []
  (-> (meta #'classes) :init-resource (reset! nil)))

(dyn/register-purge-fn reset)

(defmacro with-feature-context
  {:style/indent 1}
  [context & forms]
  `(binding [*context* ~context]
     ~@forms))

(defn create-features
  ([panon]
   (create-features panon nil))
  ([panon context]
   (log/debugf "creating features (context=<%s>) for <%s>"
               (trunc context) (trunc panon))
   (let [{:keys [word-count-stats]} context
         tokens (p/tokens panon)]
     (binding [wc/*word-count-config* wc-config]
       (merge (fe/verb-features (->> panon :sents first))
              (fw/token-features panon tokens)
              (fe/pos-tag-features tokens)
              (if word-count-stats
                (wc/label-count-score-features panon word-count-stats))
              (if word-count-stats
                (ws/similarity-features tokens word-count-stats)))))))

(defn- flatten-keys [adb-keys]
  (mapcat #(into [] %) adb-keys))

(defn create-feature-sets [& {:keys [context] :as adb-keys}]
  (log/debugf "creating features with keys=%s: %s"
              adb-keys (trunc adb-keys))
  (let [context (or context *context*)
        {:keys [anons-fn]} context
        anons (apply anons-fn (->> (flatten-keys adb-keys)
                                   (concat [:include-ids? true])))]
    (->> anons
         (map (fn [{:keys [class-label instance id]}]
                (merge {:utterance (:text instance)
                        id-key id}
                       {class-key class-label}
                       (create-features instance context)))))))

(defn create-context
  [& {:keys [anons-fn] :as adb-keys}]
  (let [fkeys (flatten-keys adb-keys)
        anons (apply anons-fn fkeys)]
    (log/debugf "creating context with key=%s anon count: %d"
                (trunc adb-keys) (count anons))
    (log/tracef "adb-keys: %s" (pr-str adb-keys))
    (binding [wc/*word-count-config* wc-config]
      (let [stats (wc/calculate-feature-stats anons)]
        {:anons-fn anons-fn
         :word-count-stats stats}))))

(defn word-count-features []
  (->> (classes)
       (map #(->> % (format "word-count-%s") symbol))))

(defn feature-metas [& _]
  (concat (ws/similarity-feature-metas (classes))
          [[:utterance 'string]]
          (fe/verb-feature-metas)
          (fw/token-feature-metas)
          (fe/pos-tag-feature-metas)
          (wc/label-word-count-feature-metas (classes))))

(defn- class-feature-meta []
  [class-key (classes)])

(defn create-model-config []
  {:name (name class-key)
   :context-fn #(:word-count-stats *context*)
   :set-context-fn #(array-map :word-count-stats %)
   :create-feature-sets-fn create-feature-sets
   :create-features-fn create-features
   :feature-metas-fn feature-metas
   :class-feature-meta-fn class-feature-meta
   :create-two-pass-context-fn create-context
   :model-return-keys #{:label :distributions :features}})

(defn instance-deref-anon-fn [id]
  (-> (anon-by-id id)
      :instance
      :panon))

(defn instance-deref-anons-fn [& keys]
  (->> (apply anons keys)
       (map (fn [{:keys [class-label instance id]}]
              {:class-label class-label
               :id id
               :instance (:panon instance)}))))

(defn display-features [& {:keys [num-features]
                           :or {num-features 100}}]
  (with-feature-context
      (create-context :anons-fn instance-deref-anons-fn)
    (with-model-conf (create-model-config)
      (ec/display-features :max num-features))))

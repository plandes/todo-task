(ns ^{:doc "This namespace parses the corpus from an Excel file."}
    uic.nlp.todo.corpus
  (:require [clojure.string :as s]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.brainbot.iniconfig :as iniconfig]
            [zensols.actioncli.resource :as res]
            [zensols.util.spreadsheet :as ss :refer (with-read-spreadsheet)]
            [uic.nlp.todo.resource :as ur]))

(ur/initialize)

(defn annotation-info
  "Return information from the `todocorp.conf` configuration file."
  []
  (let [sec (-> (res/resource-path :todocorp-config-file)
                iniconfig/read-ini
                (#(get % "default")))]
    (log/debugf "annotation info from %s" sec)
    (->> sec
         (filter (fn [[k v]]
                   (re-matches #"^annotator\d+" k)))
         (map second)
         (hash-map :annotators)
         (merge {:main-annotator (get sec "annotator_main")
                 :annotated-dir (get sec "annotated_dir")
                 :results-dir (get sec "results_dir")
                 :serialized-dir (get sec "serialized_dir")}))))

(defn annotated-file
  "Return the annoatated todo corpus spreadsheet file."
  ([] (annotated-file nil))
  ([annotator]
   (let [inf (annotation-info)
         annotator (or annotator (:main-annotator inf))
         file (-> inf
                  :annotated-dir
                  (io/file (format "%s.xlsx" annotator)))]
     (if-not (.exists file)
       (-> (format "Un-annotated file not found: %s" file)
           (ex-info {:file file})
           throw))
     file)))

(defn read-for-annotator
  "Return a list of maps, each with a Todo list data point."
  [& {:keys [limit annotator]
      :or {limit Integer/MAX_VALUE}}]
  (let [file (annotated-file annotator)]
    (with-read-spreadsheet [file rows type]
      (->> rows
           ;(#(ss/rows-to-maps % ))
           ss/rows-to-maps
           (map (fn [id {:keys [bgid board-id board-name
                                        ;id
                                source short-url
                                class utterance]}]
                  (let [class (and class (->> (s/trim class) s/lower-case))
                        utterance (and utterance (str utterance))
                        utterance (and utterance (s/trim utterance))]
                    (when (and class utterance)
                      {:id (int id)
                       :bgid bgid
                       :board-id board-id
                       :source source
                       :class (if (> (count class) 0) class)
                       :utterance utterance
                       })))
                (range))
           (remove nil?)
           (take limit)
           doall))))

(defn- anons-by-ids
  "Create annotations with unique identifiers."
  [annotator & {:keys [limit]}]
  (->> (read-for-annotator :annotator annotator)
       (map (fn [{:keys [id] :as elt}]
              {id (assoc elt ;(select-keys elt [:class :utterance])
                         :annotator annotator)}))
       (take limit)
       (apply merge)))

(defn coder-agreement
  "Create the output file used by R to create inercoder agreement (Cohen's
  Kappa)."
  [& {:keys [annotators limit]
      :or {limit Integer/MAX_VALUE}}]
  (let [info (annotation-info)
        {:keys [results-dir]} info
        annotators (or annotators (:annotators info))
        outfile (io/file results-dir "intercoder.csv")
        ;; annotator -> annotation
        by-annotator (->> annotators
                          (map (fn [annotator]
                                 {annotator (anons-by-ids annotator :limit limit)}))
                          (apply merge))
        ;; ids of utterances annotated by all annotators
        shared-ids (->> by-annotator
                        vals
                        (map #(-> % keys set))
                        (apply clojure.set/intersection))]
    (->> shared-ids
         ;; lists of maps id -> annotation list of all annotators
         (map (fn [id]
                (->> annotators
                     (map (fn [annotator]
                            (let [by-id (get by-annotator annotator)]
                              (get by-id id))))
                     (hash-map :id id :anon-list))))
         ;; rows of ID an class of each annotator
         (map (fn [{:keys [id anon-list]}]
                (let [utterances (map :utterance anon-list)]
                  ;; sanity check
                  (if (> (count (distinct utterances)) 1)
                    (-> (format "unaligned utterances for id %s: %s"
                                id (s/join utterances))
                        (ex-info {:id id
                                  :annotation-list anon-list})
                        throw
                        ;(#(log/warnf "unaligned: %s" %))
                        )))
                (cons id (map :class anon-list))))
         ;; CSV header (for R colnames later)
         (cons (cons "id" annotators))
         ((fn [data]
            (with-open [writer (io/writer outfile)]
              (csv/write-csv writer data)))))
    (log/infof "wrote intercoder agreement file: %s" outfile)
    outfile))

(defn read-anons
  "Read annotations from the Excel file."
  [& {:keys [annotators limit]
      :or {limit Integer/MAX_VALUE}}]
  (let [info (annotation-info)
        {:keys [results-dir]} info
        annotators (or annotators (:annotators info))
        ;; annotator -> annotation
        by-annotator (->> annotators
                          (map (fn [annotator]
                                 {annotator (anons-by-ids annotator :limit limit)}))
                          (apply merge))]
    (->> annotators
         reverse
         (map (fn [annotator]
                (anons-by-ids annotator :limit limit)))
         (reduce (fn [res n]
                   (merge res n)))
         vals)))

(defn serialize-annotations
  "Write annotations in an intermedia binary serialization file.
  **Note**: this should not be confused with the JSON generation,
  which [[uic.nlp.todo.db/freeze-dataset]]."
  []
  (let [{:keys [serialized-dir]} (annotation-info)
        out-file (io/file serialized-dir "annotations.dat")]
    (with-open [writer (io/writer out-file)]
      (binding [*out* writer]
        (println (pr-str (read-anons)))))
    (log/infof "wrote Clojure serialized annotations data to %s" out-file)))

(defn deserialize-annotation
  "See [[serialize-annotations]]."
  []
  (let [{:keys [serialized-dir]} (annotation-info)
        in-file (io/file serialized-dir "annotations.dat")]
    (log/infof "reading annotations data from" in-file)
    (with-open [reader (io/reader in-file)]
      (->> reader
           slurp
           read-string))))

(defn ^:deprecated metrics
  "Generate somewhat useful metrics (depreciated)."
  []
  (letfn [(source-dist [anons]
            (->> anons
                 (map (fn [{:keys [source]}]
                        (if (re-matches #"^person.*" source)
                          "volunteer"
                          source)))
                 (reduce (fn [res source]
                           (merge res {source (inc (or (get res source) 0))}))
                         {})))]
    (merge (->> (read-anons :annotators ["annotator1" "annotator2" "annotator3" "annotator4"])
                source-dist
                (array-map :annotated))
           (->> (read-anons :annotators ["relabeled"] ;["annotator1" "annotator2"]
                            )
                source-dist
                (array-map :used)))))


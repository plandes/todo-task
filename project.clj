(defproject edu.uic.nlp/todotask "0.1.0-SNAPSHOT"
  :description "Categorize natural language todo list items"
  :url "https://github.com/plandes/todo-task"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"
            :distribution :repo}
  :plugins [[lein-codox "0.10.3"]
            [lein-javadoc "0.3.0"]
            [org.clojars.cvillecsteele/lein-git-version "1.2.7"]]
  :codox {:metadata {:doc/format :markdown}
          :project {:name "Todo Categorization"}
          :output-path "target/doc/codox"
          :source-uri "https://github.com/plandes/todo-task/blob/v{version}/{filepath}#L{line}"}
  :javadoc-opts {:package-names ["edu.uic.nlp.todo-task"]
                 :output-dir "target/doc/apidocs"}
  :git-version {:root-ns "uic.nlp.todo"
                :path "src/clojure/uic/nlp/todo"
                :version-cmd "git describe --match v*.* --abbrev=4 --dirty=-dirty"}
  :source-paths ["src/clojure"]
  :test-paths ["test" "test-resources"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"]
  :jar-exclusions [#".gitignore"]
  :exclusions [com.zensols.tools/actioncli
               ch.qos.logback/logback-classic
               log4j
               org.slf4j/slf4j-log4j12
               org.yaml/snakeyaml]
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; logging for core
                 [org.apache.logging.log4j/log4j-1.2-api "2.7"]
                 [org.apache.logging.log4j/log4j-core "2.7"]
                 [org.apache.logging.log4j/log4j-jcl "2.7"]
                 [org.apache.logging.log4j/log4j-jul "2.7"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.7"]

                 ;; read ini files
                 [com.brainbot/iniconfig "0.2.0"]

                 ;; nlp/ml
                 [com.zensols.tools/actioncli "0.0.27"]
                 [com.zensols.nlp/wordvec "0.0.1"
                  :exclusions [org.apache.httpcomponents/httpmime
                               org.apache.httpcomponents/httpclient
                               org.clojure/tools.macro]]
                 [com.zensols.ml/model "0.0.18"]
                 [com.zensols.nlp/parse "0.1.6"
                  :exclusions [com.zensols.tools/actioncli
                               org.clojure/tools.macro]]
                 [com.zensols.ml/dataset "0.0.12"
                  :exclusions [org.apache.lucene/lucene-analyzers-common
                               org.apache.lucene/lucene-core
                               org.apache.lucene/lucene-queries
                               org.apache.lucene/lucene-queryparser
                               org.apache.lucene/lucene-sandbox]]]
  :pom-plugins [[org.codehaus.mojo/appassembler-maven-plugin "1.6"
                 {:configuration ([:program
                                   ([:mainClass "uic.nlp.todo.core"]
                                    [:id "todotask"])]
                                  [:environmentSetupFileName "setupenv"])}]]
  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :uberjar {:aot [uic.nlp.todo.core]}
             :appassem {:aot :all}
             :snapshot {:git-version {:version-cmd "echo -snapshot"}}
             :dev
             {:exclusions [org.slf4j/slf4j-log4j12
                           log4j/log4j
                           ch.qos.logback/logback-classic]}
             :test {:jvm-opts ["-Dlog4j.configurationFile=test-resources/test-log4j2.xml"
                               "-Xms4g" "-Xmx30g" "-XX:+UseConcMarkSweepGC"]}}
  :main uic.nlp.todo.core)

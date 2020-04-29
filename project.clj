;; this file is used to compile src/clj/demo/colombia.clj
(defproject papachan-covid19 "0.1.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.csv "1.0.0"]
                 [cheshire "5.10.0"]
                 [clj-time "0.15.2"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :resource-paths ["resources"]

  :plugins [[cider/cider-nrepl "0.25.0-SNAPSHOT"]]
  :middleware [cider-nrepl.plugin/middleware]
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]
                                  [cider/piggieback "0.4.0"]]}}
  :repl-options {:init-ns demo.colombia})

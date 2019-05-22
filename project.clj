(defproject hs-slackbot "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :min-lein-version "2.7.1"

  :dependencies [[cljs-http/cljs-http "0.1.46"]
                 [com.stuartsierra/component "0.4.0"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async  "0.4.490"]]

  :plugins [[lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.18"]
            [lein-npm "0.6.2"]]

  :hooks [leiningen.cljsbuild]

  :npm {:dependencies [[body-parser "1.17.2"]
                       [express "4.15.3"]
                       [winston "2.3.1"]
                       [xmlhttprequest "1.8.0"]]
        :devDependencies [[source-map-support "0.4.15"]
                          [ws "3.0.0"]]
        :package {:scripts
                  {:test "echo \"Error: no test specified\" && exit 1"}}}

  :source-paths ["src"]

  :clean-targets ["target"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler
                        {:main hs-slackbot.core
                         :asset-path "target/server_dev"
                         :output-to "target/server_dev/hs_slackbot.js"
                         :output-dir "target/server_dev"
                         :target :nodejs
                         :optimizations :none
                         :source-map true}}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler
                        {:output-to "target/server.js"
                         :output-dir "target/server_prod"
                         :target :nodejs
                         :optimizations :simple}}]}

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                  [figwheel-sidecar "0.5.18"]
                                  [org.clojure/tools.nrepl "0.2.13"]]}})

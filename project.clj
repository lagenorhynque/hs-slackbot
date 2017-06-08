(defproject hs-slackbot "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async  "0.3.442"]]

  :plugins [[lein-cljsbuild "1.1.5" :exclusions [[org.clojure/clojure]]]
            [lein-npm "0.6.2"]
            [lein-figwheel "0.5.10"]]

  :hooks [leiningen.cljsbuild]

  :npm {:dependencies [[express "4.15.3"]
                       [xmlhttprequest "*"]
                       [xmldom "0.1.27"]
                       [source-map-support "*"]]
        :devDependencies []
        :package {:scripts
                  {:test "echo \"Error: no test specified\" && exit 1"}}}

  :source-paths ["src"]

  :clean-targets ["target"]

  :cljsbuild {
    :builds [{:id "dev"
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
               :optimizations :simple}}]})

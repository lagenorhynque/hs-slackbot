(ns hs-slackbot.core
  (:require [cljs.nodejs :as nodejs]))

(enable-console-print!)

(def express (nodejs/require "express"))

(defn handler [req res]
  (if (= "https" (aget (.-headers req) "x-forwarded-proto"))
    (.redirect res (str "http://" (.get req "Host") (.-url req)))
    (do (.set res "Content-Type" "text/html")
        (.send res "<p>Hello from ClojureScript and Express</p>"))))

(defn server [port success]
  (doto (express)
    (.get "/" handler)
    (.listen port success)))

(defn -main [& args]
  (let [port (or (.-PORT (.-env js/process)) 1337)]
    (server port
            #(println (str "Server running at http://127.0.0.1:" port "/")))))

(set! *main-cli-fn* -main)

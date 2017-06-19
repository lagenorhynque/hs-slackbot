(ns hs-slackbot.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [clojure.string :as str]
            [cljs.core.async :refer [<! put! chan]]
            [cljs-http.client :as http]
            [hs-slackbot.ghci :as ghci]))

(nodejs/enable-util-print!)

(defonce express (nodejs/require "express"))
(defonce body-parser (nodejs/require "body-parser"))
(defonce winston (nodejs/require "winston"))

(winston.remove winston.transports.Console)
(winston.add winston.transports.Console #js {:timestamp true})

(set! js/XMLHttpRequest (.-XMLHttpRequest
                         (nodejs/require "xmlhttprequest")))

(def app (express))

(def post-url
  (.-POST_URL (.-env js/process)))

(def command-token
  (.-COMMAND_TOKEN (.-env js/process)))

(def ghci (ghci/new-ghci (atom {})))

(defn eval-expr [expr]
  (let [result-ch (chan)]
    (ghci/eval-with-ghci ghci expr #(put! result-ch %))
    [expr result-ch]))

(defn format-result [expr result user]
  (str/join "\n"
            ["```"
             (str "-- " user)
             (->> (str/split-lines expr)
                  (map #(str "GHCi> " %))
                  (str/join "\n"))
             result
             "```"]))

(defn post-to-slack [[expr result-ch] channel user]
  (let [p (if channel {:channel channel} {})]
    (go
      (let [text (format-result expr (<! result-ch) user)
            res (<! (http/post post-url
                               {:json-params (assoc p :text text)}))]
        (winston.info "Slack Incoming WebHook response"
               "[status]" (:status res) "[body]" (:body res))))))

(defn eval-and-post [s user channel]
  (-> s
      eval-expr
      (post-to-slack channel user)))

(defn handle-hs [req res]
  (let [body (js->clj (.-body req))]
    (if (not= (body "token") command-token)
      (doto res
        (.status 403)
        (.send "Unauthorized"))
      (let [channel (case (body "channel_name")
                      "directmessage" (str "@" (body "user_name"))
                      "privategroup" (body "channel_id")
                      (str "#" (body "channel_name")))]
        (eval-and-post (body "text") (body "user_name") channel)
        (doto res
          (.status 200)
          (.set "Content-Type" "text/plain")
          (.send ""))))))

(defn server [port]
  (doto app
    (.use (.urlencoded body-parser #js {:extended true}))
    (.use (.json body-parser))
    (.get "/" (fn [req res]
                (.send res "A Haskell bot for Slack")))
    (.post "/hs" handle-hs)
    (.listen port)))

(defn -main [& args]
  (let [port (or (.-PORT (.-env js/process)) 1337)]
    (server port)))

(set! *main-cli-fn* -main)

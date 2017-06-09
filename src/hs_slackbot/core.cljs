(ns hs-slackbot.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [clojure.string :as str]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]))

(nodejs/enable-util-print!)

(defonce express (nodejs/require "express"))
(defonce body-parser (nodejs/require "body-parser"))
(defonce winston (nodejs/require "winston"))

(.remove winston winston.transports.Console)
(.add winston winston.transports.Console #js {:timestamp true})

(set! js/XMLHttpRequest (.-XMLHttpRequest
                         (nodejs/require "xmlhttprequest")))

(def app (express))

(def post-url
  (.-POST_URL (.-env js/process)))

(def command-token
  (.-COMMAND_TOKEN (.-env js/process)))

(defn eval-expr [s]
  ;; TODO
  s)

(defn format-result [r user]
  (str/join "\n"
            ["```"
             (str "-- " user)
             r
             "```"]))

(defn post-to-slack [s channel]
  (let [p (if channel {:channel channel} {})]
    (go
      (let [res (<! (http/post post-url
                               {:json-params (assoc p :text s)}))]
        (.info winston "Slack Incoming WebHook response"
               "[status]" (:status res) "[body]" (:body res))))))

(defn eval-and-post [s user channel]
  (-> s
      eval-expr
      (format-result user)
      (post-to-slack channel)))

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

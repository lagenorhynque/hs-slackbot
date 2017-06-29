(ns hs-slackbot.ghci
  (:require [cljs.nodejs :as nodejs]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]))

(defonce spawn (.-spawn (nodejs/require "child_process")))
(defonce winston (nodejs/require "winston"))

(winston.remove winston.transports.Console)
(winston.add winston.transports.Console #js {:timestamp true})

(def exec-cmd "ghci")
(def exec-args #js [])
(def initial-cmds [":set +t"
                   ":set prompt \"GHCi> \""
                   ":set prompt2 \"\""])

(defn- process-chunk [ghci-core chunk]
  (swap! (:buf ghci-core) str chunk))

(defn- restart [ghci-core]
  (winston.info "process killed")
  (when (= @(:status ghci-core) :run)
    (@(:on-finish ghci-core) "process killed")
    (reset! (:on-finish ghci-core) nil))
  (component/start ghci-core))

(defn exec [proc cmd]
  (.stdin.write proc (str cmd "\n")))

(defn clear-buf [ghci-core]
  (reset! (:buf ghci-core) ""))

(defn init-process [ghci-core]
  (winston.info "init process")
  (let [proc (spawn exec-cmd exec-args)]
    (doseq [cmd initial-cmds]
      (exec proc cmd))
    (clear-buf ghci-core)
    (reset! (:ghci-process ghci-core)
            (doto proc
              (.stdout.on "data"
                          #(process-chunk ghci-core %))
              (.stderr.on "data"
                          #(process-chunk ghci-core %))
              (.on "exit" #(restart ghci-core))))
    ghci-core))

(defn kill-process [ghci-core]
  (winston.info "kill process")
  (.kill @(:ghci-process ghci-core) "SIGKILL")
  (reset! (:ghci-process ghci-core) nil)
  ghci-core)

(defrecord GHCiCore [status
                     buf
                     on-finish
                     ghci-process]
  component/Lifecycle
  (start [component]
    (init-process component))
  (stop [component]
    (kill-process component)))

(defn new-ghci-core [on-ready]
  (let [ghci-core (map->GHCiCore {:status (atom :load)
                                  :buf (atom "")
                                  :on-finish (atom nil)
                                  :ghci-process (atom nil)})]
    (js/setInterval (fn []
                      (let [buf @(:buf ghci-core)]
                        (when (seq buf)
                          (when (= @(:status ghci-core) :load)
                            (winston.info "process is ready"))
                          (when (= @(:status ghci-core) :run)
                            (winston.info "eval finish:" buf)
                            (when @(:on-finish ghci-core)
                              (@(:on-finish ghci-core) buf))
                            (reset! (:on-finish ghci-core) nil))
                          (clear-buf ghci-core)
                          (reset! (:status ghci-core) :ready)
                          (on-ready))))
                    100)
    ghci-core))

(defn eval [ghci-core expr on-finish]
  (reset! (:on-finish ghci-core) on-finish)
  (when (not= @(:status ghci-core) :ready)
    (throw "another one is running"))
  (winston.info "eval:" expr)
  (clear-buf ghci-core)
  (reset! (:status ghci-core) :run)
  (exec @(:ghci-process ghci-core) expr))

(defn new-ghci [ghci]
  (swap! ghci assoc :wait-queue #queue [])
  (swap! ghci assoc :ghci-core
         (component/start
          (new-ghci-core (fn []
                           (let [queue (:wait-queue @ghci)
                                 [[expr on-finish] :as fst] (peek queue)]
                             (when fst
                               (swap! ghci update :wait-queue pop)
                               (eval (:ghci-core @ghci)
                                     expr
                                     on-finish)))))))
  ghci)

(defn eval-with-ghci [ghci expr out-fn]
  (winston.info "request:" expr)
  (if (= @(:status (:ghci-core @ghci)) :ready)
    (eval (:ghci-core @ghci) expr out-fn)
    (swap! ghci update :wait-queue conj [expr out-fn])))

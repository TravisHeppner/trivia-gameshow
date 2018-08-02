(ns gameshow.server.routes
  (:require
    [gameshow.server.model :as model]
    [ring.middleware.defaults :as defaults]
    [ring.middleware.reload :as reload]
    [ring.middleware.cors :as cors]
    [ring.util.response :as response]
    [environ.core :as environ]
    [taoensso.sente :as sente]
    [taoensso.sente.server-adapters.http-kit :as http-kit]
    [taoensso.timbre :as timbre]
    [compojure.core :refer [defroutes GET POST]]
    [compojure.route :as route]))

(declare channel-socket)

(defn start-websocket []
  (defonce channel-socket
    (sente/make-channel-socket!
      http-kit/sente-web-server-adapter
      {:user-id-fn #'model/next-uid})))

(defroutes routes
  (GET "/" req (response/content-type
                 (response/resource-response "public/index.html")
                 "text/html"))
  (GET "/status" req (str "Running: " (pr-str @(:connected-uids channel-socket))))
  (GET "/chsk" req ((:ajax-get-or-ws-handshake-fn channel-socket) req))
  (POST "/chsk" req ((:ajax-post-fn channel-socket) req))
  (route/resources "/")
  (route/not-found "Nnt found"))

(def handler
  (-> #'routes
    (cond-> (environ/env :dev?) (reload/wrap-reload))
    (defaults/wrap-defaults (assoc-in defaults/site-defaults [:security :anti-forgery] false))
    (cors/wrap-cors :access-control-allow-origin [#".*"]
                    :access-control-allow-methods [:get :put :post :delete]
                    :access-control-allow-credentials ["true"])))

(defmulti event :id)

(defmethod event :default [{:as ev-msg :keys [event]}]
  (println "Unhandled event: " event))

(defmethod event :gameshow/username [{:as ev-msg :keys [event uid ?data]}]
  (do
    (timbre/info "Username event: " uid event ?data)
    (model/username uid ?data)))

(defmethod event :gameshow/team [{:as ev-msg :keys [event uid ?data]}]
  (do
    (timbre/info "team event: " uid event ?data)
    (model/team uid ?data)))

(defmethod event :gameshow/respawn [{:as ev-msg :keys [event uid ?data]}]
  (do
    (timbre/info "Respawn event: " uid event)
    (model/enter-game :uid uid :in-game? true)))

(defmethod event :gameshow/reset-questions [{:as ev-msg :keys [event uid ?data]}]
  (do
    (timbre/info "reset questions event: " uid event)
    (model/reset-questions)))

(defmethod event :gameshow/reset-players [{:as ev-msg :keys [event uid ?data]}]
  (do
    (timbre/info "reset players event: " uid event)
    (model/reset-players)))

(defmethod event :gameshow/select-question [{:keys [?data] :as ev-msg}]
  (do
    (timbre/info "select-question event: " ?data)
    (model/select-question ?data)))
(defmethod event :gameshow/score-player [{:keys [uid :as ev-msg]}]
  (do
    (timbre/info "scoring player" uid)
    (model/score-player uid)))

(defmethod event :chsk/uidport-open [{:keys [uid client-id]}]
  (do
    (timbre/info "connection open event:" uid client-id)
    (model/enter-game :uid uid :in-game? false)))

(defmethod event :chsk/uidport-close [{:keys [uid]}]
  (do
    (timbre/info "connection close event: " uid)
    (model/remove-player uid)))

(defmethod event :chsk/ws-ping [ev-msg]
  (do
    (timbre/info "ping event")))

(defn start-router []
  (defonce router
    (sente/start-chsk-router! (:ch-recv channel-socket) event)))

(defn broadcast []
  (doseq [uid (:any @(:connected-uids channel-socket))]
    ((:send-fn channel-socket) uid [:gameshow/world @model/world])))

(defn ticker []
  (while true
    (Thread/sleep 150)
    (try
      #_(model/tick)
      (broadcast)
      (catch Exception ex
        (println ex)))))

(defn start-ticker []
  (defonce ticker-thread
    (doto (Thread. ticker)
      (.start))))

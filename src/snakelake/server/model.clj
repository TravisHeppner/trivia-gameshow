(ns snakelake.server.model
  (:require
    [clojure.core.memoize :as memo]
    [clojure.set :as set]
    [taoensso.timbre :as timbre]))

(defonce world
  (ref {:players {}}))

(def colors
  #{"#181818" "#282828" "#383838" "#585858"
    "#B8B8B8" "#D8D8D8" "#E8E8E8" "#F8F8F8"
    "#AB4642" "#DC9656" "#F7CA88" "#A1B56C"
    "#86C1B9" "#7CAFC2" "#BA8BAF" "#A16946"})

(def colors2
  (set (for [c1 colors
             c2 colors
             :when (not= c1 c2)]
         (str c1 c2))))

(defn next-color []
  (let [available (set/difference colors2 (set (keys (:players @world))))]
    (some-> (seq available) (rand-nth))))

(defn client-uid* [client-id]
  (next-color))

(def client-uid (memo/ttl client-uid* :ttl/threshold (* 60 60 1000)))

(defn next-uid [{:keys [params]}]
  (client-uid (:client-id params)))

(defn new-player [world uid username team in-game?]
  (let [points 3]
       (-> world
           (assoc-in [:players uid] {:points points
                                     :username username
                                     :team team
                                     :active in-game?}))))

(defn enter-game
  [& {:keys [uid in-game?]}]
  (do
    (println uid "entering game")
    (dosync (alter world new-player uid
                   (if-let [{:keys [points username team]} (get-in @world [:players uid])]
                     username
                     "Unknown")
                   (if-let [{:keys [points username team]} (get-in @world [:players uid])]
                     team
                     "Unknown")
                   in-game?))))

(defn username [uid username]
  (dosync (alter world assoc-in [:players uid :username] username)))

(defn team [uid team]
  (dosync
    (timbre/info "Changeing team " team)
    (alter world assoc-in [:players uid :team] team)))

(defn remove-player* [world uid]
  (update world :players #(dissoc % uid)))
(defn remove-player [uid]
  (do
    (println "removing player "uid)
    (dosync (alter world remove-player* uid))))
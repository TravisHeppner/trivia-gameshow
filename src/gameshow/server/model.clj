(ns gameshow.server.model
  (:require
    [clojure.core.memoize :as memo]
    [clojure.set :as set]
    [taoensso.timbre :as timbre]
    [clojure.java.io :as io]))

(def quiz-resource
  (io/resource "quiz.edn"))

(defonce world
         (ref {:players   {}
               :questions (clojure.edn/read-string (slurp quiz-resource))}))

(defn reset-questions [] (dosync (alter world assoc-in [:questions](clojure.edn/read-string (slurp quiz-resource)))))
(defn reset-players [] (dosync (alter world assoc :players {})))

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

(defn new-player [world uid username team admin in-game?]
  (let [points 0]
    (assoc-in world [:players uid] {:points   points
                                    :username username
                                    :team     team
                                    :admin    admin
                                    :active   in-game?})))

(defn enter-game
  [& {:keys [uid in-game?]}]
  (do
    (println uid "entering game")
    (dosync (alter world new-player uid
                   (if-let [{:keys [points username team admin]} (get-in @world [:players uid])]
                     username
                     "Unknown")
                   (if-let [{:keys [points username team admin]} (get-in @world [:players uid])]
                     team
                     "Unknown")
                   (if-let [{:keys [points username team admin]} (get-in @world [:players uid])]
                     admin
                     false)
                   in-game?))))

(defn username [uid username]
  (dosync
    (alter world assoc-in [:players uid :username] username)
    (when (#{"admin-master"} username)
      (alter world assoc-in [:players uid :admin] true)
      (timbre/info "admin logged in: " @world))))

(defn team [uid team]
  (dosync
    (timbre/info "Changeing team " team)
    (alter world assoc-in [:players uid :team] team)))

(defn remove-player* [world uid]
  (update world :players #(dissoc % uid)))
(defn remove-player [uid]
  (do
    (println "removing player " uid)
    (dosync (alter world remove-player* uid))))

(defn select-question [[category questions]]
  (do
    (timbre/info "selecting a question.")
    (dosync
      (alter world update-in [:questions category] rest)
      (alter world assoc :current-question (first questions)))))

(defn score-player [uid]
  (dosync
    (let [points (or (:current-question world) 1)]
      (alter world update-in [:players uid :points] #(+ points %)))))
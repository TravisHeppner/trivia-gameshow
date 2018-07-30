(ns snakelake.model
  (:require
    [reagent.core :as reagent]))
(def adjectives
  ["Magical" "Kawaii" "Megane" "Mecha" "Otaku"])
(def noun
  [ "shounen" "shoujo" "sensei" "sempai" "kohai" "nee" "oni" "imouto" "ototo"])
(def honorifics
  ["chan" "san" "dono" "han" "bo" "sama" "kun" "tan"])

(defn rand-name []
  (str (rand-nth adjectives) "-" (rand-nth noun) "-" (rand-nth honorifics)))
(defn rand-team []
  (str "team " (rand-nth (range 1 2))))

(defonce app-state
  (reagent/atom
    {:username (rand-name) :team (rand-team)}))

(defn world! [world]
  (swap! app-state assoc :world world))

(defn uid! [uid]
  (swap! app-state assoc :uid uid))

(defn username! [username]
  (swap! app-state assoc :username username))

(defn team! [team]
  (println team)
  (swap! app-state assoc :team team))

(defn in-game? []
  (when-let [me (:uid @app-state)]
    (boolean (some-> @app-state :world :players (get me) :active))))


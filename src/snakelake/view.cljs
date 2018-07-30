(ns snakelake.view
  (:require
    [clojure.string :as string]
    [goog.events :as events]
    [goog.events.KeyCodes :as KeyCodes]
    [snakelake.model :as model]
    [snakelake.communication :as communication]
    [goog.crypt :as crypt]
    [goog.dom.forms :as forms]
    [goog.string :as gstring]
    [goog.string.format])
  (:import
    [goog.crypt Md5]))

(defn dir [e [dx dy]]
  (.preventDefault e)
  (communication/dir dx dy))

(defn click [e]
  (let [elem (.-target e)
         r (.getBoundingClientRect elem)
         left (.-left r)
         top (.-top r)
         width (.-width r)
         height (.-height r)
         ex (.-clientX e)
         ey (.-clientY e)
         x (- ex left (/ width 2))
         y (- ey top (/ height 2))]
    (dir e
         (if (> (js/Math.abs y) (js/Math.abs x))
           (if (pos? y)
             [0 1]
             [0 -1])
           (if (pos? x)
             [1 0]
             [-1 0])))))

(defn login-form []
      [:div
        {:style {:position "relative"
            :top "100px"
            :height 0
            :z-index 1}}
   (if (model/in-game?)
     [:h1]
     [:h1
      [:form
       {:on-submit
        (fn [e]
          (.preventDefault e)
          (model/username! (forms/getValueByName (.-target e) "username"))
          (model/team! (forms/getValueByName (.-target e) "team"))
          (communication/send-username)
          (communication/send-team)
          (communication/respawn))}
       [:label "Name:"]
       [:input
        {:type "text"
         :name "username"
         :auto-focus "autofocus"
         :default-value (:username @model/app-state)
         :style {:font-size "0.9em"}}]
       [:div
        "Team:"
        [:select {:name "team"}
         [:option "team 1"]
         [:option "team 2"]]]
       " "
       [:button {:type "submit"}
        "Join game!"]
       [:br]
       [:br]]])])

(defn get-active [[uid {:keys[points username team active]}]]
  [active (string/lower-case username)])

(defn md5-hash [s]
  (let [md5 (Md5.)]
    (.update md5 (string/trim s))
    (crypt/byteArrayToHex (.digest md5))))

(defn team->color [team]
  (as-> team x
        (hash x)
        (if (neg? x) (- 0 x) x)
        (str x "00000")
        (take 6 x)
        (apply str "#" x)))

(defn scores [{{players :players :as world} :world my-uid :uid username :username}]
  [:div
   {:style {:position "relative"
            :float "right"
            :height "0"}}
   [:table
    [:thead
     [:tr
      [:th ]
      [:th (str "Players" )]
      [:th ]]]
    [:tbody
     (doall
       (for [[team players] (group-by (comp :team second) players)
             [uid {:keys [points username team active]}] players]
         ^{:key uid}
         (if active
           [:tr
            [:td.number points]
            [:td team]
            [:td {:style {:color            "#ffffff"
                          :background-color (team->color team)}}
             (first (string/split username #"@"))]])))]]])

;(defn sound-track []
;  [:div
;   [:audio
;    {:controls "true"
;     :auto-play "true"
;     :loop "true"}
;    [:source {:src "https://ia801504.us.archive.org/16/items/AhrixNova/Ahrix%20-%20Nova.mp3"}]
;    "Your browser does not support the audio element."]
;   [:div "Ahrix - Nova [NCS Release]"]])

(defn testing [{:keys [uid username team world] :as app}]
  [:div


   [:p (str "UID: " uid)]
   [:p (str "USERNAME:" username)]
   [:p (str "TEAM: " team)]
   (map (fn [p] [:p (str "PLAYER: " p)]) (:players world))


   [:div (str app)]])

(defn main []
  [:div.content
   [:h1 "Anime trivia gameshow!!" ]
   [:center
    ; [sound-track]
    [login-form]
    [scores @model/app-state]
    [testing @model/app-state]
    ;[board @model/app-state]
    [:p {:style {:inline true}} "Multiplayer - invite your friends."]
    [:p " Steer with the arrow keys, WASD, or click/touch the side of the board."]
    ]])

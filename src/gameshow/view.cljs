(ns gameshow.view
  (:require
    [clojure.string :as string]
    [goog.events :as events]
    [goog.events.KeyCodes :as KeyCodes]
    [gameshow.model :as model]
    [gameshow.communication :as communication]
    [goog.crypt :as crypt]
    [goog.dom.forms :as forms]
    [goog.string :as gstring]
    [goog.string.format])
  (:import
    [goog.crypt Md5]))

(defn login-form []
      [:div
        {:style {:position "relative"
            :top "100px"
            :height 0
            :z-index 1}}
   (if (model/in-game?)
     [:h1]
     [:div {:class "jumbotron"}
      [:h1 {:class "display-4"} "Select name and team!"]
      [:form
       {:on-submit
        (fn [e]
          (.preventDefault e)
          (model/username! (forms/getValueByName (.-target e) "username"))
          (model/team! (forms/getValueByName (.-target e) "team"))
          (communication/send-username)
          (communication/send-team)
          (communication/respawn))}

       [:div {:class "form-group"}
        [:label "Name"]
        [:input
         {:type          "text"
          :name          "username"
          :auto-focus    "autofocus"
          :class "form-control"
          :default-value (:username @model/app-state)
          :style         {:font-size "0.9em"}}]]
       [:div {:class "col-xs-2"}
        "Team:"
        [:select {:name "team" :class "form-control form-control-sm" :text-align "center" }
         [:option "team 1"]
         [:option "team 2"]]]
       " "
       [:button {:type "submit"}
        "Join game!"]

       [:br]
       [:br]]])])

(defn get-active [[uid {:keys[points username team active admin]}]]
  [active (string/lower-case username)])

(defn admin? [uid world] (-> world :players (get uid) :admin boolean))

(defn team->color [team]
  (as-> team x
        (hash x)
        (if (neg? x) (- 0 x) x)
        (str x "00000")
        (take 6 x)
        (apply str "#" x)))

(defn scores [{{players :players :as world} :world my-uid :uid username :username}]
  [:div
   {:class "sidenav"}
   [:ul {:padding "0px 0px"}
    [:p " "] [:p " "]
    [:p (str "Players")]
    (doall
      (for [[team players] (group-by (comp :team second) players)
            [uid {:keys [points username team active admin]}] players]
        ^{:key uid}
        (if (or (and active (not admin)))
          [:li {:class "list-group-item col-xs-2 text-right"
                :style {:height           "30px"
                        :padding          "3px 5px"
                        :color            "#ffffff"
                        :background-color (team->color team)}}
           (if (admin? my-uid world)
             [:button {:on-click (fn [_] (communication/score-player uid))} "score"])
           [:font {:size 1} (first (string/split username #"@")) " "]
           [:span {:class "badge badge-primary badge-pill"} points]])))]])

;(defn sound-track []
;  [:div
;   [:audio
;    {:controls "true"
;     :auto-play "true"
;     :loop "true"}
;    [:source {:src "https://ia801504.us.archive.org/16/items/AhrixNova/Ahrix%20-%20Nova.mp3"}]
;    "Your browser does not support the audio element."]
;   [:div "Ahrix - Nova [NCS Release]"]])

(defn board [{:keys [uid username team world] :as app}]
  [:div {:class "main"}
   [:p " "][:p " "]
   (for [[category-prompt questions :as category] (:questions world)]
     [:button (cond-> {:on-click (fn [_] (communication/select-question category))
                       :type     :button
                       :class    "btn btn-primary"
                       :style    {:size 20}}
                      (= 0 (count questions)) (assoc :disabled true :class "btn btn-secondary"))
      (name category-prompt)
      ])
   [:div (str world)]])

(defn testing [{:keys [uid username team world] :as app}]
  [:div
   [:p (str "UID: " uid)]
   [:p (str "USERNAME:" username)]
   [:p (str "TEAM: " team)]
   ;(map (fn [p] [:p (str "PLAYER: " p)]) (:players world))
   [:p (str (:questions world))]
   [:p (str (:current-question world))]
   [:div (str app)]])

(defn admin-tools [{:keys [uid username team world] :as app}]
  (if (admin? uid world)
    [:div
     [:button {:on-click (fn [_] (communication/reset-questions))
               :type     :button
               :class    "btn btn-primary"
               :style    {:size 20}}
      "Reset questions"]
     [:button {:on-click (fn [_] (communication/reset-players))
               :type     :button
               :class    "btn btn-primary"
               :style    {:size 20}}
      "Reset players"]
     [:button {:type "button" :class "btn btn-primary" :data-toggle "modal" :data-target "#ScoreModal"}
      "Score question"]]
    [:h1 "Anime trivia gameshow!!"]))

(defn answer-scoring [{:keys [uid username team world] :as app}]
  [:div "thing is here!!!"
   [:div {:class "modal fade" :id "ScoreModal" :tabindex "-1" :role "dialog" :aria-labelledby "ScoreModalLabel" :aria-hidden false}
    [:div {:class "modal-dialog" :role "document"}
     [:div {:class "modal-content"}
      [:div {:class "modal-header"}
       [:h5 {:class "modal-title" :id "ScoreModalLabel"} "Scoring page"]
       [:button {:type "button" :class "close" :data-dismiss "modal" :aria-label "Close"}
        [:span {:aria-hidden false} "&times;"]]]
      [:div {:class "modal-body"}
       [:p "see scores here!!"]]
      [:div {:class "modal-footer"}
       [:button {:type "button" :class "btn btn-secondary" :data-dismiss "modal"} "Close"]
       [:button {:type "button" :class "btn btn-primary"} "Submit Scoring"]]]]]])

(defn main []
  [:div.content
      ; [sound-track]
   [scores @model/app-state]
   ;[testing @model/app-state]
   [:div {:class "main"}
    [admin-tools @model/app-state]
    [login-form]
    [board @model/app-state]
    [answer-scoring @model/app-state]
    [:p {:style {:inline true}} "Multiplayer - invite your friends."]
    [:p "Join by going to KNCAGameShow.herokuapp.com/"]]])

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
       [:button {:type "submit" :class "btn primary-btn"}
        "Join game!"]]])])

(defn get-active [[uid {:keys[points username team active admin]}]]
  [active (string/lower-case username)])

(defn admin? [uid world] (-> world :players (get uid) :admin boolean))

(defn player-answered [uid world] (-> world :players (get uid) :answer))
(defn player-answered? [uid world] (boolean (player-answered uid world)))

(defn team->color [team]
  (as-> team x
        (hash x)
        (if (neg? x) (- 0 x) x)
        (str x "00000")
        (take 6 x)
        (apply str "#" x)))

(defn scores [{{players :players :as world} :world my-uid :uid username :username}]
  [:div {:class "sidenav"}
   [:ul {:padding "0px 0px"}
    [:p " "] [:p " "]
    [:p (str "Players")]
    (doall
      (for [[team players] (group-by (comp :team second) players)
            [uid {:keys [points username team active admin] :as player}] players]
        ^{:key uid}
        (if (or (and active (not admin)))
          [:li {:class "list-group-item  text-right"
                :style {:height           "30px"
                        :width            "150px"
                        :padding          "0px 1px"
                        :color            "#ffffff"
                        :background-color (team->color team)}}
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

(defn question-card [{{:keys [image question answers points]} :current-question :as world} my-uid]
  [:div {:class "row"}
   [:div {:class "col-9 offset-2"}
    [:div {:class "card" :style {:width "70%"}}
     [:div {:class "card-header"}
      [:div]                                                ;todo add category here
      [:div "For " points " points!"]]
     (if image
       [:img {:class "card-img-top" :src image}]
       [:div])
     [:div {:class "card-body"}
      [:p {:class "card-text"} [:pre question]]
      (if (or (:scoring? world) (admin? my-uid world))
        (map (fn [a] [:pre a]) answers)
        [:div
         (if (:buzzer world)
           [:p (str "player '") (-> world :players (get (:buzzer world)) :username) "' Buzzed in."]
           [:a
            {:on-click (fn [e]
                         (.preventDefault e)
                         (communication/buzzer)) :class "btn btn-primary"}
            [:img
             {:height "40px"
              :width  "40px"
              :src    "https://is3-ssl.mzstatic.com/image/thumb/Purple125/v4/1a/90/51/1a9051e7-a13d-1f59-ef64-8fd7edba370a/AppIcon-1x_U007emarketing-0-0-GLES2_U002c0-512MB-sRGB-0-0-0-85-220-0-0-0-6.png/246x0w.jpg"}]])
         (if (player-answered? my-uid world)
           [:p "Answer submited"]

           [:form {:on-submit
                   (fn [e]
                     (.preventDefault e)
                     (communication/send-answer (forms/getValueByName (.-target e) "player-answer")))}
            [:div {:class "form-group"}
             [:label "Answer"]
             [:input
              {:type        "text"
               :name        "player-answer"
               :auto-focus  "autofocus"
               :class       "form-control"
               :placeholder "Type your answer here"
               :style       {:font-size "0.9em"}}]
             [:button {:type "submit" :class "btn primary-btn"}
              "Sumbit answer!"]]])])]]]])

(defn board [{:keys [uid username team world] :as app}]
  (if (:current-question world)
    [question-card world uid]
    [:div {:class "main"}
     [:p " "] [:p " "]
     [:div {:class "col 10" }
      [:div {:class "row"
             :style {:pading "10px 10px 10px 10px"}}
       (for [[category-prompt questions :as category] (:questions world)]
         [:div {:class "col-lg"}
          [:p]
          [:button (cond-> {:on-click (fn [_] (communication/select-question category))
                            :type     :button
                            :class    "btn btn-primary"
                            :style    {:size 20 :width 200}}
                           (not (or (admin? uid world))) (assoc :disabled true)
                           (= 0 (count questions)) (assoc :disabled true :class "btn btn-secondary"))
           (name category-prompt)]
          [:p]])]]]))

(defn testing [{:keys [uid username team world] :as app}]
  [:div
   [:p (str "UID: " uid)]
   [:p (str "USERNAME:" username)]
   [:p (str "TEAM: " team)]
   [:p (str "admin?" (admin? uid world))]
   [:p (str "current-question" (:current-question world))]
   (map (fn [p] [:p (str "PLAYER: " p)]) (:players world))
   [:p (str "questions" (:questions world))]
   [:p (str "cuurent-user" (-> world :players (get uid) :answer))]
   [:div (str app)]])

(defn admin-tools [{:keys [uid username team world] :as app}]
  [:div
   (admin? uid world)
   (if (admin? uid world)
     [:div
      [:button {:on-click (fn [_] (communication/reset-questions))
                :type     :button
                :class    "btn btn-danger"
                :style    {:size 20}}
       "Reset questions"]
      [:button {:on-click (fn [_] (communication/reset-players))
                :type     :button
                :class    "btn btn-danger"
                :style    {:size 20}}
       "Reset players"]
      [:button {:on-click (fn [_] (communication/start-scoring))
                :type     :button
                :class    (if (:current-question world) "btn btn-primary" "btn btn-warning")
                :style    {:size 20}}
       "Score question"]
      [:button {:on-click (fn [_] (communication/buzzer-done))
                :type     :button
                :class    (if (:buzzer world) "btn btn-primary" "btn btn-warning")
                :style    {:size 20}}
       "Finish buzzer"]]
     [:h1 "Anime trivia gameshow!!"])])

(defn answer-scoring [{:keys [uid username team world] :as app}]
  [:div
   (if (and (admin? uid world) (:scoring? world))
     [:div {:class "jumbotron"}
      [:h1 {:class "display-4"} "Score-screen!"]
      (if (not (seq (filter (comp :answer second) (:players world))))
        [:div
         [:button {:on-click (fn [_] (communication/end-scoring))
                   :type     :button
                   :class    (if (:current-question world) "btn btn-primary" "btn btn-warning")
                   :style    {:size 20}}
          "End Scoring"]]
        [:ul
         (for [[uid {:keys [points username team active admin] :as player}] (filter (comp :answer second) (:players world))]
           [:li
            [:form
             {:on-submit
              (fn [e]
                (.preventDefault e)
                (communication/score-player uid (forms/getValueByName (.-target e) (str uid "correct1")))
                (communication/score-player uid (forms/getValueByName (.-target e) (str uid "correct2"))))}
             [:div {:class "form-checkbox form-check-inline"}
              [:input {:type "checkbox" :class "form-check-input" :id (str uid "correct1") :name (str uid "correct1")}]
              [:input
               (if (some #(= (:answer player) %) (-> world :current-question :answers))
                 {:type "checkbox" :class "form-check-input" :id (str uid "correct2") :name (str uid "correct2") :checked true}
                 {:type "checkbox" :class "form-check-input" :id (str uid "correct2") :name (str uid "correct2")}) username]]
             [:div
              [:label (:answer player)]]
             [:div
              [:button {:type "submit" :class "btn primary-btn"}
               "Submit scores!"]]]])])]
     [:div])])

(defn final-scores [{:keys [uid user-name team world] :as app}]
  (if (seq (:players world))
    [:div
     [:ul
      (for [[team players] (group-by (comp :team second) (:players world))]
          [:li
           [:div {:class "col-10"}
            (let [total-score (reduce + (keep (comp :points second) players))]
              (if (< 0 total-score)
                [:h1 (str "Team '" team "' has " total-score " points!")]
                [:div]))]])]]
    [:div]))

(defn main []
  [:div.content
   [scores @model/app-state]
   [:div {:class "main"}
    [admin-tools @model/app-state]
    [login-form]
    [board @model/app-state]
    [answer-scoring @model/app-state]
    [final-scores @model/app-state]
    ;[testing @model/app-state]
    [:div {:class "col-md-9 offset-md-1"}
     [:p {:style {:inline true}} "Multiplayer - invite your friends."]
     [:p "Join by going to KNCAGameShow.herokuapp.com"]
     [:p "Submit questions to anime-trivia-gameshow@trivia_anime  on twitter."]]]])

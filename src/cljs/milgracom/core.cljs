(ns milgracom.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reanimated.core :as anim]
            [clojure.string :as str]
            [clojure.core.async :as async]
            [cljs.pprint :as print :refer [cl-format]]
            [cljs-http.client :as http]))


(defonce menu-labels ["blog" "games" "apps" "downloads" "donate"])
(defonce menu-colors [0xc3ebff
                      0xa0cee5
                      0x7fb6d2
                      0x6da9c7
                      0x5b96b4])

                      ;; 0x000035
                      ;; 0x000042
                      ;; 0x000053
                      ;; 0x000068
                      ;; 0x1c1c84])
                     
(defonce blog-posts (atom {:posts "Here will be posts"}))
(defonce menu-state (atom {:newlabels ["blog" "games" "apps" "downloads" "donate"]
                           :oldlabels ["blog" "games" "apps" "downloads" "donate"]}))
(defonce tabwidth (/ 300 4))


(defn submenu [active]
  (fn []
    (if active
      [:div {:style {:position "absolute"
                     :left 0
                     :margin "10px"
                     :white-space "nowrap"
                     :background "none"
                     :color "#BBBBBB"
                     :font-size "1.9em"
                     :font-weight 400
                     :font-family "-apple-system,BlinkMacSystemFont,Avenir,Avenir Next,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Fira Sans,Droid Sans,Helvetica Neue,sans-serif"
                     }}
       "Newer February January 2019 December November October September August July June May April March February January 2018 Older"]
      [:div]
      )
  ))


(defn get-content [label]
  (if (= label "blog")
    (async/go
      (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/months"))]
        (swap! blog-posts assoc :posts body)))))


(defn get-metrics
  "calculates size, position and color for menucard"
  [label]
  (let [index (.indexOf (@menu-state :newlabels) label)
        ;; get old and new positions
        oldindex (.indexOf (@menu-state :oldlabels) label)
        newindex (.indexOf (@menu-state :newlabels) label)]
    {:oldcolor (nth menu-colors oldindex)
     :newcolor (nth menu-colors newindex)
     ;; get old and new positions
     :oldpos (if (= label (last (@menu-state :oldlabels))) 300 (* oldindex tabwidth))
     :newpos (if (= label (last (@menu-state :newlabels))) 300 (* newindex tabwidth))
     ;; get old and new size
     :oldsize (if (= label (last (@menu-state :oldlabels))) 600 tabwidth)
     :newsize (if (= label (last (@menu-state :newlabels))) 600 tabwidth)}))


(defn menucard
  "returns a menucard component with the proper contents for given label"
  [label]
  (let [metrics (get-metrics label)
        ;; component-local reagent atoms for animation
        pos (reagent/atom (metrics :oldpos))
        size (reagent/atom (metrics :oldsize))
        color (reagent/atom (metrics :oldcolor))
        ;; spring animators 
        pos-spring (anim/spring pos {:mass 10.0 :stiffness 0.5 :damping 2.0})
        size-spring (anim/spring size {:mass 5.0 :stiffness 0.5 :damping 2.0})
        color-spring (anim/spring color {:mass 5.0 :stiffness 0.5 :damping 2.0})]
    (fn a-menucard []
      [:div
       ;; animation structure
       [anim/timeline
        0
        #(reset! color (metrics :newcolor))
        100
        #(reset! pos (metrics :newpos))
        150
        #(reset! size (metrics :newsize))
        200
        #(get-content label)]
       ;; menucard start
       [:div
        {:class "card"
         :style {:background (cl-format nil "#~6,'0x" @color-spring)
                 :transform (str "translate(" @pos-spring "px)")
                 :width @size-spring}  
         :on-click (fn [e]
                     (let [new-state (concat (filter #(not= % label) (@menu-state :newlabels)) [label])]
                       (swap! menu-state assoc :oldlabels (@menu-state :newlabels))
                       (swap! menu-state assoc :newlabels new-state)))}
        ;; menucard button
        [:input
         {:type "button"
          :class "cardbutton"
          :key (str label "button")
          :value label
          }]
        ;; menucard submenu
        [:div {:style {:height "50px"}}]
        ;;[(submenu (= label (@btn-state :active)))]
        ;; menucard content
        [:div {:style {;;:position "relative"
                       ;;:width "600px"
                       :top "50px"
                       :margin "10px"
                       :background "none"}}
         (if (= label "blog")
           (@blog-posts :posts))]]
       ])))


(defn page []
  "returns a page component"
  (fn []
     [:div {:style {:position "absolute"
                    :width "900px"
                    :left 0
                    :right 0
                    :top 0
                    :bottom 0
                    :border "0px"
                    :margin-left "auto"
                    :margin-right "auto"
                    :background "none"}}
      [:div       
       (map (fn [label] [(menucard label)]) (@menu-state :newlabels))]
      [:div {:class "logo"} "milgra.com"]]))


(defn start []
  (reagent/render-component
   [page]
   (. js/document (getElementById "app")))

  ;; animate to blog
  (let [new-state (concat (filter #(not= % "blog") (@menu-state :newlabels)) ["blog"])]
    (swap! menu-state assoc :oldlabels (@menu-state :newlabels))
    (swap! menu-state assoc :newlabels new-state)))


(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))


(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))

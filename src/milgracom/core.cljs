(ns milgracom.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reanimated.core :as anim]
            [clojure.string :as str]
            [clojure.core.async :as async]
            [cljs-http.client :as http]))

(defonce app-state (atom {:text "Hello world!"}))
(defonce btn-state (atom {:fixlabels ["games" "apps" "downloads" "donate" "blog"]
                          :colors ["#FFAAAA" "#FFAAFF" "#6699FF" "#3366FF" "#0033FF"]
                          :labels ["games" "apps" "downloads" "donate" "blog"]
                          :oldlabels ["games" "apps" "downloads" "donate" "blog"]
                          :active "blog"
                          :oldactive "blog"
                          :posts "Here will be posts"}))

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

(defn get-posts []
  (async/go
    (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000"))]
      (swap! btn-state assoc :posts body))))


(defn menucard [ label labelch oldlabelch ]
  (let [
        tabwidth (/ 300 4)
        colindex (.indexOf (@btn-state :fixlabels) label)
        color (nth (@btn-state :colors) colindex)
        btnindex (.indexOf (@btn-state :labels) label)
        obtnindex (.indexOf (@btn-state :oldlabels) label)
        oldindex (str/index-of oldlabelch label)
        index (str/index-of labelch label)
        oldpos (if (= label (@btn-state :oldactive))
                 (* 4 tabwidth)
                 (* obtnindex tabwidth))
        pos (reagent/atom oldpos)
        pos-spring (anim/spring pos {:mass 10.0 :stiffness 0.5 :damping 2.0})
        newpos (if (= label (@btn-state :active))
                 (* 4 tabwidth)
                 (* btnindex tabwidth))
        size (reagent/atom (if (= label (@btn-state :oldactive))
                             600
                             tabwidth))
        newsize (if (= label (@btn-state :active))
                             600
                             tabwidth)
        size-spring (anim/spring size {:mass 5.0 :stiffness 0.5 :damping 2.0})
        ]
    (fn a-button []
      [:div
       [anim/timeout #(reset! pos newpos) 100]
       [anim/timeout #(reset! size newsize) 100]
       [:div
        {
         :class "card"
         :style {:background color
                 :transform (str "translate(" @pos-spring "px)")
                 :width @size-spring}
         
         :on-click (fn [e]
                     (let [newmenu (concat (filter #(not= % label) (@btn-state :labels)) [label])]
                       (get-posts)
                       (swap! btn-state assoc :oldlabels (@btn-state :labels))
                       (swap! btn-state assoc :labels newmenu)
                       (swap! btn-state assoc :oldactive (@btn-state :active))
                       (swap! btn-state assoc :active label)))
         }
        [:input
         {:type "button"
          :class "cardbutton"
          :key (str label "button")
          :value label
          }]
        
        [:div {:style {:height "50px"}}]
        ;;[(submenu (= label (@btn-state :active)))]

        [:div {:style {;;:position "relative"
                       ;;:width "600px"
                       :top "50px"
                       :margin "10px"
                       :background "none"}}
         (@btn-state :posts)]

        ]
       ])))

(defn menu []
  (println "rerender")
  (fn []
    (let [labelch (apply str (@btn-state :labels))
          oldlabelch (apply str (@btn-state :oldlabels))]
      [:div       
       (map (fn [label] [(menucard label labelch oldlabelch)]) (@btn-state :labels))])))

(defn page []
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
      [menu]
      [:div {:class "logo"} "milgra.com"]]))


(defn start []
  (reagent/render-component [page]
                            (. js/document (getElementById "app"))))


(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))

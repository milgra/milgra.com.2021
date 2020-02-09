(ns milgracom.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reanimated.core :as anim]
            [clojure.string :as str]))

(defonce app-state (atom {:text "Hello world!"}))
(defonce btn-state (atom {:fixlabels ["games" "apps" "downloads" "donate" "blog"]
                          :colors ["#FFDDBB" "#FFBB99" "#FF9966" "#FF6633" "#FF3300"]
                          :labels ["games" "apps" "downloads" "donate" "blog"]
                          :oldlabels ["games" "apps" "downloads" "donate" "blog"]
                          :active "blog"
                          :oldactive "blog"}))


(def btnstyle {:color "#FFFFFF"
               :background "none"
               :padding 10
               :font-size "1.9em"
               :font-weight 400
               :font-family "-apple-system,BlinkMacSystemFont,Avenir,Avenir Next,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Fira Sans,Droid Sans,Helvetica Neue,sans-serif"
               :border "none"})


(def fltstyle {:background "#EEEEEEFF"
               :padding 0
               :margin "0px"
               :position "absolute"
               :width "200px"
               :height "100vh"
               :left "0px"
               :top  "0px"})


(defn submenu [active]
  (fn []
    (if active
      [:div {:style {:position "relative"
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



(defn menucard [ label labelch oldlabelch ]
  (let [conwidth (min (.-innerWidth js/window) 600)
        tabwidth (/ (- (/ (.-innerWidth js/window ) 2) (/ conwidth 2 )) 4)
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
        size-spring (anim/spring size {:mass 10.0 :stiffness 0.5 :damping 2.0})
        ]
    (fn a-button []
      [:div
       [anim/timeout #(reset! pos newpos) 100]
       [anim/timeout #(reset! size newsize) 100]
       [:div {
              :style (-> fltstyle
                         ;;(assoc :z-index btnindex)
                         (assoc :background color)
                         (assoc :transform (str "translate(" @pos-spring "px)"))
                         (assoc :width @size-spring))

          :on-click (fn [e]
                      (let [newmenu (concat (filter #(not= % label) (@btn-state :labels)) [label])]
                        (println label " : " newmenu)
                        (swap! btn-state assoc :oldlabels (@btn-state :labels))
                        (swap! btn-state assoc :labels newmenu)
                        (swap! btn-state assoc :oldactive (@btn-state :active))
                        (swap! btn-state assoc :active label)
                        ))
              }
        [:input
         {:type "button"
          :key (str label "button")
          :value label
          :style btnstyle
;;                      (assoc :transform (str "translate(" @pos-spring "px)")))
}]
        [:div {:style {:height "50px"}}]
        [(submenu (= label (@btn-state :active)))]
        [:div {:style {;;:position "relative"
                       ;;:width "600px"
                       :top "50px"
                       :margin "10px"
                       :background "none"}}
         "February January 2019 December November October September August July June May April March February January 2018"]]
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
  [:div
    [:div {:style {:position "absolute"
                   :margin "10px"
                   :top "0px"
                   :right "0px"
                   :color "#FFFFFF"
                   :background "none"
                   :font-size "1.9em"
                   :font-weight 400
                   :font-family "-apple-system,BlinkMacSystemFont,Avenir,Avenir Next,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Fira Sans,Droid Sans,Helvetica Neue,sans-serif"
                   }} "milgra.com"]
   [menu]
   ;;[submenu]
   ]))


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

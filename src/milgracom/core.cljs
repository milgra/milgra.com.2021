(ns milgracom.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reanimated.core :as anim]
            [clojure.string :as str]))

(defonce app-state (atom {:text "Hello world!"}))
(defonce btn-state (atom {:labels ["games" "apps" "downloads" "donate" "blog"]
                          :oldlabels ["games" "apps" "downloads" "donate" "blog"]
                          :active "blog"
                          :oldactive "blog"}))


(def btnstyle {:color "#333333"
               :background "none"
               :padding 0
               :margin "20px"
               :position "absolute"
               :left "0px"
               :top  "20px"
               :font-size "2em"
               :font-weight 400
               :font-family "-apple-system,BlinkMacSystemFont,Avenir,Avenir Next,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Fira Sans,Droid Sans,Helvetica Neue,sans-serif"
               :border "none"})


(defn menubutton [ label labelch oldlabelch ]
  (let [oldindex (str/index-of oldlabelch label)
        index (str/index-of labelch label)
        oldpos (if (= label (@btn-state :oldactive))
                 600
                 (* oldindex 22))
        pos (reagent/atom oldpos)
        pos-spring (anim/spring pos {:mass 5.0 :stiffness 1.0 :damping 1.0})
        newpos (if (= label (@btn-state :active))
                 600
                 (* index 22))]
    (fn a-button []
      [:div {:style {:width "100%" :height "1em"}}
       [anim/timeout #(reset! pos newpos) 100]
       [:input
        {:type "button"
         :key (str label "button")
         :value label
         :style (-> btnstyle
                    (assoc :position "absolute")
                    (assoc :transform (str "translate(" @pos-spring "px)")))
         :on-click (fn [e]
                     (let [newmenu (concat (filter #(not= % label) (@btn-state :labels)) [label])]
                       (println label " : " newmenu)
                       (swap! btn-state assoc :oldlabels (@btn-state :labels))
                       (swap! btn-state assoc :labels newmenu)
                       (swap! btn-state assoc :oldactive (@btn-state :active))
                       (swap! btn-state assoc :active label)
                       ))}]
       ])))

(defn menu []
  (println "rerender")
  (fn []
    (let [labelch (apply str (@btn-state :labels))
          oldlabelch (apply str (@btn-state :oldlabels))]
      [:div
       (map (fn [label] [(menubutton label labelch oldlabelch)]) (@btn-state :labels))])))

(defn submenu []
  (fn []
    [:div {:style {:position "absolute"
                   :left 0
                   :right 0
                   :margin-left "auto"
                   :margin-right "auto"
                   :top "100px"
                   :width "600px"
                   :overflow-x "auto"
                   :white-space "nowrap"
                   }}
     "February January 2019 December November October September August July June May April March February January 2018"])
  )

(defn content []
  (fn []
    [:div  {:style {:position "absolute"
                    :left 0
                    :right 0
                    :margin-left "auto"
                    :margin-right "auto"
                    :top "200px"
                    :align "center"
                    :width "600px"
                    :height "600px"
                    :background "#555555"}}
     "February January 2019 December November October September August July June May April March February January 2018"])
    )

(defn page []
  (fn []
  [:div
   [menu]
   [submenu]
   [content]
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

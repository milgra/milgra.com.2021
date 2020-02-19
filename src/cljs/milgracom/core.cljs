(ns milgracom.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reanimated.core :as anim]
            [markdown-to-hiccup.core :as m]
            [clojure.string :as str]
            [clojure.core.async :as async]
            [cljs.pprint :as print :refer [cl-format]]
            [cljs-http.client :as http]))


(defonce menu-labels ["blog" "games" "apps" "files"])
(defonce menu-colors [0xc3ebff
                      0xa0cee5
                      0xc3ebff
                      0xa0cee5])
                     
(defonce blog-posts (atom nil))
(defonce blog-months (atom nil))
(defonce menu-state (atom {:newlabels ["blog" "games" "apps" "files"]
                           :oldlabels ["blog" "games" "apps" "files"]}))
(defonce tabwidth 50)

(defn get-months [label]
  (if (= label "blog")
    (async/go
      (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/months"))
            months (:result
                    (js->clj
                     (.parse js/JSON body)
                     :keywordize-keys true))]
        (println "months arrived")
        (reset! blog-months months)))))


(defn get-posts [year month]
    (async/go
      (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/posts"
                                                      {:query-params {:year 2016 :month 10}}))
            posts (:result
                   (js->clj
                   (.parse js/JSON body)
                     :keywordize-keys true))
            ]
        (println "posts arrived" posts)
        (reset! blog-posts posts)
        )))


(defn get-metrics
  "calculates size, position and color for menucard"
  [label]
  (let [index (.indexOf menu-labels label)
        ;; get old and new positions
        oldindex (.indexOf (@menu-state :oldlabels) label)
        newindex (.indexOf (@menu-state :newlabels) label)]
    {:oldcolor (nth menu-colors index)
     :newcolor (nth menu-colors index)
     ;; get old and new positions
     :oldpos (if (= label (last (@menu-state :oldlabels))) 150 (* oldindex tabwidth))
     :newpos (if (= label (last (@menu-state :newlabels))) 150 (* newindex tabwidth))
     ;; get old and new size
     :oldsize (if (= label (last (@menu-state :oldlabels))) 650 tabwidth)
     :newsize (if (= label (last (@menu-state :newlabels))) 650 tabwidth)}))


(defn sidemenubtn
  "returns a side menu button component with the proper contents for given label"
  [[index [year month]]]
  (let [;; component-local reagent atoms for animation
        pos (reagent/atom 500)
        ;; spring animators 
        pos-spring (anim/spring pos {:mass 10.0 :stiffness 0.5 :damping 2.0})
        ]
    (fn a-sidemenubtn []
      [:div
       ;; animation structure
       [anim/timeline
        (* 100 index)
        #(reset! pos 0)
        1000
        #(get-posts 2018 4)
        ]
       ;; menucard start
       [:div
        {:class "sidemenubtn"
         :style {:transform (str "translate(" @pos-spring "px)")
                 :background "linear-gradient(90deg, #a0cee5 0%,  #a0cee5 20%, #a0cee500 100%)"
                 ;;:top (str (* index 30) "px")
                 }}
        (str year "-" month)]
       [:div {:style {:height "4px"}}]
      ]
    )))


(defn sidemenu
  []
  (println "sidemenu")
  (fn a-sidemenu []
    (let [months @blog-months]
      (if months
        [:div
         {:style{:background "none"
                 :position "absolute"
                 :top "140px"
                 :left "590px"
                 }}
         [:div
          (map (fn [label] [(sidemenubtn label)]) (map-indexed vector months))
         ]]
        ))))


(defn content
  []
  (fn a-content []
    (let [posts @blog-posts]
      (if posts
        [:div
         {:class "content"}
         ;; :dangerouslySetInnerHTML {:__html "<b>FASZT</b>"}}
         (m/component (m/md->hiccup ((first posts) :content)))
         ;;"FASZT"
         ;; [:br]
         ;; (str (post :title))
         ;; [:br]
         ;; (str (post :date))
         ;; [:br]
         ;; (str (post :content))
       ]))))
  
  
(defn menucard
  "returns a menucard component with the proper contents for given label"
  [label]
  (let [active (= label (last (@menu-state :newlabels)))
        metrics (get-metrics label)
        ;; component-local reagent atoms for animation
        pos (reagent/atom (metrics :oldpos))
        size (reagent/atom (metrics :oldsize))
        color (reagent/atom (metrics :oldcolor))
        ;; spring animators 
        pos-spring (anim/spring pos {:mass 10.0 :stiffness 0.5 :damping 2.0})
        size-spring (anim/spring size {:mass 5.0 :stiffness 0.5 :damping 2.0})
        color-spring (anim/spring color {:mass 5.0 :stiffness 0.5 :damping 2.0})]
    (println "active" active)
    (reset! blog-months nil)
    (reset! blog-posts nil)
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
        #(get-months label)]
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
        [:div {:class "cardbutton"} label ]
        ;; menucard submenu
        (if (and active (= label "blog")) [sidemenu])
        ;; menucard content
        (if (and active (= label "blog")) [content])
        ]
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

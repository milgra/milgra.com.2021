(ns milgracom.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reanimated.core :as anim]
            [markdown-to-hiccup.core :as m]
            [clojure.string :as str]
            [clojure.core.async :as async]
            [cljs.pprint :as print :refer [cl-format]]
            [cljs-http.client :as http]))


(defonce menu-labels ["blog" "apps" "games" "protos"])
(defonce menu-colors [0x9dfc92
                      0x2ff01a
                      0x9dfc92
                      0x2ff01a])
                     
(defonce blog-posts (atom nil))
(defonce blog-months (atom nil))
(defonce blog-projects (atom nil))
(defonce menu-state (atom {:newlabels ["blog" "apps" "games" "protos"]
                           :oldlabels ["blog" "apps" "games" "protos"]}))
(defonce tabwidth 50)
(defonce months ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"])
(defonce selected-month (atom nil))

(defn get-posts [year month]
    (async/go
      (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/posts"
                                                      {:query-params {:year year :month month}}))
            posts (:result
                   (js->clj
                   (.parse js/JSON body)
                     :keywordize-keys true))
            ]
        (println "posts arrived" posts)
        (reset! blog-posts posts)
        )))


(defn get-months []
    (async/go
      (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/months"))
            months (:result
                    (js->clj
                     (.parse js/JSON body)
                     :keywordize-keys true))
            [year month] (if (> (count months) 0) (last months))
            ]
        (println "months arrived")
        (get-posts year month)
        (reset! blog-months months))))


(defn get-projects [type]
    (async/go
      (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/projects"
                                                      {:query-params {:type type}}))
            projects (:result
                      (js->clj
                       (.parse js/JSON body)
                       :keywordize-keys true))
            ]
        (println "projects arrived" projects)
        (reset! blog-projects projects))))


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
     :oldsize (if (= label (last (@menu-state :oldlabels))) 750 tabwidth)
     :newsize (if (= label (last (@menu-state :newlabels))) 750 tabwidth)}))



(defn rightmenubtn
  "returns a right menu button component with the proper contents for given label"
  [[index [year month]]]
  (let [ ;; component-local reagent atoms for animation
        pos (reagent/atom 1500)
        ;; spring animators 
        pos-spring (anim/spring pos {:mass 15.0 :stiffness 0.5 :damping 3.0})
        newpos (if (and @selected-month (= year (@selected-month :year )) (= month (@selected-month :month))) 40 30)]
    (fn a-sidemenubtn []
      [:div {:class "a-sidemenubtn"}
       ;; animation structure
       [anim/timeline
        (* 100 index)
        #(reset! pos newpos)]
       ;; menucard start
       [:div
        {:id "sidemenubtn"
         :class "sidemenubtn"
         :style {:transform (str "translate(" @pos-spring "px)")
                 :background (if (= (mod index 2) 0) "#9dfc92" "#2ff01a")
                 }
         :on-click (fn [e]
                     (reset! pos 40)
                     (reset! selected-month {:year year :month month})
                     (get-posts year month))
         }
        (str (nth months month) " " year)]
       [:div {:id "sidemenubottom"
              :style {:height "-1px"}}]
      ])
    ))


(defn rightmenu
  []
  (println "right")
  (fn a-sidemenu []
    (let [;;smonth @selected-month
          months @blog-months]
      (if months
        [:div
         {:id "sidemenu"
          :style{:background "none"
                 :position "absolute"
                 :top "200px"
                 :left "-180px"
                 }}
         [:div {:class "sidemenubody"}
          (map (fn [label] [(rightmenubtn label)]) (map-indexed vector months))
          ]]
        "LOADING"
        ))))



(defn sidemenubtn
  "returns a side menu button component with the proper contents for given label"
  [[index [year month]]]
  (let [ ;; component-local reagent atoms for animation
        pos (reagent/atom 1500)
        ;; spring animators 
        pos-spring (anim/spring pos {:mass 15.0 :stiffness 0.5 :damping 3.0})
        newpos (if (and @selected-month (= year (@selected-month :year )) (= month (@selected-month :month))) 40 30)]
    (fn a-sidemenubtn []
      [:div {:class "a-sidemenubtn"}
       ;; animation structure
       [anim/timeline
        (* 100 index)
        #(reset! pos newpos)]
       ;; menucard start
       [:div
        {:id "sidemenubtn"
         :class "sidemenubtn"
         :style {:transform (str "translate(" @pos-spring "px)")
                 :background (if (= (mod index 2) 0) "#9dfc92" "#2ff01a")
                 }
         :on-click (fn [e]
                     (reset! pos 40)
                     (reset! selected-month {:year year :month month})
                     (get-posts year month))
         }
        (str (nth months month) " " year)]
       [:div {:id "sidemenubottom"
              :style {:height "-1px"}}]
      ])
    ))


(defn sidemenu
  []
  (println "sidemenu")
  (fn a-sidemenu []
    (let [;;smonth @selected-month
          months @blog-months]
      (if months
        [:div
         {:id "sidemenu"
          :style{:background "none"
                 :position "absolute"
                 :top "200px"
                 :left "-180px"
                 }}
         [:div {:class "sidemenubody"}
          (map (fn [label] ^{:key label} [(sidemenubtn label)]) (map-indexed vector months))
          ]]))))


(defn content-projects
  [type]
  )


(defn content-posts
  []
  (fn a-content []
    (let [posts @blog-posts]
      (if posts
        [:div
         {:id "a-content"
          :class "content"}
         ;; :dangerouslySetInnerHTML {:__html "<b>FASZT</b>"}}
         ((first posts) :title)
         [:br]
         ((first posts) :date)
         [:br]
         (str ((first posts) :tags))
         [:br]
         (m/component (m/md->hiccup ((first posts) :content)))
         ;;"FASZT"
         ;; [:br]
         ;; (str (post :title))
         ;; [:br]
         ;; (str (post :date))
         ;; [:br]
         ;; (str (post :content))
         ]
        "LOADING"
        ))))

  
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
        size-spring (anim/spring size {:mass 3.0 :stiffness 0.5 :damping 2.0})
        color-spring (anim/spring color {:mass 5.0 :stiffness 0.5 :damping 2.0})]
    
    (if (and active (= label "blog"))
      (do
        (reset! blog-months nil)
        (reset! blog-posts nil)
        (reset! selected-month nil)))
    
    (fn a-menucard []
      (println "a-menucard")
      
      [:div {:key (str "menucard" label)}
       ;; animation structure
       [anim/timeline
        0
        #(reset! color (metrics :newcolor))
        300
        #(reset! pos (metrics :newpos))
        300
        #(reset! size (metrics :newsize))
        300
        #(cond
           (and active (= label "blog"))
           (get-months)
           (and active (= label "projects"))
           (get-projects "game"))]
       ;; menucard start
       [:div
        {:key label
         :class "card"
         :style {:background (cl-format nil "#~6,'0x" @color-spring)
                 :transform (str "translate(" @pos-spring "px)")
                 :width @size-spring}}
        ;; menucard button
        [:div {:key "cardbutton"
               :class "cardbutton"
               :on-click (fn [e]
                           (let [new-state (concat (filter #(not= % label) (@menu-state :newlabels)) [label])]
                             (swap! menu-state assoc :oldlabels (@menu-state :newlabels))
                             (swap! menu-state assoc :newlabels new-state)))}
         label]
        ;;menucard submenu
        (if (and active (= label "blog")) [sidemenu])
        ;;menucard content
        (cond
          (and active (= label "blog")) [content-posts]
          (and active (= label "apps")) [content-projects "apps"]
          (and active (= label "games")) [content-projects "games"]
          (and active (= label "protos")) [content-projects "protos"])]])))


(defn page []
  "returns a page component"
  (fn []
    [:div {:key "pagecomp"
           :style {:position "absolute"
                   :width "900px"
                   :left 0
                   :right 0
                   :top 0
                   :bottom 0
                   :border "0px"
                   :margin-left "auto"
                   :margin-right "auto"
                   :background "none"}}
     [:div {:id "pagecompbody"}      
       (map (fn [label] ^{:key label} [(menucard label)]) (@menu-state :newlabels))]
     [:div {:key "logo"
            :class "logo"} "milgra.com"]]))

(defn parse-params
  "Parse URL parameters into a hashmap"
  []
  (let [param-strs (-> (.-location js/window) (clojure.string/split #"\?") last (clojure.string/split #"\&"))]
    (into {} (for [[k v] (map #(clojure.string/split % #"=") param-strs)]
               [(keyword k) v]))))

(defn start []
  (println "PARAMS" (parse-params))
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

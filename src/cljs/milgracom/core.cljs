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

(defonce lmenuitems (atom nil))
(defonce rmenuitems (atom nil))
(defonce selecteditem (atom nil))
(defonce selectedpage (atom nil))

(defonce blog-months (atom nil))
(defonce blog-posts (atom nil))
(defonce blog-projects (atom nil))
(defonce menu-state (atom {:newlabels ["blog" "apps" "games" "protos"]
                           :oldlabels ["blog" "apps" "games" "protos"]}))
(defonce tabwidth 50)
(defonce monthnames ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"])
(defonce selected-month (atom nil))

(defn get-posts [year month]
    (async/go
      (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/posts"
                                                      {:query-params {:year year :month month}}))
            posts (:posts (js->clj (.parse js/JSON body) :keywordize-keys true))]
        (reset! blog-posts posts))))


(defn get-months []
    (async/go
      (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/months"))
            result (js->clj (.parse js/JSON body) :keywordize-keys true)
            months (result :months)
            tags (result :tags) 
            labels (reduce
                    (fn [res [year month]] (conj res (str (nth monthnames month) " " year)))
                    []
                    months)
            [year month] (if (> (count months) 0) (last months))]
        (reset! blog-months months)
        (reset! lmenuitems labels)
        (reset! rmenuitems tags)
        (get-posts year month))))


(defn get-projects [type]
    (async/go
      (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/projects"
                                                      {:query-params {:type type}}))
            result (js->clj (.parse js/JSON body) :keywordize-keys true)
            projects (result :projects)
            labels (map #(% :title) projects)
            tags (result :tags)]
        (println "result" result)
        (reset! blog-projects projects)
        (reset! lmenuitems labels)
        (reset! rmenuitems tags))))


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
  "returns a side menu button component with the proper contents for given label"
  [[index label]]
  (let [ ;; component-local reagent atoms for animation
        pos (reagent/atom (/ (.-innerWidth js/window) 2))
        ;; spring animators 
        pos-spring (anim/spring pos {:mass 15.0 :stiffness 0.5 :damping 3.0})
        newpos (if (= @selecteditem label) 40 30)]
    (fn a-leftmenubtn []
      [:div {:class "a-leftmenubtn"}
       ;; animation structure
       [anim/timeline
        (+ 450 (* 100 index))
        #(reset! pos newpos)]
       ;; menucard start
       [:div
        {:id "rightmenubtn"
         :class "rightmenubtn"
         :style {:transform (str "translate(" @pos-spring "px)")
                 :background (if (= (mod index 2) 0) "#9dfc92" "#2ff01a")}
         :on-click (fn [e]
                     (reset! pos 40)
                     (cond
                       (= @selectedpage "blog")
                       (let [[year month] (nth @blog-months index)] 
                         (reset! selected-month {:year year :month month})
                         (get-posts year month))
                     ))
         }
        label]
       [:div {:id "rightmenubottom"
              :style {:height "-1px"}}]])))


(defn rightmenu
  []
  (println "leftmenu")
  (fn a-leftmenu []
    (let [items @rmenuitems]
      (if items
        [:div
         {:id "leftmenu"
          :style{:background "none"
                 :position "absolute"
                 :top "200px"
                 :left "700px"
                 }}
         [:div {:class "leftmenubody"}
          (map (fn [item] ^{:key item} [(rightmenubtn item)]) (map-indexed vector items))
          ]]))))


(defn leftmenubtn
  "returns a side menu button component with the proper contents for given label"
  [[index label]]
  (let [ ;; component-local reagent atoms for animation
        pos (reagent/atom (/ (.-innerWidth js/window) -2))
        ;; spring animators 
        pos-spring (anim/spring pos {:mass 15.0 :stiffness 0.5 :damping 3.0})
        newpos (if (= @selecteditem label) 40 30)]
    (fn a-leftmenubtn []
      [:div {:class "a-leftmenubtn"}
       ;; animation structure
       [anim/timeline
        (* 100 index)
        #(reset! pos newpos)]
       ;; menucard start
       [:div
        {:id "leftmenubtn"
         :class "leftmenubtn"
         :style {:transform (str "translate(" @pos-spring "px)")
                 :background (if (= (mod index 2) 0) "#9dfc92" "#2ff01a")
                 }
         :on-click (fn [e]
                     (reset! pos 40)
                     (cond
                       (= @selectedpage "blog")
                       (let [[year month] (nth @blog-months index)] 
                         (reset! selected-month {:year year :month month})
                         (get-posts year month))
                     ))
         }
        label]
       [:div {:id "leftmenubottom"
              :style {:height "-1px"}}]])))


(defn leftmenu
  []
  (println "leftmenu")
  (fn a-leftmenu []
    (let [items @lmenuitems]
      (if items
        [:div
         {:id "leftmenu"
          :style{:background "none"
                 :position "absolute"
                 :top "200px"
                 :left "-180px"
                 }}
         [:div {:class "leftmenubody"}
          (map (fn [item] ^{:key item} [(leftmenubtn item)]) (map-indexed vector items))
          ]]))))


(defn content-projects
  [type]
  (fn a-content []
    (let [projects @blog-projects]
      (if projects
        [:div
         {:id "a-content"
          :class "content"}
         ;; :dangerouslySetInnerHTML {:__html "<b>FASZT</b>"}}
         ((first projects) :title)
         [:br]
         ((first projects) :type)
         [:br]
         (str ((first projects) :tags))
         [:br]
         (m/component (m/md->hiccup ((first projects) :content)))
         ;;"FASZT"
         ;; [:br]
         ;; (str (post :title))
         ;; [:br]
         ;; (str (post :date))
         ;; [:br]
         ;; (str (post :content))
         ]
        ""
        ))))


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
        ""
        ))))


(defn pagecard
  "returns a pagecard component with the proper contents for given label"
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
    
    (fn a-pagecard []
      (println "a-pagecard")
      
      [:div {:key (str "pagecard" label)}
       ;; animation structure
       [anim/timeline
        0
        #(reset! color (metrics :newcolor))
        300
        #(reset! pos (metrics :newpos))
        300
        #(reset! size (metrics :newsize))
        300
        #(when active
           (cond
             (= label "blog")
             (get-months)
             (= label "games")
             (get-projects "game")
             (= label "apps")
             (get-projects "app")
             (= label "protos")
             (get-projects "proto")))]
       ;; pagecard start
       [:div
        {:key label
         :class "card"
         :style {:background (cl-format nil "#~6,'0x" @color-spring)
                 :transform (str "translate(" @pos-spring "px)")
                 :width @size-spring}}
        ;; pagecard button
        [:div {:key "cardbutton"
               :class "cardbutton"
               :on-click (fn [e]
                           (reset! lmenuitems nil)
                           (reset! rmenuitems nil)
                           (reset! blog-months nil)
                           (reset! blog-posts nil)
                           (reset! blog-projects nil)
                           (reset! selected-month nil)
                           (reset! selectedpage label)
                           (let [new-state (concat (filter #(not= % label) (@menu-state :newlabels)) [label])]
                             (swap! menu-state assoc :oldlabels (@menu-state :newlabels))
                             (swap! menu-state assoc :newlabels new-state)))}
         label]
        ;;pagecard submenu
        (if active [leftmenu])
        (if active [rightmenu])
        ;;pagecard content
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
       (map (fn [label] ^{:key label} [(pagecard label)]) (@menu-state :newlabels))]
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
  (reset! selectedpage "blog")
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

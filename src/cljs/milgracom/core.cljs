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
(defonce tabwidth 50)
(defonce monthnames ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"])

(defonce selectedpage (atom nil))
(defonce menu-state (atom {:newlabels ["blog" "apps" "games" "protos"]
                           :oldlabels ["blog" "apps" "games" "protos"]}))
(defonce page-state (atom :normal))
(defonce mode-admin (atom false))
(defonce pass (atom nil))

(defn get-posts [year month blog-posts]
  (async/go
    (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/posts"
                                                    {:query-params {:year year :month month}}))
          posts (:posts (js->clj (.parse js/JSON body) :keywordize-keys true))]
      (reset! blog-posts posts))))


(defn get-months [lmenuitems rmenuitems blog-months blog-posts]
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
      (get-posts year month blog-posts))))


(defn get-projects [type lmenuitems rmenuitems blog-projects]
  (async/go
    (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/projects"
                                                    {:query-params {:type type}}))
          result (js->clj (.parse js/JSON body) :keywordize-keys true)
          projects (result :projects)
          labels (map #(% :title) projects)
          tags (result :tags)]
      (reset! blog-projects projects)
      (reset! lmenuitems labels)
      (reset! rmenuitems tags))))


(defn get-comments [postid comments]
  (async/go
    (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/comments"
                                                    {:query-params {:postid postid}}))
          result (js->clj (.parse js/JSON body) :keywordize-keys true)
          ncomments (result :comments)]
      (reset! comments ncomments))))


(defn send-comment [postid comments nick text code]
  (async/go
    (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/newcomment"
                                                    {:query-params {:postid postid :nick nick :text text :code code}}))
          result (js->clj (.parse js/JSON body) :keywordize-keys true)
          status (result :result)]
      (get-comments postid comments)
      )))

(defn delete-comment [id]
  (async/go
    (let [{:keys [status body]} (async/<! (http/get "http://localhost:3000/delcomment"
                                                    {:query-params {:id id}}))
          result (js->clj (.parse js/JSON body) :keywordize-keys true)
          status (result :result)])))
 

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
  (let [selecteditem (atom nil)
        ;; component-local reagent atoms for animation
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
                     (reset! selecteditem label)
                     (cond
                       (= @selectedpage "blog")
                       (println "e")))}
        label]
       [:div {:id "rightmenubottom"
              :style {:height "-1px"}}]])))


(defn rightmenu
  [rmenuitems]
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
  [[index label] blog-months blog-posts postpos]
  (let [selecteditem (atom nil)
        ;; component-local reagent atoms for animation
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
                     (reset! selecteditem label)
                     (cond
                       (= @selectedpage "blog")
                       (let [[year month] (nth @blog-months index)] 
                         (get-posts year month blog-posts))
                     ))
         }
        label]
       [:div {:id "leftmenubottom"
              :style {:height "-1px"}}]])))


(defn leftmenu
  [lmenuitems blog-months blog-posts postpos]
  (fn a-leftmenu []
    (let [items @lmenuitems]
      (if items
        [:div
         {:id "leftmenu"
          :style{:background "none"
                 :position "absolute"
                 :top "200px"
                 :left "-180px"}}
         [:div {:class "leftmenubody"}
          (map (fn [item] ^{:key item} [(leftmenubtn item blog-months blog-posts postpos)]) (map-indexed vector items))]]))))


(defn impressum []
  [:div {:class "impressum"}
   "www.milgra.com by Milan Toth | Powered by Clojure and Datomic."])


(defn comments [postid comments showcomments showeditor]
  (let [nick (atom nil)
        text (atom nil)
        code (atom nil)]
    ;;(fn []
      [:div
       [:div {:class "comments"}
        [:div {:style {:padding-right "20px"
                       :cursor "pointer"}
               :class "shwocommentbtn"
               :on-click (fn []
                           (get-comments postid comments) 
                           (swap! showcomments not))}
         "5 comments"]
        "|"
        [:div {:style {:padding-left "20px"
                       :cursor "pointer"}
               :class "shwocommentbtn"
               :on-click (fn [] (swap! showeditor not))}
         " Post comment"]
        (if @mode-admin
          [:div {:style {:padding-left "20px"
                       :cursor "pointer"}
               :class "shwocommentbtn"
               :on-click (fn [] (swap! showeditor not))}
           "[Edit post]"])]
       
       (if @showeditor
         [:div
          [:div
           [:div {:style {:padding-top "20px" :padding-bottom "20px" :width "100%" :text-align "center"}} "Nick"]
           [:input {:style {:width "150px"
                            :display "block"
                            :margin-left "auto"
                            :margin-right "auto"}
                    :on-change #(reset! nick (-> % .-target .-value))}]
           [:div {:style {:width "100%" :text-align "center" :padding-top "20px" :padding-bottom "20px"}} "Comment"]
           [:textarea {:style {
                               :width "100%"
                               :height "100px"}
                               :on-change #(reset! text (-> % .-target .-value))}]
           [:div {:style {:width "100%" :text-align "center" :padding-top "20px" :padding-bottom "20px"}} "How much is nine multiplied by eight? Use numbers"]
           [:input {:style {:width "150px"  
                            :display "block"
                            :margin-left "auto"
                            :margin-right "auto"}
                    :on-change #(reset! code (-> % .-target .-value))}]
           [:br]
           [:div {:style {:width "100%" :text-align "center"}
                  :on-click (fn [event]
                               (swap! showeditor not)
                               (if @showeditor (send-comment postid comments @nick @text @code)) 
                               )}
            "Send Comment"]
           ]])
       (if (and @showcomments @comments)
         (map (fn [comment]
                [:div {:key (rand 1000000)}
                 [:h3
                  (comment :nick)
                  "|"
                  (comment :date)]
                 [:div (comment :content)]
                 (if @mode-admin
                   [:div {:style {:cursor "pointer"}
                          :class "showcommentbtn"
                          :on-click (fn [event] (delete-comment (comment :id)))} "Delete comment"])

                 [:hr {:style {:width "20%" :background-color "#FFFFFF"}}]
                 ])
              @comments))]))


(defn content-projects
  [type blog-projects]
  (let [projects @blog-projects]
    (if projects
      [:div
       (map (fn [project]
              [:div
               {:key (rand 1000000)
                :id "a-content"
                :class "content"}
               ;; :dangerouslySetInnerHTML {:__html "<b>FASZT</b>"}}
               (project :title)
               [:br]
               (project :type)
               [:br]
               (str (project :tags))
               [:br]
               (m/component (m/md->hiccup (project :content)))
               ])
            projects)])))


(defn content-posts
  [blog-posts]
  (if @blog-posts
    (fn []
      [:div {:id "a-content"
             :class "content"}
       [:div {:style {:border-radius "10px"
                        :height "100%"}}
        (map (fn [post]
               (let [showcomments (atom false)
                     showeditor (atom false)
                     comms (atom nil)]
                 [:div {:key (rand 1000000)}
                  ;; :dangerouslySetInnerHTML {:__html "<b>FASZT</b>"}}
                  [:h1 (post :title)]
                  [:h2 (post :date)]
                  [:h2 (clojure.string/join "," (post :tags))]
                  (m/component (m/md->hiccup (post :content)))
                  [:br]
                  [comments (post :id) comms showcomments showeditor]
                  [:br]
                  [:hr]
                  [:br]
                  ]))
             @blog-posts)]])))


(defn pagecard
  "returns a pagecard component with the proper contents for given label"
  [label]
  (let [lmenuitems (atom nil)
        rmenuitems (atom nil)

        blog-months (atom nil)
        blog-posts (atom nil)
        blog-projects (atom nil)

        active (= label (last (@menu-state :newlabels)))
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
      [:div {:key (str "pagecard" label)}
       ;; animation structure
       [anim/timeline
        0
        #(reset! color (metrics :newcolor))
        0
        #(reset! pos (metrics :newpos))
        300
        #(reset! size (metrics :newsize))
        300
        #(when active
           (cond
             (= label "blog")
             (get-months lmenuitems rmenuitems blog-months blog-posts)
             (= label "games")
             (get-projects "game" lmenuitems rmenuitems blog-projects)
             (= label "apps")
             (get-projects "app" lmenuitems rmenuitems blog-projects)
             (= label "protos")
             (get-projects "proto" lmenuitems rmenuitems blog-projects)))
        ]
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
                           (reset! selectedpage label)
                           (let [new-state (concat (filter #(not= % label) (@menu-state :newlabels)) [label])]
                             (swap! menu-state assoc :oldlabels (@menu-state :newlabels))
                             (swap! menu-state assoc :newlabels new-state)))}
         label]
        ;;pagecard submenu
        (if active [leftmenu lmenuitems blog-months blog-posts])
        (if active [rightmenu rmenuitems])
        ;;pagecard content
        (cond
          (and active (= label "blog")) [content-posts blog-posts]
          (and active (= label "apps")) [content-projects "apps" blog-projects]
          (and active (= label "games")) [content-projects "games" blog-projects]
          (and active (= label "protos")) [content-projects "protos" blog-projects])
        [:br]
        ;;impressum
        (if (or @blog-posts @blog-projects)
          [impressum])
        ]])))


(defn newproject []
  [:div {:style {:position "absolute" :width "100%"}}
   [:div {:style {:padding-top "20px" :padding-bottom "20px" :width "100%" :text-align "center"}} "Title"]
   [:input {:style {:width "300px"
                    :display "block"
                    :margin-left "auto"
                    :margin-right "auto"}
            ;;:on-change #(reset! nick (-> % .-target .-value))}
            }]
   [:div {:style {:padding-top "20px" :padding-bottom "20px" :width "100%" :text-align "center"}} "Tags"]
   [:input {:style {:width "300px"
                    :display "block"
                    :margin-left "auto"
                    :margin-right "auto"}
            ;;:on-change #(reset! nick (-> % .-target .-value))}
            }]
   [:div {:style {:padding-top "20px" :padding-bottom "20px" :width "100%" :text-align "center"}} "Type"]
   [:input {:style {:width "300px"
                    :display "block"
                    :margin-left "auto"
                    :margin-right "auto"}
            ;;:on-change #(reset! nick (-> % .-target .-value))}
            }]
   [:div {:style {:width "100%" :text-align "center" :padding-top "20px" :padding-bottom "20px"}} "Content"]
   [:textarea {:style {
                       :width "100%"
                       :height "500px"}
               ;;:on-change #(reset! text (-> % .-target .-value))
               }]
   [:div {:style {:width "100%" :text-align "center"}
          :on-click (fn [event] )}
    "Post!"]])



(defn newpost []
  [:div {:style {:position "absolute" :width "100%"}}
   [:div {:style {:padding-top "20px" :padding-bottom "20px" :width "100%" :text-align "center"}} "Title"]
   [:input {:style {:width "300px"
                    :display "block"
                    :margin-left "auto"
                    :margin-right "auto"}
            ;;:on-change #(reset! nick (-> % .-target .-value))}
            }]
   [:div {:style {:padding-top "20px" :padding-bottom "20px" :width "100%" :text-align "center"}} "Date"]
   [:input {:style {:width "150px"
                    :display "block"
                    :margin-left "auto"
                    :margin-right "auto"}
            ;;:on-change #(reset! nick (-> % .-target .-value))}
            }]
   [:div {:style {:padding-top "20px" :padding-bottom "20px" :width "100%" :text-align "center"}} "Tags"]
   [:input {:style {:width "300px"
                    :display "block"
                    :margin-left "auto"
                    :margin-right "auto"}
            ;;:on-change #(reset! nick (-> % .-target .-value))}
            }]
   [:div {:style {:width "100%" :text-align "center" :padding-top "20px" :padding-bottom "20px"}} "Post"]
   [:textarea {:style {
                       :width "100%"
                       :height "500px"}
               ;;:on-change #(reset! text (-> % .-target .-value))
               }]
   [:div {:style {:width "100%" :text-align "center"}
          :on-click (fn [event] )}
    "Post!"]])


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
     (cond
       (= @page-state :newpost)
       [newpost]
       (= @page-state :newproject)
       [newproject]
       :else
       [:div {:id "pagecompbody"}      
        (map (fn [label] ^{:key label} [(pagecard label)]) (@menu-state :newlabels))])

     [:div {:key "logo"
            :class "logo"}
      [:div {:class "logobutton"} "milgra.com"]]
     (if @mode-admin
       [:div {:style {:position "absolute"
                       :right "-110px"}}
        [:input {:style {:width "100px"}
                 :on-change #(reset! pass (-> % .-target .-value))
                 :type "password" }]
        [:div {:class "logobutton"
                :on-click (fn [e] (reset! page-state :newpost))} "add post"]
        [:div {:class "logobutton"
               :on-click (fn [e] (reset! page-state :newproject))} "add project"]
        [:div {:class "logobutton"
               :on-click (fn [e] (reset! page-state :normal))} "return"]
        ])
     ]))


(defn parse-params
  "Parse URL parameters into a hashmap"
  []
  (let [param-strs (->
                    (.-location js/window)
                    (clojure.string/split #"\?")
                    last
                    (clojure.string/split #"\&"))]
    (into {} (for [[k v] (map #(clojure.string/split % #"=") param-strs)]
               [(keyword k) v]))))

(defn set-hash! [loc]
  (set! (.-location js/window) loc))

;;(set-hash! "/dip") ;; => http://localhost:3000/#/dip

(defn start []

  (let [params (parse-params)]
    (reset! mode-admin (params :admin))
    (reagent/render-component
     [page]
     (. js/document (getElementById "app"))))
     
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

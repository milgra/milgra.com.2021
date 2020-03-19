(ns milgracom.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reanimated.core :as anim]
            [markdown-to-hiccup.core :as m]
            [clojure.string :as str]
            [clojure.core.async :as async]
            [cljs.pprint :as print :refer [cl-format]]
            [cljs-http.client :as http]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType])            
  (:import goog.History
           goog.Uri))


(defonce server-url (if js/goog.DEBUG "http://localhost:3000" "http://116.203.87.141"))
;;(defonce server-url "http://localhost:3000")
(defonce monthnames ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"])

(defonce selected-page (atom nil))
(defonce selected-post (atom nil))

(defonce page-state (atom :normal))
(defonce mode-admin (atom false))
(defonce pass (atom nil))

(defonce posttoedit (atom nil))
(defonce blog-project (atom nil))

(defonce lmenuitems (atom nil))
(defonce rmenuitems (atom nil))

(defonce blog-months (atom nil))
(defonce blog-posts (atom nil))
(defonce blog-list (atom nil))
(defonce blog-tag (atom nil))


(defn get-posts-by-date [year month type blog-posts]
  (async/go
    (let [{:keys [status body]} (async/<! (http/get (str server-url "/api-getpostsbydate")
                                                    {:query-params {:year year :month month :type type}}))
          posts (:posts (js->clj (.parse js/JSON body) :keywordize-keys true))]
      (reset! blog-posts (reverse posts)))))


(defn get-posts-by-tag [tag]
  (reset! blog-tag tag)
  (async/go
    (let [{:keys [status body]} (async/<! (http/get (str server-url "/api-getpostsbytag")
                                                    {:query-params {:tag tag}}))
          posts (:posts (js->clj (.parse js/JSON body) :keywordize-keys true))]
      (reset! blog-list posts))))


(defn get-posts [type lmenuitems rmenuitems blog-posts]
  (async/go
    (let [{:keys [status body]} (async/<! (http/get (str server-url "/api-getposts")
                                                    {:query-params {:type type}}))
          result (js->clj (.parse js/JSON body) :keywordize-keys true)
          posts (reverse (result :posts))
          tags (result :tags)
          labels (map #(% :title) posts)]
      (reset! blog-posts posts)
      (reset! lmenuitems labels)
      (reset! rmenuitems tags))))


(defn get-post [id]
  (async/go
    (let [{:keys [status body]} (async/<! (http/get (str server-url "/api-getpost")
                                                    {:query-params {:id id}}))
          result (js->clj (.parse js/JSON body) :keywordize-keys true)
          posts (result :posts)]
      (reset! blog-posts posts))))


(defn get-months [type lmenuitems rmenuitems blog-months blog-posts]
  (async/go
    (let [{:keys [status body]} (async/<! (http/get (str server-url "/api-getmonths")
                                                    {:query-params {:type type}}))
          result (js->clj (.parse js/JSON body) :keywordize-keys true)
          months (result :months)
          tags (result :tags) 
          labels (reduce
                  (fn [res [year month]] (conj res (str (nth monthnames (dec month)) " " year)))
                  []
                  months)
          [year month] (if (> (count months) 0) (first months))]
      (reset! blog-months months)
      (reset! lmenuitems labels)
      (reset! rmenuitems tags))))


(defn get-comments [postid comments]
  (async/go
    (let [{:keys [status body]} (async/<! (http/get (str server-url "/api-getcomments")
                                                    {:query-params {:postid postid}}))
          result (js->clj (.parse js/JSON body) :keywordize-keys true)
          ncomments (result :comments)]
      (reset! comments ncomments))))


(defn delete-comment [id pass]
  (async/go
    (let [{:keys [status body]} (async/<! (http/get (str server-url "/api-removecomment")
                                                    {:query-params {:id id :pass pass}}))
          result (js->clj (.parse js/JSON body) :keywordize-keys true)
          status (result :result)])))


(defn rightmenubtn
  "returns a side menu button component with the proper contents for given label"
  [[index label]]
  (let [selected-item (atom nil)
        ;; component-local reagent atoms for animation
        pos (reagent/atom (/ (.-innerWidth js/window) 2))
        ;; spring animators 
        pos-spring (anim/spring pos {:mass 5.0 :stiffness 0.5 :damping 3.0})
        newpos (if (= @selected-item label) 40 30)]
    (fn a-leftmenubtn []
      [:div
       ;; animation structure
       [anim/timeline
        (* 40 index)
        #(reset! pos newpos)]
       ;; menucard start
       [:div
        {:id "rightmenubtn"
         :class "menubtn rightmenubtn"
         :style {:transform (str "translate(" @pos-spring "px)")
                 :background (if (= (mod index 2) 0) "#dff6df" "#d5f3d5")}
         :on-click (fn [e]
                     (reset! pos 40)
                     (reset! selected-post nil)
                     (get-posts-by-tag label))}
        label]
       [:div {:id "rightmenubottom"
              :style {:height "-1px"}}]])))


(defn rightmenu
  []
  (fn a-leftmenu []
    (let [items @rmenuitems]
      (if items
        [:div
         {:id "rightmenu"
          :class "rightmenu"}
         [:div
          (doall (map (fn [item] ^{:key item} [rightmenubtn item]) (map-indexed vector items)))]]))))


(defn leftmenubtn
  "returns a side menu button component with the proper contents for given label"
  [[index label]]
  (let [selected-item (atom nil)
        ;; component-local reagent atoms for animation
        pos (reagent/atom (/ (.-innerWidth js/window) -2))
        ;; spring animators 
        pos-spring (anim/spring pos {:mass 5.0 :stiffness 0.5 :damping 3.0})
        newpos (if (= @selected-item label) 40 30)]
    (fn a-leftmenubtn []
      [:div
       ;; animation structure
       [anim/timeline
        (* 50 index)
        #(reset! pos newpos)]
       ;; menucard start
       [:div
        {:id "a-leftmenubtn"
         :class "menubtn leftmenubtn"
         :style {:transform (str "translate(" @pos-spring "px)")
                 :background (if (= (mod index 2) 0) "#dff6df" "#d5f3d5")}
         :on-click (fn [e]
                     (reset! pos 40)
                     (reset! blog-list nil)
                     (reset! selected-post nil)
                     (reset! selected-item label)
                     (. js/history pushState "" "" "/")
                     (cond
                       (= @selected-page "blog")
                       (let [[year month] (nth @blog-months index)]
                         (get-posts-by-date year month "blog" blog-posts))
                       :else
                       (let [project (nth @blog-posts index)]
                         (reset! blog-project project))))
         }
        label]])))


(defn leftmenu
  []
  (fn a-leftmenu []
    (let [items @lmenuitems
          [year month] (first @blog-months)]
      (if items
        [:div
         [anim/timeline
         (+ 280 (* 50 (count @lmenuitems)))
          #(cond
             (not= @selected-post nil)
             (get-post @selected-post)
             (= @selected-page "blog")
             ;; get first menu item and load posts for montj
             (let [[year month] (first @blog-months)]
               (get-posts-by-date year month "blog" blog-posts))
             :else
             (let [project (first @blog-posts)]
               (reset! blog-project project)))]
         [:div
          {:id "leftmenu"
           :class "leftmenu"}
          [:div
           (doall (map (fn [item] ^{:key item} [leftmenubtn item blog-months blog-posts]) (map-indexed vector items)))]]]
        (do
          (cond
            (= @selected-page "blog")
            (get-months "blog" lmenuitems rmenuitems blog-months blog-posts)
            (= @selected-page "games")
            (get-posts "game" lmenuitems rmenuitems blog-posts)
            (= @selected-page "apps")
            (get-posts "app" lmenuitems rmenuitems blog-posts)
            (= @selected-page "protos")
            (get-posts "proto" lmenuitems rmenuitems blog-posts))
          nil)

        ))))


(defn impressum []
  [:div {:class "impressum"}
   "www.milgra.com by Milan Toth | Powered by Clojure and Datomic."])


(defn comments [post comments showcomments showeditor riddle]
  (let [nick (clojure.core/atom nil)
        text (clojure.core/atom nil)
        code (clojure.core/atom nil)]
    ;;(fn []
      [:div
       [:div {:class "comments"}
        [:div {:style {:padding-right "20px" :cursor "pointer"}
               :on-click (fn []
                           (get-comments (post :id) comments) 
                           (swap! showcomments not))}
         (str (post :comments) " comments")]
        "|"
        [:div {:style {:padding-left "20px" :cursor "pointer"}
               :on-click (fn []
                           ((fn []
                              (async/go
                                (let [{:keys [status body]} (async/<! (http/get (str server-url "/api-genriddle")))
                                      result (js->clj (.parse js/JSON body) :keywordize-keys true)
                                      question (result :question)]
                                  (swap! showeditor not)
                                  (reset! riddle question))))))}
         " Post comment"]
        (if @mode-admin
          [:div {:style {:padding-left "20px" :cursor "pointer"}
                 :on-click (fn []
                             (reset! page-state :newpost)
                             (reset! posttoedit post))}
           "[Edit post]"])]

       (if @showeditor
         [:div {:style {:text-align "center"}}
          [:br]
          [:div {:style {:width "100%" }} "Nick"]
          [:input {:style {:width "150px"}
                   :on-change #(reset! nick (-> % .-target .-value))}]
          [:div "Comment"]
          [:textarea {:style {:width "100%" :height "100px"}
                      :on-change #(reset! text (-> % .-target .-value))}]
          [:div @riddle]
          [:input {:style {:width "150px"}
                   :on-change #(reset! code (-> % .-target .-value))}]
          [:br]
          [:div {:class "showcommentbtn"
                 :style {:cursor "pointer"}
                 :on-click (fn [event]
                             (async/go
                               (let [{:keys [status body]} (async/<! (http/get (str server-url "/api-addcomment")
                                                                               {:query-params {:postid (post :id) :nick @nick :text @text :code @code}}))
                                     result (js->clj (.parse js/JSON body) :keywordize-keys true)
                                     status (result :result)]
                                 (if (= status "Invalid code")
                                   (reset! riddle "Invalid result, please try again later")
                                   (do
                                     (get-comments (post :id) comments)
                                     (reset! showcomments true)
                                     (swap! showeditor not))))))}
           "Send Comment"]])
       (if (and @showcomments @comments)
         (map (fn [comment]
                [:div {:key (rand 1000000)}
                 [:h3
                  (comment :nick)
                  "|"
                  (clojure.string/replace (comment :date) #"T" " ")]
                 [:div (comment :content)]
                 (if @mode-admin
                   [:div {:style {:cursor "pointer"}
                          :class "showcommentbtn"
                          :on-click (fn [event] (delete-comment (comment :id) @pass ))} "Delete comment"])
                 [:hr]
                 ])
              @comments))]))


(defn content-projects
  []
  (fn []
    (if @blog-project
      [:div {:id "a-content"
             :class "content"}
       (let [showcomments (atom false)
             showeditor (atom false)
             riddle (atom nil)
             comms (atom nil)]
         [:div {:key (rand 1000000)}
          [:h1
           ;;[:a {:href (str "/post/" (:id @blog-project))}
           (@blog-project :title)]
          [:h2 (clojure.string/join "," (@blog-project :tags))]
          [:br]
          (m/component (m/md->hiccup (@blog-project :content) (:encode? true)))
          [:br]
          [comments @blog-project comms showcomments showeditor riddle]
          [:br]
          [:hr]
          [impressum]
          ])])))


(defn content-posts
  []
  (fn []
    (if @blog-posts
      [:div {:id "a-content"
             :class "content"}
       (map (fn [post]
              (let [showcomments (atom false)
                    showeditor (atom false)
                    riddle (atom nil)
                    comms (atom nil)]
                [:div {:key (rand 1000000)
                       :style {:z-index "inherit"}}
                 [:h1
                  [:a {:href (str "/post/" (:id post))} (post :title)]]                 
                 [:h2 (str (clojure.string/replace (post :date) #"T" " ") " / " (clojure.string/join "," (post :tags)))]
                 [:br]
                 (m/component (m/md->hiccup (post :content)))
                 [:br]
                 [comments post comms showcomments showeditor riddle]
                 [:br]
                 [:hr]]))
            @blog-posts)
          [impressum]
       ])))


(defn content-list
  []
  (fn []
    (if @blog-list
      [:div {:id "a-content"
             :class "content"}
       [:h1 (str "Posts with the tag " @blog-tag)]
       (map (fn [post]
              [:div {:key (rand 1000000)
                     :style {:z-index "inherit"}
                     :on-click (fn [event]
                                 (reset! blog-list nil))}
               [:a {:href (str "/post/" (:id post))} (str (subs (post :date) 0 10) " " (post :title))]
                             
               [:br][:br]])
            @blog-list)
       [:br]
       [:hr]
       [impressum]
       ]
       )))
  

(defonce menuitems (atom [
                          {:color "#dff6df"
                           :posatom (reagent/atom 0)
                           :sizeatom (reagent/atom 0)
                           :label "apps"
                           :index (clojure.core/atom 5000)}
                          {:color "#d5f3d5"
                           :sizeatom (reagent/atom 0)
                           :posatom (reagent/atom 0)
                           :label "games"
                           :index (clojure.core/atom 6000)}
                          {:color "#dff6df"
                           :sizeatom (reagent/atom 0)
                           :posatom (reagent/atom 0)
                           :label "protos"
                           :index (clojure.core/atom 7000)}
                          {:color "#d5f3d5"
                           :posatom (reagent/atom 0)
                           :sizeatom (reagent/atom 0)
                           :label "blog"
                           :index (clojure.core/atom 8000)}]))

(defonce fixposes [ 0 50 100 150 ])
(defonce fixsizes [ 50 50 50 750 ])

(defn pagecard
  "returns a pagecard component with the proper contents for given label"
  [index]
  (let [item (nth @menuitems index)
        label (item :label)
       ;; component-local reagent atoms for animation
        pos (item :posatom)
        size (item :sizeatom)
        ;; spring animators
        pos-spring (anim/spring pos {:mass 10.0 :stiffness 0.5 :damping 2.0})
        size-spring (anim/spring size {:mass 3.0 :stiffness 0.5 :damping 2.0}) ]
    
    (reset! pos (nth fixposes index))
    (reset! size (nth fixsizes index))

    (fn a-pagecard []

      (let [active (= @selected-page label)
            zindex (deref (item :index)) ]

        [:div {:key (str "pagecard" label)}
         ;; pagecard start
         [:div
          {:key label
           :class "card"
           :style {:background (item :color)
                   :transform (str "translate(" @pos-spring "px)")
                   :width @size-spring
                   :z-index zindex}}
          ;; pagecard button
          [:div {:key "cardbutton"
                 :class "verticaltext cardbutton"
                 :on-click (fn [e]
                             ;; shift menuitems
                             (reset! menuitems
                                      (concat
                                       (filter #(not= (% :label) label) @menuitems)
                                       (filter #(= (% :label) label) @menuitems)))
                             ;; repos
                             (doall
                              (map (fn [[idx elem]]
                                     (reset! (elem :posatom)(nth fixposes idx))
                                     (reset! (elem :sizeatom)(nth fixsizes idx))
                                     (reset! (elem :index) idx)
                                     )
                                   (map-indexed vector @menuitems)))

                             (reset! blog-list nil)
                             (reset! lmenuitems nil)
                             (reset! rmenuitems nil)
                             (reset! blog-months nil)
                             (reset! blog-posts nil)
                             (reset! blog-project nil)
                             (reset! selected-post nil)
                             (reset! selected-page label)
                           )}
           label]
          ;;pagecard submenu
          (if active [leftmenu])
          (if active [rightmenu])
          ;;pagecard content
          (cond
            (and active (not= nil @blog-list)) [content-list]
            (and active (= label "blog")) [content-posts]
            (and active (= label "apps")) [content-projects "apps"]
            (and active (= label "games")) [content-projects "games"]
            (and active (= label "protos")) [content-projects "protos"])
          ;;impressum
          ]]))))


(defn newpost []
  (let [title (clojure.core/atom (if @posttoedit (@posttoedit :title) "title"))
        date (clojure.core/atom (if @posttoedit (@posttoedit :date) (subs (pr-str (js/Date.)) 7 26) ))
        type (clojure.core/atom (if @posttoedit (@posttoedit :type) "type"))
        tags (clojure.core/atom (if @posttoedit (clojure.string/join "," (@posttoedit :tags)) "tags,tags"))
        content (clojure.core/atom (if @posttoedit (@posttoedit :content) "content"))
        url (if @posttoedit (str server-url "/api-updatepost") (str server-url "/api-addpost"))
        id (if @posttoedit (@posttoedit :id) 0)]
  [:div {:style {:position "absolute" :width "100%" :text-align "center"}}
   [:br]
   [:div "Title"]
   [:input {:default-value @title
            :style {:width "300px"}
            :on-change #(reset! title (-> % .-target .-value))}]
   [:div "Date"]
   [:input {:default-value (if @date @date "2015-12-05T00:00:00")
            :style {:width "200px"}
            :on-change #(reset! date (-> % .-target .-value))}]
   [:div "Tags"]
   [:input {:default-value @tags
            :style {:width "300px"}
            :on-change #(reset! tags (-> % .-target .-value))}]
   [:div "Type"]
   [:input {:default-value @type
            :style {:width "300px"}
            :on-change #(reset! type (-> % .-target .-value))}]
   [:div "Post"]
   [:textarea {:default-value @content
               :style {:width "100%" :height "400px"}
               :on-change #(reset! content (-> % .-target .-value))}]
   [:div {:style {:cursor "pointer"}
          :on-click (fn [event]
                        (async/go
                          (let [{:keys [status body]}
                                (async/<! (http/post url {:form-params {:title @title
                                                                        :date @date
                                                                        :tags (clojure.string/split @tags #",")
                                                                        :type @type
                                                                        :content @content
                                                                        :pass @pass
                                                                        :id id}}))
                                result (js->clj (.parse js/JSON body) :keywordize-keys true)
                                status (result :result)]
                            (if (= status "OK")
                              (reset! page-state :normal))
                              (js/alert status)))
                      )} "Send"]
   [:br][:br]
   [:div {:style {:cursor "pointer"}
          :on-click (fn [event]
                      (async/go
                        (let [{:keys [status body]} (async/<! (http/get (str server-url "/api-removepost")
                                                                        {:query-params {:id id :pass @pass}}))
                              result (js->clj (.parse js/JSON body) :keywordize-keys true)
                              status (result :result)]
                          (if (= status "OK")
                            (reset! page-state :normal))
                          (js/alert status)))
                      )} "!!!Remove Post!!!"]
   ]))


(defn page []
  "returns a page component"
  (fn []
    [:div {:key "page" :class "page"}
     (cond
       (= @page-state :newpost)
       [newpost]
       :else
       [:div {:id "pagecompbody"}      
        (doall (map (fn [index] ^{:key (str "card" index)} [pagecard index]) (range 4) ))])

     [:div {:key "logo":class "verticaltext logo"}
      [:div {:class "logobutton"} "milgra.com"]]
     (if @mode-admin
       [:div {:style {:position "absolute" :right "-110px"}}
        [:input {:style {:width "100px"}
                 :on-change #(reset! pass (-> % .-target .-value))
                 :type "password" }]
        [:div {:class "adminbutton"
               :on-click (fn [e]
                           (reset! posttoedit nil)
                           (reset! page-state :newpost))} "add post"]
        [:div {:class "adminbutton"
               :on-click (fn [e] (reset! page-state :normal))} "return"]])]))


(defn parse-params
  "Parse URL parameters into a hashmap"
  [url]
  (let [param-strs (->
                    url
                    (clojure.string/split #"\?")
                    last
                    (clojure.string/split #"\&"))]
    (into {} (for [[k v] (map #(clojure.string/split % #"=") param-strs)]
               [(keyword k) v]))))

(defn parse-routes!
  [url]
  (let [path (.getPath (.parse Uri url))
        [_ mainroute subroute ] (clojure.string/split path #"/" )
        ]
    (if (= mainroute "admin")
      (reset! mode-admin true))
    (if (= mainroute "post")
      (reset! selected-post subroute))))


(defn start []
  (reset! selected-page "blog")
  (parse-routes! (.-location js/window))
  (reagent/render-component [page] (. js/document (getElementById "app"))))


(events/listen js/document "click"
               (fn [e]
                 (let [path (.getPath (.parse Uri (.-href (.-target e))))
                       [_ mainroute subroute ] (clojure.string/split path #"/" )]

                   (if (= mainroute "post")
                     (do
                       (. e preventDefault)
                       (get-post subroute)))

                   (when path
                     (. js/history pushState "" "" (.-href (.-target e)))))))


(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))


(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))

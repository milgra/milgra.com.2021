(ns milgracom.view.content
  (:require
    [markdown-to-hiccup.core :as m]
    [milgracom.events :as events]
    [milgracom.subs :as subs]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reanimated.core :as anim]))


(defn impressum
  []
  [:div {:class "impressum"}
   "www.milgra.com by Milan Toth | Powered by Clojure and Datomic."])


(defn comments
  [{id :id :as post}]
  (let [showcomments (r/atom false)
        showeditor (r/atom false)
        comments (rf/subscribe [::subs/blog-comments])
        riddle (rf/subscribe [::subs/blog-comment-riddle])
        admin-mode (rf/subscribe [::subs/admin-mode])
        nick (r/atom nil)
        text (r/atom nil)
        code (r/atom nil)]
    (fn []
      [:div
       [:div {:class "comments"}
        [:div {:style {:padding-right "20px" :cursor "pointer"}
               :on-click (fn []
                           (rf/dispatch [::events/get-comments id])
                           (swap! showcomments not))}
         "Show comments"]
        "|"
        [:div {:style {:padding-left "20px" :cursor "pointer"}
               :on-click (fn []
                           (rf/dispatch [::events/gen-riddle])
                           (swap! showeditor not))}
         " Post comment"]
        (if @admin-mode
          [:div {:style {:padding-left "20px" :cursor "pointer"}
                 :on-click (fn []
                             (rf/dispatch [::events/edit-post post]))}
           "[Edit post]"])]

       (if @showeditor
         [:div {:style {:text-align "center"}}
          [:br]
          [:div {:style {:width "100%"}} "Nick"]
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
                             (swap! showeditor not)
                             (rf/dispatch [::events/add-comment id @text @nick @code]))}
           "Send Comment"]])

       (if @showcomments
         (if (empty? @comments)
           [:div "No comments"]
           [:div
            (map (fn [comment]
                   [:div {:key (:id comment)}
                    [:h3
                     (comment :nick)
                     "|"
                     (clojure.string/replace (comment :date) #"T" " ")]
                    [:div (comment :content)]
                 ;; (if @mode-admin
                   [:div {:style {:cursor "pointer"}
                          :class "showcommentbtn"
                          ;;:on-click (fn [event] (delete-comment (comment :id) @pass ))
                          } "Delete comment"]
                 [:hr]])
                 @comments)]))])))


(defn post
  [post]
  (when-let [{:keys [title content id date tags]} post]
    (if content
      [:div
       {:id "a-content"
        :class "content"
        :style {:z-index "inherit"}}
       [:h1 [:a {:href (str "/post/" id)} title]]
       [:h2 (str (clojure.string/replace date #"T" " ") " / " (clojure.string/join "," tags))]
       [:br]
          (m/component (m/md->hiccup content (:encode? true)))
          [:br]
          [comments post]
          [:br]
          [:hr]]

      [:div
       {:id "a-content"
        :class "content"
        :style {:z-index "inherit"}}
       [:h1 [:a {:href (str "/post/" id)} title]]
       [:h2 (str (clojure.string/replace date #"T" " "))]])))


(defn content-post
  []
  (let [blog-post (rf/subscribe [::subs/blog-post])]
    [:div
     [post @blog-post]
     [impressum]]))


(defn content-post-list
  []
  (let [blog-posts (rf/subscribe [::subs/blog-posts])]
    (if @blog-posts
      [:div
       (map (fn [apost]
              [:div
               {:key (:id apost)}
               [post apost]])
            @blog-posts)
       [impressum]])))


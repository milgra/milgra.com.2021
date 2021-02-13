(ns milgracom.view.editor
  (:require
    [markdown-to-hiccup.core :as m]
    [milgracom.events :as events]
    [milgracom.subs :as subs]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reanimated.core :as anim]))


(defn editor
  []
  (let [post (rf/subscribe [::subs/admin-post])
        title (r/atom (if @post (@post :title) "title"))
        date (r/atom (if @post (@post :date) (subs (pr-str (js/Date.)) 7 26)))
        type (r/atom (if @post (@post :type) "type"))
        tags (r/atom (if @post (clojure.string/join "," (@post :tags)) "tags,tags"))
        content (r/atom (if @post (@post :content) "content"))
        id (if @post (@post :id) 0)]
    (fn []
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
              :on-click #(let [newpost {:title @title
                                        :date @date
                                        :tags @tags
                                        :type @type
                                        :content @content
                                        :id id}]
                           (if @post
                             (rf/dispatch [::events/update-post newpost])
                             (rf/dispatch [::events/create-post newpost])))}
        "Send"]
       [:br] [:br]
       [:div {:style {:cursor "pointer"}
              :on-click #(rf/dispatch [::events/delete-post {:id id}])}
        "!!!Remove Post!!!"]])))

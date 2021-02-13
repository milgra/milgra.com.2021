(ns milgracom.view.menus
  (:require
    [markdown-to-hiccup.core :as m]
    [milgracom.events :as events]
    [milgracom.subs :as subs]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reanimated.core :as anim]))


(defonce monthnames ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"])


(defn date-menu-btn
  [[index [year month]] type]
  (let [pos (r/atom (/ (.-innerWidth js/window) -2))
        pos-spring (anim/spring pos {:mass 5.0 :stiffness 0.5 :damping 3.0})
        newpos 30]
    (fn []
      [:div
       [anim/timeline
        (* 50 index)
        #(reset! pos newpos)]
       [:div
        {:id "a-leftmenubtn"
         :class "menubtn leftmenubtn"
         :style {:transform (str "translate(" @pos-spring "px)")
                 :background (if (= (mod index 2) 0) "#dff6df" "#d5f3d5")}
         :on-click (fn [e]
                     (reset! pos 40)
                     (rf/dispatch [::events/get-posts-by-date [year month type]]))}
        (str (nth monthnames (dec month)) " " year)]])))


(defn post-menu-btn
  [[index post]]
  (let [pos (r/atom (/ (.-innerWidth js/window) -2))
        pos-spring (anim/spring pos {:mass 5.0 :stiffness 0.5 :damping 3.0})
        newpos 30]
    (fn []
      [:div
       [anim/timeline
        (* 50 index)
        #(reset! pos newpos)]
       [:div
        {:id "a-leftmenubtn"
         :class "menubtn leftmenubtn"
         :style {:transform (str "translate(" @pos-spring "px)")
                 :background (if (= (mod index 2) 0) "#dff6df" "#d5f3d5")}
         :on-click (fn [e]
                     (reset! pos 40)
                     (rf/dispatch [::events/set-blog-post post]))}
        (:title post)]])))


(defn tag-menu-btn
  [[index label]]
  (let [pos (r/atom (/ (.-innerWidth js/window) 2))
        pos-spring (anim/spring pos {:mass 5.0 :stiffness 0.5 :damping 3.0})
        newpos 30]
    (fn []
      [:div
       [anim/timeline
        (* 40 index)
        #(reset! pos newpos)]
       [:div
        {:id "rightmennubtn"
         :class "menubtn rightmenubtn"
         :style {:transform (str "translate(" @pos-spring "px)")
                 :background (if (= (mod index 2) 0) "#dff6df" "#d5f3d5")}
         :on-click (fn [e]
                     (reset! pos 40)
                     (rf/dispatch [::events/get-posts-by-tag label]))}
        label]
       [:div {:id "rightmenubottom"
              :style {:height "-1px"}}]])))


(defn date-menu
  [type]
  (let [blog-months (rf/subscribe [::subs/blog-months])]
    [:div
     [anim/timeline
      [:div "LOADING DATES"]
      10 ; load months from server
      #(rf/dispatch [::events/get-months "blog"])
      1000 ; load first month entry from server
      #(when-let [[year month] (first @blog-months)]
         (rf/dispatch [::events/get-posts-by-date [year month type]]))
      100
      [:div]]
     [:div
      {:id "leftmenu"
       :class "leftmenu"}
      [:div
       (map
         (fn [item] ^{:key item} [date-menu-btn item type])
         (map-indexed vector @blog-months))]]]))


(defn posts-menu
  [type]
  (let [blog-posts (rf/subscribe [::subs/blog-posts])]
    [:div
     [anim/timeline
      [:div "LOADING POSTS"]
      10 ; load months from server
      #(rf/dispatch [::events/get-posts type])
      100
      [:div]]
     [:div
      {:id "leftmenu"
       :class "leftmenu"}
      [:div
       (map
         (fn [item]
           ^{:key item}
           [post-menu-btn item])
         (map-indexed vector @blog-posts))]]]))


(defn tags-menu
  []
  (let [blog-tags (rf/subscribe [::subs/blog-tags])]
    [:div
     {:id "rightmenu"
      :class "rightmenu"}
     [:div
      (map
        (fn [item] ^{:key item} [tag-menu-btn item])
        (map-indexed vector @blog-tags))]]))

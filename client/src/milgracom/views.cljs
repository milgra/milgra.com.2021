(ns milgracom.views
  (:require
    [markdown-to-hiccup.core :as m]
    [milgracom.events :as events]
    [milgracom.routes :as routes]
    [milgracom.subs :as subs]
    [milgracom.view.content :as view-content]
    [milgracom.view.editor :as view-editor]
    [milgracom.view.menus :as view-menus]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reanimated.core :as anim]))


(defonce fixposes [0 50 100 150])
(defonce fixsizes [50 50 50 750])


(defn page-card
  [index]
  (let [pos (r/atom 0)
        wth (r/atom 0)
        pos-spring (anim/spring pos {:mass 10.0 :stiffness 0.5 :damping 2.0})
        wth-spring (anim/spring wth {:mass 3.0 :stiffness 0.5 :damping 2.0})]

    (reset! pos (nth fixposes index))
    (reset! wth (nth fixsizes index))

    (fn inner-page-card
      []
      (let [items (rf/subscribe [::subs/menuitems])
            {:keys [label place color]} (nth @items index)]

        (reset! pos (nth fixposes place))
        (reset! wth (nth fixsizes place))

        [:div {:key (str "page-card" label)}
         [:div
          {:class "card"
           :style {:background color
                   :transform (str "translate(" @pos-spring "px)")
                   :z-index place
                   :width @wth-spring}}
          ; button
          [:div {:key "cardbutton"
                 :class "verticaltext cardbutton"
                 :on-click (fn []
                             (routes/add-to-history (str "/" label) label)
                             (rf/dispatch [::events/select-page-card label]))}
           label]
          ; content
          (when (= place 3)
            [:div
             (if (= label "blog") [view-menus/date-menu label] [view-menus/posts-menu label])
             (if (= label "blog") [view-menus/tags-menu])
             (if (= label "blog") [view-content/content-post-list] [view-content/content-post label])])]]))))


(defn main-panel
  []
  (let [view-mode (rf/subscribe [::subs/view-mode])
        admin-mode (rf/subscribe [::subs/admin-mode])
        remote-error (rf/subscribe [::subs/remote-error])]

    [:div {:key "page" :class "page"}
     (if @remote-error [:div @remote-error])

     [:div
      (case @view-mode
        "blog" (map
                 (fn [index] ^{:key (str "card" index)} [page-card index])
                 (range 4))
        "edit" [view-editor/editor])]

     [:div {:key "logo" :class "verticaltext logo"}
      [:div {:class "logobutton"} "milgra.com"]]

     (if @admin-mode
       [:div {:class "adminpanel"}
        [:input {:style {:width "100px"}
                 :on-change #(rf/dispatch [::events/set-pass (-> % .-target .-value)])
                 :type "password"}]
        [:div {:class "adminbutton"
               :on-click #(do
                            (rf/dispatch [::events/edit-post nil])
                            (rf/dispatch [::events/set-view-mode "edit"]))}
         "add post"]
        [:div {:class "adminbutton"
               :on-click #(rf/dispatch [::events/set-view-mode "blog"])}
         "return"]])]))

(ns milgracom.routes
  (:require
    [goog.events :as gevents]
    [goog.history.EventType :as EventType]
    [milgracom.events :as events]
    [re-frame.core :as rf]
    [secretary.core :as secretary :include-macros true])
  (:import
    goog.Uri
    goog.history.Html5History))


(def history (atom nil))


(secretary/defroute root-path "/" []
                    (rf/dispatch [::events/select-page-card "blog"]))


(secretary/defroute post-path "/post/:id" [id]
                    (rf/dispatch [::events/get-post id]))


(secretary/defroute proto-path "/proto" []
                    (rf/dispatch [::events/select-page-card "proto"]))


(secretary/defroute app-path "/app" []
                    (rf/dispatch [::events/select-page-card "app"]))


(secretary/defroute game-path "/game" []
                    (rf/dispatch [::events/select-page-card "game"]))


(secretary/defroute blog-path "/blog" []
                    (rf/dispatch [::events/select-page-card "blog"]))


(secretary/defroute admin-path "/admin" []
                    (rf/dispatch [::events/toggle-admin]))


(defn add-to-history
  [path title]
  (. @history (setToken path title)))


(defn hook-browser-navigation!
  []
  (let [hst (doto (Html5History.)
              (gevents/listen EventType/NAVIGATE (fn [event] (secretary/dispatch! (.-token event))))
              (.setUseFragment false)
              (.setPathPrefix "")
              (.setEnabled true))]

    (reset! history hst)
    (gevents/listen js/document "click"
                    (fn [e]
                      (let [path (.getPath (.parse Uri (.-href (.-target e))))
                            title (.-title (.-target e))]
                        (if (> (count path) 0)
                          (let [route (secretary/locate-route path)]
                            (if route (. e preventDefault)) ; don't follow internal anchors
                            (. hst (setToken path title)))))))));

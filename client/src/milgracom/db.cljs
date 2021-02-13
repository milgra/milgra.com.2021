(ns milgracom.db)


(def default-db
  {:menuitems [{:color "#dff6df"
                :place 0
                :index 0
                :label "app"}
               {:color "#d5f3d5"
                :place 1
                :index 1
                :label "game"}
               {:color "#dff6df"
                :place 2
                :index 2
                :label "proto"}
               {:color "#d5f3d5"
                :place 3
                :index 3
                :label "blog"}]
   :view-mode "blog"

   :blog-months []
   :blog-posts []
   :blog-tags []
   :blog-post nil
   :blog-comments []
   :blog-comment-riddle ""

   :admin-pass ""
   :admin-mode false
   :admin-post nil

   :remote-error nil})

(ns milgracom.events
  (:require
    [ajax.core :as ajax]
    [day8.re-frame.http-fx]
    [goog.Uri.QueryData :as query]
    [goog.structs :as structs]
    [milgracom.db :as db]
    [re-frame.core :as rf]))


(defonce server-url (if js/goog.DEBUG "http://localhost:3000" "http://116.203.87.141"))


(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))


(rf/reg-event-db
  ::set-pass ; comes from password input field in admin mode
  (fn [db [_ pass]]
    (assoc db :admin-pass pass)))


(rf/reg-event-db
  ::toggle-admin ; comes from router when url ends with /admin
  (fn [db [_]]
    (update db :admin-mode not)))


(rf/reg-event-db
  ::edit-post ; comes from the edit post button in view/content/comments
  (fn [db [_ post]]
    (assoc db
           :admin-post post
           :view-mode "edit")))


(rf/reg-event-db
  ::set-view-mode ; comes from main panel in views
  (fn [db [_ mode]]
    (assoc db :view-mode mode)))


(rf/reg-event-db
  ::select-page-card
  (fn [db [_ label]]
    (let [sorted (sort-by :place (:menuitems db))
          active (last sorted)]
      (if (not= (:label active) label)
        (let [newitems (->> (concat
                              (filter #(not= (% :label) label) sorted)
                              (filter #(= (% :label) label) sorted)) ; put selected item at the end
                            (map-indexed (fn [idx itm] (assoc itm :place idx))) ; set new places
                            (sort-by :index))] ; rearrange by index
          (-> db
              (assoc :blog-tags [])
              (assoc :blog-post nil)
              (assoc :blog-posts [])
              (assoc :blog-months [])
              (assoc :menuitems newitems)))))))


(rf/reg-event-fx
  ::get-posts
  (fn [{db :db} [_ type]]
    {:http-xhrio {:method          :get
                  :uri             (str server-url "/api-getposts")
                  :params {:type type}
                  :format         (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-posts-result-success]
                  :on-failure      [::remote-result-failure]}
     :db (assoc db :loading? true)}))


(rf/reg-event-fx
  ::get-posts-by-date
  (fn [{db :db} [_ [year month type]]]
    {:http-xhrio {:method          :get
                  :uri             (str server-url "/api-getpostsbydate")
                  :params {:year year :month month :type type}
                  :format         (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-posts-by-date-result-success]
                  :on-failure      [::remote-result-failure]}
     :db (assoc db :loading? true)}))


(rf/reg-event-fx
  ::get-posts-by-tag
  (fn [{db :db} [_ tag]]
    {:http-xhrio {:method          :get
                  :uri             (str server-url "/api-getpostsbytag")
                  :params {:tag tag}
                  :format         (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-posts-by-tag-result-success]
                  :on-failure      [::remote-result-failure]}
     :db (assoc db :loading? true)}))


(rf/reg-event-fx
  ::get-months
  (fn [{db :db} [_ type]]
    {:http-xhrio {:method          :get
                  :uri             (str server-url "/api-getmonths")
                  :params {:type type}
                  :format         (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-months-result-success]
                  :on-failure      [::remote-result-failure]}
     :db (assoc db :loading? true)}))


(rf/reg-event-fx
  ::get-post
  (fn [{db :db} [_ id]]
    {:http-xhrio {:method          :get
                  :uri             (str server-url "/api-getpost")
                  :params {:id id}
                  :format         (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-post-result-success]
                  :on-failure      [::remote-result-failure]}
     :db (assoc db :loading? true)}))


(rf/reg-event-fx
  ::get-comments
  (fn [{db :db} [_ id]]
    {:http-xhrio {:method          :get
                  :uri             (str server-url "/api-getcomments")
                  :params {:postid id}
                  :format         (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-comments-result-success]
                  :on-failure      [::remote-result-failure]}
     :db (assoc db :loading? true)}))


(rf/reg-event-fx
  ::add-comment
  (fn [{db :db} [_ postid text nick code]]
    {:http-xhrio {:method          :get
                  :uri             (str server-url "/api-addcomment")
                  :params {:postid postid
                           :text text
                           :nick nick
                           :code code}
                  :format         (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::add-comment-result-success]
                  :on-failure      [::remote-result-failure]}
     :db (assoc db
                :loading? true
                :last-comment-postid postid)}))


(rf/reg-event-fx
  ::update-post
  (fn [{db :db} [_ {:keys [title date tags type content id] :as data}]]
    (let [pass (:admin-pass db)]
      {:http-xhrio {:method          :post
                    :uri             (str server-url "/api-updatepost")
                    :body            (doto (js/FormData.)
                                       (.append "pass" pass)
                                       (.append "title" title)
                                       (.append "date" date)
                                       (.append "tags" tags)
                                       (.append "type" type)
                                       (.append "content" content)
                                       (.append "id" id))
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::update-post-result-success]
                    :on-failure      [::remote-result-failure]}
       :db (assoc db
                  :loading? true)})))


(rf/reg-event-fx
  ::create-post
  (fn [{db :db} [_ {:keys [title date tags type content] :as data}]]
    (let [pass (:admin-pass db)]
      {:http-xhrio {:method          :post
                    :uri             (str server-url "/api-addpost")
                    :body            (doto (js/FormData.)
                                       (.append "pass" pass)
                                       (.append "title" title)
                                       (.append "date" date)
                                       (.append "tags" tags)
                                       (.append "type" type)
                                       (.append "content" content))
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::update-post-result-success]
                    :on-failure      [::remote-result-failure]}
       :db (assoc db
                  :loading? true)})))


(rf/reg-event-fx
  ::delete-post
  (fn [{db :db} [_ post]]
    (let [pass (:admin-pass db)
          data (assoc post :pass pass)]
      {:http-xhrio {:method          :get
                    :uri             (str server-url "/api-removepost")
                    :params data
                    :format         (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::delete-post-result-success]
                    :on-failure      [::remote-result-failure]}
       :db (assoc db
                  :loading? true)})))


(rf/reg-event-fx
  ::gen-riddle
  (fn [{db :db} [_ postid text nick code]]
    {:http-xhrio {:method          :get
                  :uri             (str server-url "/api-genriddle")
                  :format         (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::gen-riddle-result-success]
                  :on-failure      [::remote-result-failure]}
     :db (assoc db :loading? true)}))


(rf/reg-event-db
  ::set-blog-post
  (fn [db [_ post]]
    (assoc db :blog-post post)))


(rf/reg-event-db
  ::get-posts-result-success
  (fn [db [_ {:keys [posts tags]}]]
    (-> db
        (assoc :blog-posts posts)
        (assoc :blog-tags tags)
        (assoc :blog-post (first posts)))))


(rf/reg-event-db
  ::get-posts-by-date-result-success
  (fn [db [_ {:keys [posts tags]}]]
    (-> db
        (assoc :blog-posts posts))))


(rf/reg-event-db
  ::get-posts-by-tag-result-success
  (fn [db [_ {:keys [posts tags]}]]
    (-> db
        (assoc :blog-posts posts))))


(rf/reg-event-db
  ::get-months-result-success
  (fn [db [_ {:keys [months tags]}]]
    (-> db
        (assoc :blog-months months)
        (assoc :blog-tags tags))))


(rf/reg-event-db
  ::get-post-result-success
  (fn [db [_ {:keys [posts tags]}]]
    (-> db
        (assoc :blog-posts posts))))


(rf/reg-event-db
  ::get-comments-result-success
  (fn [db [_ {:keys [comments]}]]
    (-> db
        (assoc :blog-comments comments))))


(rf/reg-event-db
  ::add-comment-result-success
  (fn [db [_ {:keys [result]}]]
    (let [postid (:last-comment-postid db)]
      (rf/dispatch [::get-comments postid])
      db)))


(rf/reg-event-db
  ::gen-riddle-result-success
  (fn [db [_ {:keys [question]}]]
    (assoc db :blog-comment-riddle question)))


(rf/reg-event-db
  ::update-post-result-success
  (fn [db [_ {:keys [result]}]]
    (if (= result "OK")
      (assoc db :view-mode "blog")
      (js/alert result)))) ; show invalid pass 


(rf/reg-event-db
  ::delete-post-result-success
  (fn [db [_ {:keys [result]}]]
    (if (= result "OK")
      (assoc db :view-mode "blog")
      (js/alert result)))) ; show invalid pass


(rf/reg-event-db
  ::remote-result-failure
  (fn [db [_ response]]
    (assoc db :remote-error (str "server error" (:debug-message response)))))

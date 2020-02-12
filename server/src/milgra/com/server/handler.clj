(ns milgra.com.server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [clj-time.format :as time]
            [datomic.api :as d]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(def uri "datomic:dev://localhost:4334/milgracom")

(def blog-schema [{:db/ident :blog/title
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/doc "The title of the post"}
                  
                  {:db/ident :blog/date
                   :db/valueType :db.type/instant
                   :db/cardinality :db.cardinality/one
                   :db/doc "The date of the post"}
                  
                  {:db/ident :blog/content
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/doc "The conent of the post in html"}])

(def comment-schema [{:db/ident :comment/conent
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/doc "The content of the comment "}

                     {:db/ident :comment/email
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/doc "The email of the commenter"}

                     {:db/ident :comment/date
                      :db/valueType :db.type/instant
                      :db/cardinality :db.cardinality/one
                      :db/doc "The date of the comment"}])

(def first-posts [{:blog/title "Első post"
                   :blog/date (new java.util.Date 115 11 3)
                   :blog/content "<h>Ehun egy html.<br>Ehun meg egy</h>"}
                  {:blog/title "Második post"
                   :blog/date (new java.util.Date 115 11 6)
                   :blog/content "<h>Másdoik második. Ehun egy html.<br>Ehun meg egy</h>"}
                  {:blog/title "Második post"
                   :blog/date (new java.util.Date 117 3 22)
                   :blog/content "<h>Másdoik második. Ehun egy html.<br>Ehun meg egy</h>"}
                  {:blog/title "Harmadik post"
                   :blog/date (new java.util.Date 116 6 6)
                   :blog/content "<h>Harmadik harmadik. Ehun egy html.<br>Ehun meg egy</h>"}])

(def mapfromdb '[:find ?artist-name ?release-name
                 :keys artist release
                 :where [?release :release/name ?release-name]
                 [?release :release/artists ?artist]
                 [?artist :artist/name ?artist-name]])

(def all-posts-q '[:find ?e :where [?e :blog/title]])

(def all-posts-all-data-q '[:find ?e ?title ?date ?content
                            :where [?e :blog/title ?title] 
                            [?e :blog/date ?date]
                            [?e :blog/content ?content]])

(defn setup []
  (let [uri "datomic:dev://localhost:4334/milgracom"
        succ (d/create-database uri)]
    (if succ
      (let [conn (d/connect uri)
            db (d/db conn)]
        (let [resp @(d/transact conn blog-schema)]
          (println "blog schema insert resp" resp))
        (let [resp @(d/transact conn comment-schema)]
          (println "comment schema insert resp" resp))
        (let [resp @(d/transact conn first-posts)]
          (println "data insert resp" resp))))))

;; get all months for posts in database

(def all-posts-date-q '[:find ?date
                        :where [?e :blog/date ?date]])

(defn get-blog-months []
  (let [conn (d/connect uri)
        db (d/db conn)
        dates (d/q all-posts-date-q db)]
    (reverse
     (sort
      (set
       (map (fn [item]
              (let [date (nth item 0)]
                (str (+ 1900 (.getYear date)) ":" (.getMonth date))
                )) dates))))))

(get-blog-months)

;; get all posts for year and month

(def posts-between-dates
  '[:find ?e ?title ?date ?content
    :in $ ?start ?end
    :where
    [?e :blog/title ?title]
    [?e :blog/date ?date]
    [?e :blog/content ?content]
    [(> ?date ?start)]
    [(< ?date ?end)]
    ])

(defn format-date
  [date-str]
  (time/parse (time/formatter "yyyy-MM-dd") date-str))

(defn get-blog-posts []
  (let [uri "datomic:dev://localhost:4334/milgracom"
        conn (d/connect uri)
        db (d/db conn)
        start #inst "2015-12-01T12:00:00"
        end #inst "2016-01-01T12:00:00"
        posts (d/q posts-between-dates db start end)]
    (println "posts" posts)))

(get-blog-posts)

(defn testrequest [ ]
  (let [uri "datomic:dev://localhost:4334/milgracom"
        conn (d/connect uri)
        db (d/db conn)
        posts (d/q all-posts-all-data-q db)]
    
    (println "posts" posts)
    
    (apply str (first posts))))

(testrequest)

(defn comments-of-user-about-post
  "Given a user Entity and a post Entity, returns the user's comments about that post as a seq of Entities."
  [user post]
  (let [db (d/entity-db user)]
    (->> (d/q '[:find [?comment ...] :in $ ?user ?post :where
                [?comment :comment/post ?post]
                [?comment :comment/user ?user]]
           db (:db/id user) (:db/id post))
     (map #(d/entity db %))
     )))

(defn find-user-by-id [db userId])
(defn find-post-by-id [db postId])
(defn comments-of-user-about-post [user post])
(defn cl-comment [])

(defroutes app-routes
  (GET "/" [] (testrequest))
  (GET "/months" [] (json/write-str {:result (get-blog-months)}))
  (GET "/posts" [month] (println "posts, args: " month))
  (GET "/postos/:postId/comments-of-user/:userId"
       [postId userId :as req]
       (let [db (:db req)
             ;; resources identification
             user (find-user-by-id db userId)
             post (find-post-by-id db postId)]
         {:body (->> (comments-of-user-about-post user post) ;; domain logic
                     (map cl-comment) ;; result layout
                     )}))
  (route/not-found "Not Found"))

(setup)

(def app
  (-> app-routes
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get]
                 :access-control-allow-credentials "true")
      (wrap-defaults site-defaults)))

(ns milgra.com.server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [datomic.api :as d]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

>
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
                   :blog/date (new java.util.Date)
                   :blog/content "<h>Ehun egy html.<br>Ehun meg egy</h>"}
                  {:blog/title "Második post"
                   :blog/date (new java.util.Date)
                   :blog/content "<h>Másdoik második. Ehun egy html.<br>Ehun meg egy</h>"}
                  {:blog/title "Harmadik post"
                   :blog/date (new java.util.Date)
                   :blog/content "<h>Harmadik harmadik. Ehun egy html.<br>Ehun meg egy</h>"}])

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


(defn testrequest [ ]
  (let [uri "datomic:dev://localhost:4334/milgracom"
        conn (d/connect uri)
        db (d/db conn)
        posts (d/q all-posts-all-data-q db)]
  
  (println "uri" uri)
  (println "conn" conn)
  (println "db" db)
  (println "posts" posts)

  (apply str (first posts))))

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
  (GET "/posts/:postId/comments-of-user/:userId"
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

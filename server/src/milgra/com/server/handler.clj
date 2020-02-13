(ns milgra.com.server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [milgra.com.server.database :as db]
            [clojure.data.json :as json]
            [clj-time.format :as time]
            [datomic.api :as d]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


(def uri "datomic:dev://localhost:4334/milgracom")


(defn setup
  "create schema and fill up with test data"
  []
  (if (d/create-database uri)
    (let [conn (d/connect uri)
          db (d/db conn)]
      (let [resp @(d/transact conn db/blog-schema)]
        (println "blog schema insert resp" resp))
      (let [resp @(d/transact conn db/comment-schema)]
        (println "comment schema insert resp" resp))
      (let [resp @(d/transact conn db/first-posts)]
        (println "data insert resp" resp)))))


(defn insert-comment []
  (let [conn (d/connect uri)
        db (d/db conn)]
    (let [resp @(d/transact conn db/first-comment)]
      (println "blog schema insert resp" resp))))


(defn get-all-posts
  "get all posts"
  []
  (let [conn (d/connect uri)
        db (d/db conn)
        posts (d/q db/all-posts-all-data-q db)]
    posts))


(defn get-post-months
  "get all months where posts exist"
  []
  (let [conn (d/connect uri)
        db (d/db conn)
        dates (d/q db/all-post-months-q db)]
    (reverse
     (sort
      (set
       (map (fn [item]
              (let [date (nth item 1)]
                [(+ 1900 (.getYear date)) (inc (.getMonth date))]))
            dates))))))


(defn get-posts-for-month
  "get all posts for given year and month"
  [year month]
  (let [conn (d/connect uri)
        db (d/db conn)
        endmonth (if (= month 12) 1 (+ month 1))
        endyear (if (= month 12) (+ year 1) year)
        start (clojure.instant/read-instant-date (format "%d-%02d-01T00:00:00" year month))
        end (clojure.instant/read-instant-date  (format "%d-%02d-01T00:00:00" endyear endmonth))
        posts (d/q db/posts-between-dates db start end)]
    posts))

;;(get-posts-for-month 2017 7)


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


(def app
  (-> app-routes
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get]
                 :access-control-allow-credentials "true")
      (wrap-defaults site-defaults)))

;; init database on start
(setup)

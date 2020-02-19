(ns milgracom.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [milgracom.database :as db]
            [clojure.data.json :as json]
            [datomic.api :as d]
            [ring.util.response :as resp]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


(def uri "datomic:dev://localhost:4334/milgracom")


(defn get-all-posts
  "get all posts"
  []
  (let [conn (d/connect uri)
        db (d/db conn)
        posts (d/q db/all-posts-all-data-q db)]
    posts))


(defn setup
  "create schema"
  []
  (let [succ (d/create-database uri)] 
    (if succ 
      (let [conn (d/connect uri)
            db (d/db conn)]
        (let [resp (d/transact conn db/blog-schema)]
          (println "blog schema insert resp" resp))
        (let [resp (d/transact conn db/comment-schema)]
          (println "comment schema insert resp" resp))
        (let [resp (d/transact conn db/project-schema)]
          (println "project schema insert resp" resp)))
      (println "db exists"))))

(defn delete []
  (let [succ (d/delete-database uri)]
    (println "delete" succ)
    ))


(defn fillup
  "fill up db with dev data"
  []
  (let [conn (d/connect uri)
        db (d/db conn)]
    (let [resp (d/transact conn db/first-projects)]
      (println "project insert resp" resp))
    (let [resp (d/transact conn db/first-posts)]
      (println "post insert resp" resp))
    (let [postid ((first (get-all-posts)) :e)
          comment (assoc db/first-comment :comment/postid postid)
          resp (d/transact conn [comment])]
      (println "comment insert resp" resp))))
  

(defn get-all-comments
  "get all comments"
  []
  (let [conn (d/connect uri)
        db (d/db conn)
        posts (d/q db/all-comments-all-data-q db)]
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
    (map (fn [{date :date :as val}]
           (assoc val :date (.format (java.text.SimpleDateFormat. "MM/dd/yyyy") date)))
           posts)))

;;(get-posts-for-month 2018 4)

(defn get-comments-for-post
  "returns comments for give post id"
  [postid]
  (let [conn (d/connect uri)
        db (d/db conn)
        comments (d/q db/comments-for-post-q db postid)]
    comments))

;;(get-post-comments 17592186045419)

(defn get-projects
  "return all projects with given type"
  [type]
  (let [conn (d/connect uri)
        db (d/db conn)
        projects (d/q db/projects-for-type-q type)]
    projects))


(defn add-post [pass title date content]
  (let [data [{:blog/title title ;;"Első post"
               :blog/date date ;; #inst "2015-12-05T00:00:00" 
               :blog/content content ;; "<h>Ehun egy html.<br>Ehun meg egy</h>"}]
               }]
        conn (d/connect uri)
        db (d/db conn)
        resp (d/transact conn data)]
      (println "post insert resp" resp)))

(defn add-comment [pass postid date email content]
  (let [data [{:comment/postid postid
               :comment/content content ;; "Faszasag!!!"
               :comment/email email ;; "milgra@milgra.com"
               :comment/date date ;; #inst "2018-07-30T00:00:00"
               }]
        conn (d/connect uri)
        db (d/db conn)
        resp (d/transact conn data)]
      (println "comment insert resp" resp)))

(defn add-project [pass title type content]
  (let [data [{:project/title title ;; "Termite 3D"
               :project/type type ;; "game" 
               :project/content content ;; "Egy kurva jo jatek"
               }]
        conn (d/connect uri)
        db (d/db conn)
        resp (d/transact conn data)]
      (println "project insert resp" resp)))

(defroutes app-routes
  (GET "/" [] "BLANK")
  (GET "/months" [] (json/write-str {:result (get-post-months)}))
  (GET "/posts" [year month] (json/write-str {:result (get-posts-for-month (Integer/parseInt year) (Integer/parseInt month))}))
  (GET "/comments" [postid] (json/write-str {:result (get-comments-for-post postid)}))
  (GET "/projects" [type] (json/write-str {:result (get-projects type)}))
  (route/resources "/")
  (route/not-found "Not Found"))


(def app
  (-> app-routes
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get]
                 :access-control-allow-credentials "true")
      (wrap-defaults site-defaults)))

;; init database on start
(setup)

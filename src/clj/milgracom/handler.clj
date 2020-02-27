(ns milgracom.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [milgracom.database :as db]
            [clojure.data.json :as json]
            [datomic.api :as d]
            [crypto.password.pbkdf2 :as password]
            [ring.util.response :as resp]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


(def uri "datomic:dev://localhost:4334/milgracom")
(def epass "AYag$X5r8CmDjJOQ=$uDGDAgnDrf3Gju5pPq9bTWWpsMc=")

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
    (println "delete" succ)))


(defn fillup
  "fill up db with dev data"
  []
  (let [conn (d/connect uri)
        db (d/db conn)]
    (let [resp (d/transact conn db/first-projects)]
      (println "project insert resp" resp))
    (let [resp (d/transact conn db/first-posts)]
      (println "post insert resp" resp))
    (let [postid ((first (first (get-all-posts))) :db/id)
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


(defn get-post-tags
  "get all tags in posts"
  []
  (let [conn (d/connect uri)
        db (d/db conn)
        tags (d/q db/all-post-tags-q db)]
    (reduce (fn [res item] (into res ((first item) :blog/tags)) ) #{} tags)))


(defn get-project-tags
  "get all tags in posts"
  [type]
  (let [conn (d/connect uri)
        db (d/db conn)
        tags (d/q db/all-project-tags-q db type)]
    (reduce (fn [res item] (into res ((first item) :project/tags)) ) #{} tags)))


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
    (map (fn [[{date :blog/date :as val}]]
           (assoc val :blog/date (.format (java.text.SimpleDateFormat. "yyyy-dd-MM") date)))
         posts)))

;;(get-posts-for-month 2018 4)

(defn get-post-comments
  "returns comments for give post id"
  [postid]
  (println "get-post-comments" postid)
  (let [conn (d/connect uri)
        db (d/db conn)
        comments (d/q db/comments-for-post-q db postid)]
    (println "res" comments)
    (map (fn [[{date :comment/date :as val}]]
           (assoc val :comment/date (.format (java.text.SimpleDateFormat. "yyyy-dd-MM") date)))
    comments)))

;;(get-post-comments 17592186045425)

(defn get-projects
  "return all projects with given type"
  [type]
  (println "type" type)
  (let [conn (d/connect uri)
        db (d/db conn)
        projects (d/q db/projects-for-type-q db type)]
    projects))

(get-projects "game")

(defn add-post [pass title date tags content]
  (println "add post" pass title date tags content)
  (if (password/check pass epass)
    (let [data [{:blog/title title ;;"Első post"
                 :blog/tags (if (= (type tags) "java.lang.String") (clojure.string/split tags #",") tags)
                 :blog/date (clojure.instant/read-instant-date date) ;; #inst "2015-12-05T00:00:00" 
                 :blog/content content ;; "<h>Ehun egy html.<br>Ehun meg egy</h>"}]
                 }]
          conn (d/connect uri)
          db (d/db conn)
          resp (d/transact conn data)]
      (println "resp" resp)
      "OK")
    "Invalid pass"))

(defn update-post [pass postid title date tags content]
  (println "add post" pass title date tags content)
  (if (password/check pass epass)
    (let [data [{:db/id postid
                 :blog/title title ;;"Első post"
                 :blog/tags (if (= (type tags) "java.lang.String") (clojure.string/split tags #",") tags)
                 :blog/date (clojure.instant/read-instant-date date) ;; #inst "2015-12-05T00:00:00" 
                 :blog/content content ;; "<h>Ehun egy html.<br>Ehun meg egy</h>"}]
                 }]
          conn (d/connect uri)
          db (d/db conn)
          resp (d/transact conn data)]
      (println "resp" resp)
      "OK")
    "Invalid pass"))


(defn add-project [pass title tags type content]
  (println "add project" pass title tags type content)
  (if (password/check pass epass)
    (let [data [{:project/title title ;; "Termite 3D"
                 :project/tags (clojure.string/split tags #",")
                 :project/type type ;; "game" 
                 :project/content content ;; "Egy kurva jo jatek"
                 }]
          conn (d/connect uri)
          db (d/db conn)
          resp (d/transact conn data)]
      "OK")
    "Invalid password"
    ))

(defn -project [pass title tags type content]
  (println "add project" pass title tags type content)
  (if (password/check pass epass)
    (let [data [{:project/title title ;; "Termite 3D"
                 :project/tags (clojure.string/split tags #",")
                 :project/type type ;; "game" 
                 :project/content content ;; "Egy kurva jo jatek"
                 }]
          conn (d/connect uri)
          db (d/db conn)
          resp (d/transact conn data)]
      "OK")
    "Invalid password"
    ))

(defn add-comment [postid nick content code]
  (println "add-comment" postid nick content code)
  (let [data [{:comment/postid postid
               :comment/content content ;; "Faszasag!!!"
               :comment/nick nick ;; "milgra@milgra.com"
               :comment/date (new java.util.Date)
               }]
        conn (d/connect uri)
        resp (d/transact conn data)]
    (println "resp" resp)
    "OK"))

(defn remove-comment [commentid pass]
  (if (password/check pass epass)
    (let [conn (d/connect uri)
          resp (d/transact conn [[:db.fn/retractEntity commentid]])]
      (println "resp" resp)
      "OK")
    "Invalid pass"))

;;(add-comment 34556 "milgra" "faszt" 345)


(defn get-client-ip [req]
  (if-let [ips (get-in req [:headers "x-forwarded-for"])]
    (-> ips (clojure.string/split #",") first)
    (:remote-addr req)))

(defroutes app-routes
  (GET "/" [] "BLANK")
  (GET "/months" [] (json/write-str {:months (get-post-months)
                                     :tags (get-post-tags)}))
  (GET "/posts" [year month] (json/write-str {:posts (get-posts-for-month (Integer/parseInt year) (Integer/parseInt month))}))
  (GET "/comments" [postid] (json/write-str {:comments (get-post-comments (Long/parseLong postid))}))
  (GET "/projects" [type] (json/write-str {:projects (get-projects type)
                                          :tags (get-project-tags type)}))
  (GET "/newcomment" [postid nick text code] (json/write-str {:result (add-comment (Long/parseLong postid) nick text code)}))
  (GET "/delcomment" [commid pass] (json/write-str {:result (remove-comment (Long/parseLong commid) pass)}))
  (POST "/newproject" [pass title tags type content] (json/write-str {:result (add-project pass title tags type content)}))
  (POST "/newpost" [pass title date tags content] (json/write-str {:result (add-post pass title date tags content)}))
  (POST "/updatepost" [pass postid title date tags content] (json/write-str {:result (update-post pass (Long/parseLong postid) title date tags content)}))
  
  (route/resources "/")
  (route/not-found "Not Found"))


(def app
  (-> app-routes
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:post :get]
                 :access-control-allow-credentials "true"
                 :Access-Control-Allow-Headers "Content-Type, Accept, Authorization, Authentication, If-Match, If-None-Match, If-Modified-Since, If-Unmodified-Since")
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))

;; init database on start
(setup)

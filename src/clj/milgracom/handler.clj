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


(defn get-all-comments
  "get all comments"
  []
  (let [conn (d/connect uri)
        db (d/db conn)
        posts (d/q db/all-comments-all-data-q db)]
    posts))


(defn setup
  "create schema"
  []
  (let [succ (d/create-database uri)] 
    (if succ 
      (let [conn (d/connect uri)
            db (d/db conn)]
        (let [resp (d/transact conn db/post-schema)]
          (println "post schema insert resp" resp))
        (let [resp (d/transact conn db/comment-schema)]
          (println "comment schema insert resp" resp)))
      (println "db exists"))))


(defn delete []
  (let [succ (d/delete-database uri)]
    (println "delete" succ)))


(defn fillup
  "fill up db with dev data"
  []
  (let [conn (d/connect uri)
        db (d/db conn)]
    (let [resp (d/transact conn db/first-posts)]
      (println "post insert resp" resp))
    (let [postid ((first (first (get-all-posts))) :db/id)
          comment (assoc db/first-comment :comment/postid postid)
          resp (d/transact conn [comment])]
      (println "comment insert resp" resp))))


(defn get-post-months
  "get all months where posts exist"
  [type]
  (let [dbtype (keyword type)
        conn (d/connect uri)
        db (d/db conn)
        dates (d/q db/all-post-months-by-type-q db dbtype)]
    (println "dates" dates)
    (reverse
     (sort
      (set
       (map (fn [[{date :post/date}]]
              [(+ 1900 (.getYear date)) (inc (.getMonth date))])
            dates))))))

;;(get-post-months "blog")

(defn get-post-tags
  "get all tags in posts"
  [type]
  (let [dbtype (keyword type)
        conn (d/connect uri)
        db (d/db conn)
        tags (d/q db/all-post-tags-by-type-q db dbtype)]
    (reduce (fn [res item] (into res ((first item) :post/tags)) ) #{} tags)))

;;(get-post-tags "blog")

(defn get-posts-for-month
  "get all posts for given year and month"
  [year month type]
  (let [dbyear (Integer/parseInt year)
        dbmonth (Integer/parseInt month)
        dbtype (keyword type)
        conn (d/connect uri)
        db (d/db conn)
        endmonth (if (= dbmonth 12) 1 (+ dbmonth 1))
        endyear (if (= dbmonth 12) (+ dbyear 1) dbyear)
        start (clojure.instant/read-instant-date (format "%d-%02d-01T00:00:00" dbyear dbmonth))
        end (clojure.instant/read-instant-date  (format "%d-%02d-01T00:00:00" endyear endmonth))
        posts (d/q db/posts-between-dates-by-type-q db start end dbtype)]
    (map (fn [[{date :post/date :as val}]]
           (assoc val :post/date (.format (java.text.SimpleDateFormat. "yyyy-dd-MM") date)))
         posts)))

;;(get-posts-for-month 2018 4 "blog")

(defn get-posts-for-type
  "get all posts for given year and month"
  [type]
  (let [dbtype (keyword type)
        conn (d/connect uri)
        db (d/db conn)
        posts (d/q db/all-posts-all-data-by-type-q db dbtype)]
    (map (fn [[{date :post/date :as val}]]
           (assoc val :post/date (.format (java.text.SimpleDateFormat. "yyyy-dd-MM") date)))
         posts)))

;;(get-posts-for-type "game")

(defn get-post-comments
  "returns comments for give post id"
  [postid]
  (println "get-post-comments" postid)
  (let [dbpostid (Long/parseLong postid)
        conn (d/connect uri)
        db (d/db conn)
        comments (d/q db/comments-for-post-q db dbpostid)]
    (println "res" comments)
    (map (fn [[{date :comment/date :as val}]]
           (assoc val :comment/date (.format (java.text.SimpleDateFormat. "yyyy-dd-MM") date)))
    comments)))

;;(get-post-comments 17592186045425)

(defn add-post [pass title date tags type content]
  (println "add post" pass title date tags content)
  (if (password/check pass epass)
    (let [dbtype (keyword type)
          data [{:post/title title ;;"Első post"
                 :post/type dbtype
                 :post/tags (if (= (type tags) "java.lang.String") (clojure.string/split tags #",") tags)
                 :post/date (clojure.instant/read-instant-date date) ;; #inst "2015-12-05T00:00:00" 
                 :post/content content ;; "<h>Ehun egy html.<br>Ehun meg egy</h>"}]
                 }]
          conn (d/connect uri)
          db (d/db conn)
          resp (d/transact conn data)]
      (println "resp" resp)
      "OK")
    "Invalid pass"))


(defn update-post [pass id title date tags type content]
  (println "add post" pass title date tags content)
  ;; todo check input validity
  (if (password/check pass epass)
    (let [dbtype (keyword type)
          dbid (Long/parseLong id)
          data [{:db/id id
                 :post/title title ;;"Első post"
                 :post/type dbtype
                 :post/tags (if (= (type tags) "java.lang.String") (clojure.string/split tags #",") tags)
                 :post/date (clojure.instant/read-instant-date date) ;; #inst "2015-12-05T00:00:00" 
                 :post/content content ;; "<h>Ehun egy html.<br>Ehun meg egy</h>"}]
                 }]
          conn (d/connect uri)
          db (d/db conn)
          resp (d/transact conn data)]
      (println "resp" resp)
      "OK")
    "Invalid pass"))


(defn add-comment [postid nick content code]
  (println "add-comment" postid nick content code)
  (if (and postid nick content code)
    (let [dbpostid (Long/parseLong postid)
          data [{:comment/postid dbpostid
                 :comment/content content ;; "Faszasag!!!"
                 :comment/nick nick ;; "milgra@milgra.com"
                 :comment/date (new java.util.Date)
                 }]
          conn (d/connect uri)
          resp (d/transact conn data)]
      (println "resp" resp)
      "OK"))
  "Invalid parameters"
  )

(defn remove-comment [pass id]
  (if (password/check pass epass)
    (let [dbid (Long/parseLong id)
          conn (d/connect uri)
          resp (d/transact conn [[:db.fn/retractEntity dbid]])]
      (println "resp" resp)
      "OK")
    "Invalid pass"))


(defn get-client-ip [req]
  (if-let [ips (get-in req [:headers "x-forwarded-for"])]
    (-> ips (clojure.string/split #",") first)
    (:remote-addr req)))


(defroutes app-routes
  (GET "/" [] "BLANK")
  (GET "/months" [type] (json/write-str {:months (get-post-months type) :tags (get-post-tags type)}))
  (GET "/posts" [year month type] (json/write-str {:posts (get-posts-for-month year month type)}))
  (GET "/comments" [postid] (json/write-str {:comments (get-post-comments postid)}))
  (GET "/newcomment" [postid nick text code] (json/write-str {:result (add-comment postid nick text code)}))
  (GET "/delcomment" [pass id] (json/write-str {:result (remove-comment pass id)}))
  (POST "/newpost" [pass title date tags content] (json/write-str {:result (add-post pass title date tags content)}))
  (POST "/updatepost" [pass id title date tags type content] (json/write-str {:result (update-post pass id title date tags type content)}))
  
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

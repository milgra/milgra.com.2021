(ns milgracom.handler
  (:require
            [datomic.api :as d]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [milgracom.database :as db]
            [crypto.password.scrypt :as password]
            [ring.util.response :as resp]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


(defonce uri "datomic:dev://localhost:4334/milgracom")
(defonce epass "$s0$f0801$ltUrt7mIR8BW90xbCpGe0Q==$sxNuunkgX7GuXuzjAEUXPUqVIq0U00CRUxJFG9MyP30=")
;;(password/encrypt "password")

(defonce ip-to-result (atom []))
(defonce number-names ["zero" "one" "two" "three" "four" "five" "six" "seven" "eight" "nine"])
(defonce types ["blog" "game" "app" "proto"])
(defonce conn (d/connect uri))

(defn get-client-ip [req]
  (if-let [ips (get-in req [:headers "x-forwarded-for"])]
    (-> ips (clojure.string/split #",") first)
    (:remote-addr req)))


(defn get-all-posts
  "get all posts"
  []
  (let [db (d/db conn)
        posts (d/q db/all-posts-all-data-q db)]
    posts))


(defn get-all-comments
  "get all comments"
  []
  (let [db (d/db conn)
        posts (d/q db/all-comments-all-data-q db)]
    posts))


(defn setup
  "create schema"
  []
  (let [succ (d/create-database uri)] 
    (if succ 
      (let [db (d/db conn)]
        (let [resp (d/transact conn db/post-schema)]
          (println "post schema insert resp" resp))
        (let [resp (d/transact conn db/comment-schema)]
          (println "comment schema insert resp" resp)))
      (println "db exists"))))


(defn delete
  "delete database"
  []
  (let [succ (d/delete-database uri)]
    (println "delete" succ)))


(defn fillup
  "fill up db with dev data"
  []
  (let [db (d/db conn)]
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
        db (d/db conn)
        dates (d/q db/all-post-months-by-type-q db dbtype)]
    (reverse (sort (set (map
                         (fn [[{date :post/date}]] [(+ 1900 (.getYear date)) (inc (.getMonth date))])
                         dates))))))

;;(get-post-months "blog")

(defn get-post-tags
  "get all tags in posts"
  [type]
  (let [dbtype (keyword type)
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
        db (d/db conn)
        endmonth (if (= dbmonth 12) 1 (+ dbmonth 1))
        endyear (if (= dbmonth 12) (+ dbyear 1) dbyear)
        start (clojure.instant/read-instant-date (format "%d-%02d-01T00:00:00" dbyear dbmonth))
        end (clojure.instant/read-instant-date  (format "%d-%02d-01T00:00:00" endyear endmonth))
        posts (d/q db/posts-between-dates-by-type-q db start end dbtype)]
    (sort-by :post/date (map #(assoc % :post/date (subs (pr-str (% :post/date)) 7 26)) (map first posts)))
    ))

;;(sort-by :post/date (get-posts-for-month "2020" "03" "blog"))

(defn get-posts-for-type
  "get all posts for given year and month"
  [type]
  (let [dbtype (keyword type)
        db (d/db conn)
        posts (d/q db/all-posts-all-data-by-type-q db dbtype)]
    (sort-by :post/date (map #(assoc % :post/date (subs (pr-str (% :post/date)) 7 26)) (map first posts)))
    ))


;;(get-posts-for-type "game")
;;(str #inst "2019-12-30T01:01:01")


(defn get-posts-for-tag
  "get post titles for given tag"
  [tag]
  (let [db (d/db conn)
        posts (d/q db/posts-for-tag-q db tag)]
    (reverse (sort-by :post/date (map #(assoc % :post/date (subs (pr-str (% :post/date)) 7 26)) (map first posts)))
    )))

;;(get-posts-for-tag "Music")

(defn get-post
  [id]
  (let [dbid (Long/parseLong id)
        db (d/db conn)
        entity (d/entity db dbid)
        post (into {} (seq entity))
        result (assoc post :post/date (subs (pr-str (post :post/date)) 7 26))
        ]
    (list result)))

;;(get-post "17592186045508")

(defn get-post-comments
  "returns comments for give post id"
  [postid]
  (let [dbpostid (Long/parseLong postid)
        db (d/db conn)
        comments (d/q db/comments-for-post-q db dbpostid)]

    (sort-by :comment/date (map #(assoc % :comment/date (subs (pr-str (% :comment/date)) 7 26)) (map first comments)))
    ))


;;(get-post-comments "17592186045425")


(defn add-post
  "add post to database"
  [pass title date tags typestr content]
  (if (and (password/check pass epass) (some #(= % typestr) types))
    (let [dbtype (keyword typestr)
          data [{:post/title title
                 :post/type dbtype
                 :post/tags tags
                 :post/date (clojure.instant/read-instant-date date)
                 :post/content content
                 }]
          db (d/db conn)
          resp (d/transact conn data)]
      "OK")
    "Invalid pass or type"))


(defn update-post
  "update post in database"
  [pass id title date tags typestr content]
  ;; todo check input validity
  (if (and (password/check pass epass) (some #(= % typestr) types))
    (let [dbtype (keyword typestr)
          dbid (Long/parseLong id)
          data [{:db/id dbid
                 :post/title title
                 :post/type dbtype
                 :post/tags tags
                 :post/date (clojure.instant/read-instant-date date)
                 :post/content content
                 }]
          db (d/db conn)
          resp (d/transact conn data)]
      "OK")
    "Invalid pass"))


(defn remove-post
  "retracts post from database"
  [pass id]
  (if (password/check pass epass)
    (let [dbid (Long/parseLong id)
          resp (d/transact conn [[:db.fn/retractEntity dbid]])]
      "OK")
    "Invalid pass"))


(defn add-comment
  "add comment to database"
  [postid nick content code request]
  (if (and postid nick content code)
    (let [numcode (Integer/parseInt code)
          clientip (get-client-ip request)
          checks (filter (fn [{ip :ip result :result}] (and (= ip clientip) (= result numcode)))  @ip-to-result)]
      ;; check validity
      (if (> (count checks) 0)
        (let [dbpostid (Long/parseLong postid)
              data [{:comment/postid dbpostid
                     :comment/content content
                     :comment/nick nick
                     :comment/date (new java.util.Date)}]
              resp (d/transact conn data)]
          "OK")
        "Invalid code"))
    "Invalid parameters"))


(defn remove-entity
  "retracts entity from database"
  [pass id]
  (if (password/check pass epass)
    (let [dbid (Long/parseLong id)
          resp (d/transact conn [[:db.fn/retractEntity dbid]])]
      "OK")
    "Invalid pass"))


(defn generate-riddle
  "generates riddle to filter bots for commenting"
  [ip]
  (let [numa (rand-int 10)
        numb (rand-int 10)
        namea (nth number-names numa)
        nameb (nth number-names numb)
        items (count @ip-to-result)]
    ;; remove old items to keep memory clean
    (if (> items 10) (reset! ip-to-result (subvec @ip-to-result (- items 10))))
    ;; add new item
    (swap! ip-to-result conj {:ip ip :result (+ numa numb)})
    (str "How much is " namea " plus " nameb "?")))

;; (generate-riddle "156.45.67.66")  TODO move this to tests


(defroutes app-routes
  
  (GET "/" [] (resp/redirect "/index.html"))
  (GET "/post/:id" [] (resp/redirect "/index.html"))
  (GET "/projects" [] (resp/redirect "/index.html"))
  (GET "/apps" [] (resp/redirect "/index.html"))
  (GET "/games" [] (resp/redirect "/index.html"))
  (GET "/protos" [] (resp/redirect "/index.html"))

  (GET "/api-getmonths" [type] (json/write-str {:months (get-post-months type) :tags (get-post-tags type)}))
  (GET "/api-getpostsbydate" [year month type] (json/write-str {:posts (get-posts-for-month year month type)}))
  (GET "/api-getpostsbytag" [tag] (json/write-str {:posts (get-posts-for-tag tag)}))
  (GET "/api-getposts" [year month type] (json/write-str {:posts (get-posts-for-type type) :tags (get-post-tags type)}))
  (GET "/api-getpost" [id] (json/write-str {:posts (get-post id)}))
  (GET "/api-getcomments" [postid] (json/write-str {:comments (get-post-comments postid)}))
  (GET "/api-genriddle" request (json/write-str {:question (generate-riddle (get-client-ip request))}))
  (GET "/api-addcomment" [postid nick text code :as request] (json/write-str {:result (add-comment postid nick text code request)}))

  ;; admin related
  
  (POST "/api-addpost" [pass title date tags type content] (json/write-str {:result (add-post pass title date tags type  content)}))
  (POST "/api-updatepost" [pass id title date tags type content] (json/write-str {:result (update-post pass id title date tags type content)}))
  (GET "/api-removepost" [pass id] (json/write-str {:result (remove-entity pass id)}))
  (GET "/api-removecomment" [pass id] (json/write-str {:result (remove-entity pass id)}))
  
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

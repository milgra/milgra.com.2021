(ns milgra.com.server.database)

;; schemas

(def blog-schema
  [{:db/ident :blog/title
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


(def comment-schema
  [{:db/ident :comment/postid
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The id of the comment's parent post"}

   {:db/ident :comment/content
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


(def project-schema
  [{:db/ident :project/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The id of the comment's parent post"}

   {:db/ident :project/type
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The content of the comment "}
   
   {:db/ident :project/content
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The email of the commenter"}])


;; queries

(def all-posts-q
  '[:find ?e
   :where [?e :blog/title]])


(def all-posts-all-data-q
  '[:find ?e ?title ?date ?content
    :keys e title date content
    :where [?e :blog/title ?title] 
    [?e :blog/date ?date]
    [?e :blog/content ?content]])


(def all-post-months-q
  '[:find ?e ?date
    :where [?e :blog/date ?date]])


(def posts-between-dates
  '[:find ?e ?title ?date ?content
    :keys e title date content
    :in $ ?start ?end
    :where
    [?e :blog/title ?title]
    [?e :blog/date ?date]
    [?e :blog/content ?content]
    [(> ?date ?start)]
    [(< ?date ?end)]])


(def all-comments-all-data-q
  '[:find ?e ?postid ?email ?date ?content
    :keys e postid email date content
    :where
    [?e :comment/postid ?postid]
    [?e :comment/email ?email]
    [?e :comment/date ?date]
    [?e :comment/content ?content]])


(def comments-for-post-q
  '[:find ?postid ?email ?date ?content
    :keys postid email date content
    :in $ ?postid
    :where
    [?postid]
    [?e :comment/email ?email]
    [?e :comment/date ?date]
    [?e :comment/content ?content]])


(def projects-for-type-q
  '[:find ?title ?type ?content
    :keys title type content
    :in $ ?type
    :where
    [?type]
    [?e :comment/title ?type]
    [?e :comment/content ?content]])


;; test data

(def first-posts
  [{:blog/title "Első post"
    :blog/date #inst "2015-12-05T00:00:00" 
    :blog/content "<h>Ehun egy html.<br>Ehun meg egy</h>"}

   {:blog/title "Második post"
    :blog/date  #inst "2015-11-25T00:00:00"
    :blog/content "<h>Másdoik második. Ehun egy html.<br>Ehun meg egy</h>"}

   {:blog/title "Második post"
    :blog/date  #inst "2018-04-13T00:00:00"
    :blog/content "<h>Negyedik második. Ehun egy html.<br>Ehun meg egy</h>"}

   {:blog/title "Harmadik post"
    :blog/date  #inst "2017-07-30T00:00:00"
    :blog/content "<h>Harmadik harmadik. Ehun egy html.<br>Ehun meg egy</h>"}])


(def first-comment
  {:comment/postid 17592186045419
   :comment/content "Faszasag!!!"
   :comment/email "milgra@milgra.com"
   :comment/date #inst "2018-07-30T00:00:00"})


(def first-projects
  [{:project/title "Termite 3D"
    :project/type "game" 
    :project/content "Egy kurva jo jatek"}

   {:project/title "Kinetic 3D"
    :project/type  "prototype"
    :project/content "Faszasag!!!"}

   {:project/title "Mac Media Key Forwarder"
    :project/type  "app"
    :project/content "Faszasag!!!"}])

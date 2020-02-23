(ns milgracom.database)

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

   {:db/ident :blog/tags
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "The tags of the post"}
 
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

   {:db/ident :project/tags
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "The tags of the post"}
   
   {:db/ident :project/content
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The email of the commenter"}])


;; queries

(def all-posts-q
  '[:find ?e
   :where [?e :blog/title]])


(def all-post-tags-q
  '[:find (pull ?e [:blog/tags])
    :where [?e :blog/tags]])


(def all-project-tags-q
  '[:find (pull ?e [:project/title :project/tags])
    :in $ ?type
    :where
    [?e :project/type ?ptype]
    [(= ?type ?ptype)]])


(def all-posts-all-data-q
  '[:find (pull ?e [:db/id :blog/title :blog/date :blog/content :blog/tags])
    :where [?e :blog/title]])


(def all-post-months-q
  '[:find ?e ?date
    :where [?e :blog/date ?date]])


(def posts-between-dates
  '[:find (pull ?e [:db/id :blog/title :blog/date :blog/content :blog/tags])
    :in $ ?start ?end
    :where
    [?e :blog/date ?date]
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
    :in $ ?wtype
    :where
    [?e :project/title ?title]
    [?e :project/type ?type]
    [?e :project/content ?content]
    [(= ?type ?wtype)]]
  )


;; test data

(def first-posts
  [{:blog/title "Első post"
    :blog/date #inst "2016-10-07T00:00:00"
    :blog/tags ["c" "programming"]
    :blog/content "#Emscripten
*2016-10-07 17:49*
Category: Programming

Tags: C, Emscripten, WebGL
Emscripten compiles C code to javascript. Not any kind of C code, just carefully, platform-independently written C code. Not to plain javascript, but to superfast asm.js javascript. It converts OpenGL3/ES2 calls to WebGL calls, it converts BSD sockets to Websockets, it puts MEMFS/IDBFS file systems under your file I/O and you don't have to deal with anything! ( Okay, you can't use POSIX threads but if you really need them you can work it around with webworkers ).

![emscripten](images/20161007_emscripten.png)

So you just take previously written C/OPENGL games/prototypes and you compile them for the BROWSER!!! It's madness!

I don't have the nerve for web programming, for tons of DOM elements and css, for different js vm implementations, for debugging hell and everything else modern web gave me as a developer. And now I don't have to deal with all these things, and still I can deploy for the browser!!! ( okay, I have to deal with them a little because it still is web development, but hey, I only need hours now to fix something strange, not days!!! )

I already compiled a lot of my stuff to javascript with this fantastic technology :

The ultimate fighting experience, [Mass Brawl](massbrawl)  
The ambient-reflex game, [FLOW](flow)  
The conference prototype, [Control Room](controlroom)  
Laser trinagulation prototype, [Laserscan](laserscan)  

IMHO Emscripten is the best technology of the 2010's so far."}

   {:blog/title "Második post"
    :blog/tags ["c" "drawing"]
    :blog/date  #inst "2015-11-25T00:00:00"
    :blog/content "<h>Másdoik második. Ehun egy html.<br>Ehun meg egy</h>"}

   {:blog/title "Második post"
    :blog/tags ["c"]
    :blog/date  #inst "2018-04-13T00:00:00"
    :blog/content "<h>Negyedik második. Ehun egy html.<br>Ehun meg egy</h>"}

   {:blog/title "Harmadik post"
    :blog/tags ["d" "prog"]
    :blog/date  #inst "2017-07-30T00:00:00"
    :blog/content "<h>Harmadik harmadik. Ehun egy html.<br>Ehun meg egy</h>"}])


(def first-comment
  {:comment/postid 17592186045419
   :comment/content "Faszasag!!!"
   :comment/email "milgra@milgra.com"
   :comment/date #inst "2018-07-30T00:00:00"})


(def first-projects
  [{:project/title "Termite 3D"
    :project/tags ["action" "strategy" "real-time"] 
    :project/type "game" 
    :project/content "Egy kurva jo jatek"}

   {:project/title "Kinetic 3D"
    :project/type  "prototype"
    :project/tags ["3D" "OpenGL"]
    :project/content "Faszasag!!!"}

   {:project/title "Mac Media Key Forwarder"
    :project/tags ["Utility"]
    :project/type  "app"
    :project/content "Faszasag!!!"}])

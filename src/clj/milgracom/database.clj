(ns milgracom.database)

;; schemas

(def post-schema
  [{:db/ident :post/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The title of the post"}

   {:db/ident :post/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "The type of the post (:blog :game :app :proto)"}
   
   {:db/ident :post/date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The date of the post"}

   {:db/ident :post/tags
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "The tags of the post"}

   {:db/ident :post/comments
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/many
    :db/doc "Comment count for the  post"}

   {:db/ident :post/content
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
   
   {:db/ident :comment/nick
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The nick of the commenter"}
   
   {:db/ident :comment/date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The date of the comment"}])


;; queries

(def all-posts-q
  '[:find ?e
   :where [?e :post/type]])


(def all-posts-all-data-q
  '[:find (pull ?e [:db/id :post/title :post/date :post/content :post/tags :post/type :post/comments])
    :where [?e :post/title]])


(def all-posts-all-data-by-type-q
  '[:find (pull ?e [:db/id :post/title :post/date :post/content :post/tags :post/type :post/comments])
    :in $ ?type
    :where
    [?e :post/type ?ptype]
    [(= ?type ?ptype)]])

(def all-post-tags-by-type-q
  '[:find (pull ?e [:post/tags])
    :in $ ?type
    :where
    [?e :post/type ?ptype]
    [(= ?type ?ptype)]])


(def all-post-months-by-type-q
  '[:find (pull ?e [:post/date])
    :in $ ?type
    :where
    [?e :post/type ?ptype]
    [(= ?type ?ptype)]])


(def posts-between-dates-by-type-q
  '[:find (pull ?e [:db/id :post/title :post/date :post/content :post/tags :post/type :post/comments])
    :in $ ?start ?end ?type
    :where
    [?e :post/type ?ptype]
    [?e :post/date ?pdate]
    [(= ?ptype ?type)]
    [(>= ?pdate ?start)]
    [(<= ?pdate ?end)]])


(def all-comments-all-data-q
  '[:find (pull ?e [:db/id :comment/postid :comment/nick :comment/date :comment/content])
    :where
    [?e :comment/postid ?postid]])


(def comments-for-post-q
  '[:find (pull ?e [:db/id :comment/postid :comment/nick :comment/date :comment/content])
    :in $ ?postid
    :where
    [?e :comment/postid ?postid]])


;; test data

(def first-posts
  [{:post/title "Emscripten"
    :post/date #inst "2016-10-07T00:00:00"
    :post/type :blog
    :post/tags ["c" "programming"]
    :post/comments 0
    :post/content "
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

   {:post/title "Második post"
    :post/tags ["c" "drawing"]
    :post/date  #inst "2015-11-25T00:00:00"
    :post/type :blog
    :post/comments 0
    :post/content "<h>Másdoik második. Ehun egy html.<br>Ehun meg egy</h>"}

   {:post/title "Második post"
    :post/tags ["c"]
    :post/type :blog
    :post/date  #inst "2018-04-13T00:00:00"
    :post/comments 0
    :post/content "<h>Negyedik második. Ehun egy html.<br>Ehun meg egy</h>"}

   {:post/title "Harmadik post"
    :post/tags ["d" "prog"]
    :post/type :blog
    :post/date  #inst "2017-07-30T00:00:00"
    :post/comments 0
    :post/content "<h>Harmadik harmadik. Ehun egy html.<br>Ehun meg egy</h>"}

   {:post/title "Termite 3D"
    :post/type :game
    :post/tags ["action" "strategy" "real-time"] 
    :post/date  #inst "2017-07-30T00:00:00"
    :post/comments 0
    :post/content "Egy kurva jo jatek"}
   
   {:post/title "Kinetic 3D"
    :post/type  :prototype
    :post/date  #inst "2017-07-30T00:00:00"
    :post/comments 0
    :post/tags ["3D" "OpenGL"]
    :post/content "Faszasag!!!"}
   
   {:post/title "Mac Media Key Forwarder"
    :post/tags ["Utility"]
    :post/type  :app
    :post/date  #inst "2017-07-30T00:00:00"
    :post/comments 0
    :post/content "Faszasag!!!"}])


(def first-comment
  {:comment/postid 17592186045419
   :comment/content "Faszasag!!!"
   :comment/nick "milgra"
   :comment/date #inst "2018-07-30T00:00:00"})

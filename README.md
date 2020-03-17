# Clojure Blog Engine

This is a project that I used to learn full stack web development in clojure and this is also the source code of my homepage.

Step-by-step tutorial on how to create will be here : [link](github.com/milgra/full-stack-clojure)

client side code : ```src/cljs```

server side code : ```src/clj```

start server :

```lein ring server-headless```

start shadow-cljs repl and server for client side dev :

```shadow watch app```

todo :

* search field & functionality
* dynamic page size based on window size to help mobile viewers
* tag button should show up a title list instead of all posts
* increase comment counter
* admin should show up with actual date at new post
* all posts admin page with delete button
* allow access only from ip
* stilusok kivezetese
* insert go blocks into functions where possible
* break up code, create component namespaces
* rossz code eseten kerje ujra code-ot vagy dobjon napi kvota lejartat
* server csekkoljon minden parametert
* tesztek
* shadow-cljs release app not working
* auto backup
* "loading" text stylize in html on startup
* szerver nelkul hogy kezeli a site a hibakat?
* heckelest hogy allja a szerver?
* adott post/comment jojjon be ha parametert kap az index.html -> apps linkek mukodjenek
* parameter validity checking (length, year-month format, etc)
# Clojure Blog Engine

This is a project that I used to learn full stack web development in clojure and this is also the source code of my homepage.

[Check it out](http://milgra.com)

client side code : ```src/cljs```

server side code : ```src/clj```

start server :

```lein ring server-headless```

start shadow-cljs repl and server for client side dev :

```shadow watch app```

todo :

* valid hour:minute on post edit
* convert to inst everywhere
* css cleanup & refactor
* stilusok kivezetese

* search field & functionality
* dynamic page size based on window size to help mobile viewers
* increase comment counter
* admin should show up with actual date at new post
* all posts admin page with delete button
* allow access only from ip
* insert go blocks into functions where possible
* break up code, create component namespaces
* rossz code eseten kerje ujra code-ot vagy dobjon napi kvota lejartat
* server csekkoljon minden parametert
* tesztek
* "loading" text stylize in html on startup
* szerver nelkul hogy kezeli a site a hibakat?
* heckelest hogy allja a szerver?
* adott post/comment jojjon be ha parametert kap az index.html -> apps linkek mukodjenek
* parameter validity checking (length, year-month format, etc)

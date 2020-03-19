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

* stilusok kivezetese
* "loading" text stylize in html on startup

* increase comment counter
* pagecard state - posts, projects, list
* blog/apps/protos/games in url

* search field & functionality
* dynamic page size based on window size to help mobile viewers
* all posts admin page with delete button
* allow access only from milgra.com when dns is ready
* break up code, create component namespaces
* server should check all parameters
* server side tests on temporary database

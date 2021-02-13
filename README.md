# Clojure Blog Engine

This is a project that I used to learn full stack web development in clojure and this is also the source code of my homepage.

[Check it out](http://milgra.com)

start server :

```
cd server
lein ring server-headless
```

start client :

```
cd client
shadow watch app
```

todo :

* post-bound comments in central db
* increase comment counter
* implement history back/next
* blog/apps/protos/games in url

* search field & functionality
* dynamic page size based on window size to help mobile viewers
* all posts admin page with delete button
* allow access only from milgra.com when dns is ready
* break up code, create component namespaces
* server should check all parameters
* server side tests on temporary database

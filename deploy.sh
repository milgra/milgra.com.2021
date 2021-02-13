cd client
shadow-cljs release app
cd ..

cd server
lein ring uberjar

cd target

# bin/console -p 8080 dev datomic:dev://localhost:4334/
# rsync -v -r -e ssh datomic-pro-0.9.6024 root@116.203.87.141:/root/

rsync -vre ssh milgra.com.server-0.8.0-standalone.jar root@116.203.87.141:/root/

rsync -v -r -e ssh datomic-pro-0.9.6024/ root@80.211.79.127:/root
rsync -v -r -e ssh milgra.com.server-0.1.0-SNAPSHOT-standalone.jar root@80.211.79.127:/root 

nohup bin/transactor -Xmx256m -Xms256m dev.properties &
nohup java -server -Xms256m -Xmx256m -Ddatomic.objectCacheMax=64m -Ddatomic.memoryIndexMax=64m -jar milgra.com.server-0.1.0-SNAPSHOT-standalone.jar &

http://80.211.79.127/months

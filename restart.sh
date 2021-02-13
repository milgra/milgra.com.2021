killall java
cd datomic-pro-0.9.6024
nohup bin/transactor -Xmx256m -Xms256m dev.properties &
cd ..
nohup java -server -Xms256m -Xmx256m -Ddatomic.objectCacheMax=64m -Ddatomic.memoryIndexMax=64m -jar milgra.com.server-0.8.0-standalone.jar &

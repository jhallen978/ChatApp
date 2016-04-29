all:
	javac ./client/*.java ./server/*.java
clean:
	rm ./server/*.java~ ./server/*.class ./server/*.class~ ./client/*.java~ ./client/*.class ./client/*.class~ Makefile~
runserver:
	java -cp ./server/ Server
runclient:
	java -cp ./client/ Client
all: target/graylog2-server-0.9.6-SNAPSHOT.jar target/graylog2-server-0.9.6-SNAPSHOT-jar-with-dependencies.jar test


target/graylog2-server-0.9.6-SNAPSHOT.jar target/graylog2-server-0.9.6-SNAPSHOT-jar-with-dependencies.jar:
	mvn assembly:assembly

test:
	mvn test

clean:
	mvn clean

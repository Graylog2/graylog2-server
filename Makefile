NAME=graylog2-server
PREFIX=/usr
DESTDIR=
CONF=$(DESTDIR)/etc

SERVER_W_DEP=target/graylog2-server-0.9.6-SNAPSHOT-jar-with-dependencies.jar
SERVER=target/graylog2-server-0.9.6-SNAPSHOT.jar

all: $(SERVER) $(SERVER_W_DEP) test

$(SERVER) $(SERVER_W_DEP):
	mvn assembly:assembly

test:
	mvn test

clean:
	mvn clean

install: $(SERVER_W_DEP)
	install -m 755 -d $(DESTDIR)$(PREFIX)/share/$(NAME)
	install -m 0644 $(SERVER_W_DEP) $(DESTDIR)$(PREFIX)/share/$(NAME)/graylog2-server.jar
	install -m 755 -d $(DESTDIR)/etc/init.d

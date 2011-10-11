NAME=graylog2-server
PREFIX=/usr
DESTDIR=

SERVER_W_DEP=target/graylog2-server-0.9.6-SNAPSHOT-jar-with-dependencies.jar
SERVER=target/graylog2-server-0.9.6-SNAPSHOT.jar
INITD=contrib/distro/generic/graylog2-server.init.d
CONF=misc/graylog2.conf

MVN_REPO="/tmp/$(NAME)-build-${USER}"
MVN_OPTS=-Dmaven.repo.local=${MVN_REPO}

all: $(SERVER) $(SERVER_W_DEP) test

$(SERVER) $(SERVER_W_DEP):
	mvn $(MVN_OPTS) assembly:assembly

test:
	mvn $(MVN_OPTS) test

clean:
	mvn $(MVN_OPTS) clean

install: $(SERVER_W_DEP) $(INITD)
	install -m 755 -d $(DESTDIR)$(PREFIX)/share/$(NAME)
	install -m 0644 $(SERVER_W_DEP) $(DESTDIR)$(PREFIX)/share/$(NAME)/graylog2-server.jar
	install -m 755 -d $(DESTDIR)/etc/init.d
	install -m 0755 $(INITD) $(DESTDIR)/etc/init.d/graylog2-server
	install -m 0600 $(CONF) $(DESTDIR)/etc/graylog2.conf

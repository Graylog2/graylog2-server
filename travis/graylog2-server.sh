#!/bin/bash

pushd $HOME

git clone https://github.com/Graylog2/graylog2-server.git

pushd graylog2-server

sh install-syslog4j-jar.sh
mvn clean package -DskipTests

mvn install

cp $TRAVIS_BUILD_DIR/travis/server.conf graylog2-travis-server.conf
nohup java -jar graylog2-server/target/graylog2-server.jar -f graylog2-travis-server.conf -l -p graylog2-travis-server.pid &

popd

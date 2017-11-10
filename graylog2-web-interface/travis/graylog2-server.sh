#!/bin/bash -e
cd $HOME

echo 'Cloning graylog2-server git repository...'
git clone --quiet --depth 1 https://github.com/Graylog2/graylog2-server.git

pushd graylog2-server

sh install-syslog4j-jar.sh

echo 'Building graylog2-server...'
mvn --batch-mode --fail-fast --quiet -DskipTests=true \
  -Dmaven.javadoc.skip=true -Dspotbugs.skip=true -Dsource.skip=true \
  clean install

echo 'Starting graylog2-server...'
nohup java -jar graylog2-server/target/graylog2-server.jar -f $TRAVIS_BUILD_DIR/travis/server.conf -l -p graylog2-travis-server.pid &

popd

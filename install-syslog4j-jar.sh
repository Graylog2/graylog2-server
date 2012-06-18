MVN_REPO="/tmp/graylog2-server-build-${USER}"
MVN_OPTS=-Dmaven.repo.local=${MVN_REPO}

echo "Installing provided syslog4j jar to local mvn repository for great justice!"
mvn install:install-file ${MVN_OPTS} -DgroupId=org.syslog4j -DartifactId=syslog4j -Dversion=0.9.46 -Dpackaging=jar -Dfile=lib/syslog4j-0.9.46-bin.jar

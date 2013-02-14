echo "Installing our provided syslog4j fork .jar to local mvn repository for great justice!"
mvn install:install-file -DgroupId=org.productivity.java -DartifactId=syslog4j-graylog2 -Dversion=0.9.48-graylog2 -Dpackaging=jar -Dfile=lib/syslog4j-graylog2-0.9.48-graylog2.jar

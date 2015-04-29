#!/bin/bash

tar xzvfC /opt/graylog/assembly/*.tar.gz /opt/graylog --strip 1
mkdir -p /opt/graylog/data/journal
mkdir /etc/service/graylog-server

cat > /etc/service/graylog-server/run <<'EOF'
#!/bin/sh

exec 2>&1

JAVA_OPTS="-Djava.net.preferIPv4Stack=true -Xms1g -Xmx1g -XX:NewRatio=1 -XX:PermSize=128m -XX:MaxPermSize=256m -server -XX:+ResizeTLAB -XX:+UseConcMarkSweepGC -XX:+CMSConcurrentMTEnabled -XX:+CMSClassUnloadingEnabled -XX:+UseParNewGC -XX:-OmitStackTraceInFastThrow"

exec java -Djava.library.path=/opt/graylog/lib/sigar $JAVA_OPTS -jar /opt/graylog/graylog.jar server -f /opt/graylog/graylog.conf -np
EOF
chmod +x /etc/service/graylog-server/run

/sbin/my_init

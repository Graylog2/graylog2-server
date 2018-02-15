#!/bin/bash

JAR=${uberjar.name}-${git.commit.id.describe}.jar
DEFAULT_JAVA_OPTS="-Xms512m -Xmx512m -server -XX:+ResizeTLAB -XX:+UseConcMarkSweepGC -XX:+CMSConcurrentMTEnabled -XX:+CMSClassUnloadingEnabled -XX:+UseParNewGC -XX:-OmitStackTraceInFastThrow"
JAVA_OPTS="${JAVA_OPTS:="$DEFAULT_JAVA_OPTS"}"

# default java
JAVA_CMD=${JAVA_CMD:=$(which java)}

if [ -n "$JAVA_HOME" ]
then
    # try to use $JAVA_HOME
    if [ -x "$JAVA_HOME"/bin/java ]
    then
        JAVA_CMD="$JAVA_HOME"/bin/java
    else
        die "$JAVA_HOME"/bin/java is not executable
    fi
fi

$JAVA_CMD $JAVA_OPTS -jar $JAR $@
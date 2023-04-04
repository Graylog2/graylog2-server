#!/usr/bin/env bash

set -eo pipefail

# Resolve links - $0 may be a softlink
DATANODE_BIN="$0"

while [ -h "$DATANODE_BIN" ]; do
    ls=$(ls -ld "$DATANODE_BIN")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        DATANODE_BIN="$link"
    else
        DATANODE_BIN=$(dirname "$DATANODE_BIN")/"$link"
    fi
done

DATANODE_ROOT="$(dirname "$(dirname "$DATANODE_BIN")")"
DATANODE_DEFAULT_JAR="${DATANODE_ROOT}/graylog-datanode.jar"
DATANODE_JVM_OPTIONS_FILE="${DATANODE_JVM_OPTIONS_FILE:-"$DATANODE_ROOT/config/jvm.options"}"
DATANODE_LOG4J_CONFIG_FILE="${DATANODE_LOG4J_CONFIG_FILE:-"$DATANODE_ROOT/config/log4j2.xml"}"

DATANODE_PARSED_JAVA_OPTS=""
if [ -f "$DATANODE_JVM_OPTIONS_FILE" ]; then
	DATANODE_PARSED_JAVA_OPTS=$(grep '^-' "$DATANODE_JVM_OPTIONS_FILE" | tr '\n' ' ')
fi

DATANODE_JAVA_OPTS="-Dlog4j.configurationFile=${DATANODE_LOG4J_CONFIG_FILE} ${DATANODE_PARSED_JAVA_OPTS% } $JAVA_OPTS"
DATANODE_JAR=${DATANODE_JAR:="$DATANODE_DEFAULT_JAR"}

JAVA_CMD="${JAVA_CMD}"

if [ -z "$JAVA_CMD" ]; then
	if [ -d "$DATANODE_ROOT/jvm" ]; then
		JAVA_HOME="$DATANODE_ROOT/jvm"
	else
		echo "ERROR: Java is not installed."
		exit 1
	fi
fi

if [ -n "$JAVA_HOME" ]; then
	java_cmd="${JAVA_HOME}/bin/java"

	if [ -x "$java_cmd" ]; then
		JAVA_CMD="$java_cmd"
	else
		echo "$java_cmd not executable or doesn't exist"
		exit 1
	fi
fi

exec "$JAVA_CMD" ${DATANODE_JAVA_OPTS% } -jar "$DATANODE_JAR" "$@"

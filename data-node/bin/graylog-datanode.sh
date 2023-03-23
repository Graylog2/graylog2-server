#!/usr/bin/env bash

set -o pipefail

JAVA_CMD=${JAVA_CMD:=$(which java)}

if [ -z "$JAVA_CMD" ]; then
  echo "ERROR: Java is not installed."
  exit 1
fi

set -e

if [ -n "$JAVA_HOME" ]; then
	java_cmd="${JAVA_HOME}/bin/java"

	if [ -x "$java_cmd" ]; then
		JAVA_CMD="$java_cmd"
	else
		echo "$java_cmd not executable or doesn't exist"
		exit 1
	fi
fi

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
DATANODE_JVM_OPTIONS_FILE="${DATANODE_JVM_OPTIONS_FILE:-$DATANODE_ROOT/config/jvm.options}"

DATANODE_PARSED_JAVA_OPTS=""
if [ -f "$DATANODE_JVM_OPTIONS_FILE" ]; then
	DATANODE_PARSED_JAVA_OPTS=$(grep '^-' "$DATANODE_JVM_OPTIONS_FILE" | tr '\n' ' ')
fi

DATANODE_JAVA_OPTS="${DATANODE_PARSED_JAVA_OPTS% } $JAVA_OPTS"
DATANODE_JAR=${DATANODE_JAR:="$DATANODE_DEFAULT_JAR"}

exec "$JAVA_CMD" ${DATANODE_JAVA_OPTS% } -jar "$DATANODE_JAR" "$@"

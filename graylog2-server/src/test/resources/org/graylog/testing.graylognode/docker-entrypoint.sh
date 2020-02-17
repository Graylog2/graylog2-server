#!/bin/bash

set -e

# save the settings over the docker(-compose) environment
__GRAYLOG_SERVER_JAVA_OPTS=${GRAYLOG_SERVER_JAVA_OPTS}

# shellcheck disable=SC1091
source /etc/profile

# and add the previos saved settings to our defaults
if [[ ! -z ${__GRAYLOG_SERVER_JAVA_OPTS} ]]
then
  echo "adding environment opts"
  GRAYLOG_SERVER_JAVA_OPTS="${GRAYLOG_SERVER_JAVA_OPTS} ${__GRAYLOG_SERVER_JAVA_OPTS}"
  export GRAYLOG_SERVER_JAVA_OPTS
fi

# Convert all environment variables with names ending in __FILE into the content of
# the file that they point at and use the name without the trailing __FILE.
# This can be used to carry in Docker secrets.
for VAR_NAME in $(env | grep '^GRAYLOG_[^=]\+__FILE=.\+' | sed -r 's/^(GRAYLOG_[^=]*)__FILE=.*/\1/g'); do
  VAR_NAME_FILE="${VAR_NAME}__FILE"
  if [ "${!VAR_NAME}" ]; then
    echo >&2 "ERROR: Both ${VAR_NAME} and ${VAR_NAME_FILE} are set but are exclusive"
    exit 1
  fi
  VAR_FILENAME="${!VAR_NAME_FILE}"
  echo "Getting secret ${VAR_NAME} from ${VAR_FILENAME}"
  if [ ! -r "${VAR_FILENAME}" ]; then
    echo >&2 "ERROR: ${VAR_FILENAME} does not exist or is not readable"
    exit 1
  fi
  export "${VAR_NAME}"="$(< "${VAR_FILENAME}")"
  unset "${VAR_NAME_FILE}"
done



if [ "${1:0:1}" = '-' ]; then
  set -- graylog "$@"
fi

# Delete outdated PID file
[[ -e /tmp/graylog.pid ]] && rm --force /tmp/graylog.pid

# check if we are inside kubernetes, Graylog should be run as statefulset and $POD_NAME env var should be defined like this
#          env:
#          - name: POD_NAME
#            valueFrom:
#              fieldRef:
#                fieldPath: metadata.name
# First stateful member is having pod name ended with -0, so
if [[ ! -z "${POD_NAME}" ]]
then
 if echo "${POD_NAME}" | grep "\\-0$" >/dev/null
 then
   export GRAYLOG_IS_MASTER="true"
 else
   export GRAYLOG_IS_MASTER="false"
 fi
fi

# check if we are inside a nomad cluster
# First member is having alloc-index 0, so
if [[ ! -z "${NOMAD_ALLOC_INDEX}" ]]; then
  if [ ${NOMAD_ALLOC_INDEX} == 0 ]; then
    export GRAYLOG_IS_MASTER="true"
  else
    export GRAYLOG_IS_MASTER="false"
  fi
fi


setup() {
  # Create data directories
  for d in journal log plugin config contentpacks
  do
    dir=${GRAYLOG_HOME}/data/${d}
    [[ -d "${dir}" ]] || mkdir -p "${dir}"

    if [[ "$(stat --format='%U:%G' $dir)" != 'graylog:graylog' ]] && [[ -w "$dir" ]]; then
      chown -R graylog:graylog "$dir" || echo "Warning can not change owner to graylog:graylog"
    fi
  done
}

graylog() {

  "${JAVA_HOME}/bin/java" \
    ${GRAYLOG_SERVER_JAVA_OPTS} \
    -jar \
    -Dlog4j.configurationFile="${GRAYLOG_HOME}/data/config/log4j2.xml" \
    -Djava.library.path="${GRAYLOG_HOME}/lib/sigar/" \
    -Dgraylog2.installation_source=docker \
    "${GRAYLOG_HOME}/graylog.jar" \
    server \
    -f "${GRAYLOG_HOME}/data/config/graylog.conf"
}

run() {
  setup
  graylog
}

run

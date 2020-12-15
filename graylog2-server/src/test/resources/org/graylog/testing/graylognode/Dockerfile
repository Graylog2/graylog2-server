FROM openjdk:8-jre-slim-buster

ARG GRAYLOG_VERSION
ARG BUILD_DATE
ARG GRAYLOG_HOME=/usr/share/graylog
ARG GRAYLOG_USER=graylog
ARG GRAYLOG_UID=1100
ARG GRAYLOG_GROUP=graylog
ARG GRAYLOG_GID=1100
ARG DEBUG_OPTS=''

WORKDIR ${GRAYLOG_HOME}

# hadolint ignore=DL3027,DL3008
RUN \
  echo "export JAVA_HOME=/usr/local/openjdk-8"     > /etc/profile.d/graylog.sh && \
  echo "export BUILD_DATE=${BUILD_DATE}"           >> /etc/profile.d/graylog.sh && \
  echo "export GRAYLOG_VERSION=${GRAYLOG_VERSION}" >> /etc/profile.d/graylog.sh && \
  echo "export GRAYLOG_SERVER_JAVA_OPTS='-Djdk.tls.acknowledgeCloseNotify=true -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:NewRatio=1 -XX:MaxMetaspaceSize=256m -server -XX:+ResizeTLAB -XX:+UseConcMarkSweepGC -XX:+CMSConcurrentMTEnabled -XX:+CMSClassUnloadingEnabled -XX:+UseParNewGC -XX:-OmitStackTraceInFastThrow " ${DEBUG_OPTS} "'" >> /etc/profile.d/graylog.sh && \
  echo "export GRAYLOG_HOME=${GRAYLOG_HOME}"       >> /etc/profile.d/graylog.sh && \
  echo "export GRAYLOG_USER=${GRAYLOG_USER}"       >> /etc/profile.d/graylog.sh && \
  echo "export GRAYLOG_GROUP=${GRAYLOG_GROUP}"     >> /etc/profile.d/graylog.sh && \
  echo "export GRAYLOG_UID=${GRAYLOG_UID}"         >> /etc/profile.d/graylog.sh && \
  echo "export GRAYLOG_GID=${GRAYLOG_GID}"         >> /etc/profile.d/graylog.sh && \
  echo "export PATH=${GRAYLOG_HOME}/bin:${PATH}"   >> /etc/profile.d/graylog.sh && \
  apt-get update  > /dev/null && \
  apt-get install --no-install-recommends --assume-yes \
    curl \
    tini \
    libcap2-bin \
    libglib2.0-0 \
    libx11-6 \
    libnss3 \
    fontconfig > /dev/null && \
  addgroup \
    --gid "${GRAYLOG_GID}" \
    --quiet \
    "${GRAYLOG_GROUP}" && \
  adduser \
    --disabled-password \
    --disabled-login \
    --gecos '' \
    --home ${GRAYLOG_HOME} \
    --uid "${GRAYLOG_UID}" \
    --gid "${GRAYLOG_GID}" \
    --quiet \
    "${GRAYLOG_USER}" && \
  chown --recursive "${GRAYLOG_USER}":"${GRAYLOG_GROUP}" ${GRAYLOG_HOME} && \
  setcap 'cap_net_bind_service=+ep' "${JAVA_HOME}/bin/java" && \
  apt-get remove --assume-yes --purge \
    apt-utils > /dev/null && \
  rm -f /etc/apt/sources.list.d/* && \
  apt-get clean > /dev/null && \
  apt autoremove --assume-yes > /dev/null && \
  rm -rf \
    /tmp/* \
    /var/cache/debconf/* \
    /var/lib/apt/lists/* \
    /var/log/* \
    /usr/share/X11 \
    /usr/share/doc/* 2> /dev/null

EXPOSE 9000
USER ${GRAYLOG_USER}

RUN \
  install \
    --directory \
    --mode=0750 \
    ${GRAYLOG_HOME}/data \
    ${GRAYLOG_HOME}/data/journal \
    ${GRAYLOG_HOME}/data/log \
    ${GRAYLOG_HOME}/data/config \
    ${GRAYLOG_HOME}/data/plugin \
    ${GRAYLOG_HOME}/data/data \
    ${GRAYLOG_HOME}/data/contentpacks \
    ${GRAYLOG_HOME}/lib \
    ${GRAYLOG_HOME}/log \
    ${GRAYLOG_HOME}/plugin

COPY docker-entrypoint.sh /
COPY graylog.conf ${GRAYLOG_HOME}/data/config
COPY log4j2.xml ${GRAYLOG_HOME}/data/config

ENTRYPOINT ["tini", "--", "/docker-entrypoint.sh"]
CMD ["graylog"]


# -------------------------------------------------------------------------------------------------
#
# layer for download and verifying
FROM debian:stretch-slim as graylog-downloader

ARG VCS_REF
ARG GRAYLOG_VERSION

WORKDIR /tmp

# hadolint ignore=DL3008,DL3015
RUN \
  apt-get update  > /dev/null && \
  apt-get install --assume-yes \
    ca-certificates \
    curl > /dev/null

RUN \
  curl \
    --silent \
    --location \
    --retry 3 \
    --output "/tmp/graylog-${GRAYLOG_VERSION}.tgz" \
    "https://packages.graylog2.org/releases/graylog/graylog-${GRAYLOG_VERSION}.tgz"

RUN \
  curl \
    --silent \
    --location \
    --retry 3 \
    --output "/tmp/graylog-${GRAYLOG_VERSION}.tgz.sha256.txt" \
    "https://packages.graylog2.org/releases/graylog/graylog-${GRAYLOG_VERSION}.tgz.sha256.txt"

RUN \
  sha256sum --check "graylog-${GRAYLOG_VERSION}.tgz.sha256.txt"

RUN \
  mkdir /opt/graylog && \
  tar --extract --gzip --file "/tmp/graylog-${GRAYLOG_VERSION}.tgz" --strip-components=1 --directory /opt/graylog

RUN \
  install \
    --directory \
    --mode=0750 \
    /opt/graylog/data \
    /opt/graylog/data/journal \
    /opt/graylog/data/log \
    /opt/graylog/data/config \
    /opt/graylog/data/plugin \
    /opt/graylog/data/data

# -------------------------------------------------------------------------------------------------
#
# final layer
# use the smallest debain with headless openjdk and copying files from download layers
FROM openjdk:8-jre-slim

ARG VCS_REF
ARG GRAYLOG_VERSION
ARG BUILD_DATE
ARG GRAYLOG_HOME=/usr/share/graylog
ARG GRAYLOG_USER=graylog
ARG GRAYLOG_UID=1100
ARG GRAYLOG_GROUP=graylog
ARG GRAYLOG_GID=1100

COPY --from=graylog-downloader /opt/graylog ${GRAYLOG_HOME}
COPY config ${GRAYLOG_HOME}/data/config

WORKDIR ${GRAYLOG_HOME}

# hadolint ignore=DL3027,DL3008
RUN \
  echo "export JAVA_HOME=/usr/local/openjdk-8"     > /etc/profile.d/graylog.sh && \
  echo "export BUILD_DATE=${BUILD_DATE}"           >> /etc/profile.d/graylog.sh && \
  echo "export GRAYLOG_VERSION=${GRAYLOG_VERSION}" >> /etc/profile.d/graylog.sh && \
  echo "export GRAYLOG_SERVER_JAVA_OPTS='-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:NewRatio=1 -XX:MaxMetaspaceSize=256m -server -XX:+ResizeTLAB -XX:+UseConcMarkSweepGC -XX:+CMSConcurrentMTEnabled -XX:+CMSClassUnloadingEnabled -XX:+UseParNewGC -XX:-OmitStackTraceInFastThrow'" >> /etc/profile.d/graylog.sh && \
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


COPY docker-entrypoint.sh /
COPY health_check.sh /

EXPOSE 9000
USER ${GRAYLOG_USER}
VOLUME ${GRAYLOG_HOME}/data
ENTRYPOINT ["tini", "--", "/docker-entrypoint.sh"]
CMD ["graylog"]

# add healthcheck
HEALTHCHECK \
  --interval=10s \
  --timeout=2s \
  --retries=12 \
  CMD /health_check.sh

# -------------------------------------------------------------------------------------------------

LABEL maintainer="Graylog, Inc. <hello@graylog.com>" \
      org.label-schema.name="Graylog Docker Image" \
      org.label-schema.description="Official Graylog Docker image" \
      org.label-schema.url="https://www.graylog.org/" \
      org.label-schema.vcs-ref=${VCS_REF} \
      org.label-schema.vcs-url="https://github.com/Graylog2/graylog-docker" \
      org.label-schema.vendor="Graylog, Inc." \
      org.label-schema.version=${GRAYLOG_VERSION} \
      org.label-schema.schema-version="1.0" \
      org.label-schema.build-date=${BUILD_DATE} \
      com.microscaling.docker.dockerfile="/Dockerfile" \
      com.microscaling.license="Apache 2.0"

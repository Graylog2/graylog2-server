# Build Graylog (github.com/Graylog2/graylog2-server) for Anthropic's OSS Scanner.
# The scanner uses the repository root as the build context and audits /src offline.
#
# Mirrors the "Open Build" CI workflow (.github/workflows/build.yml): Temurin JDK 21 and the Maven wrapper.
# The frontend-maven-plugin installs the Node.js and Yarn versions pinned in pom.xml, and the data-node module
# downloads the OpenSearch builds it bundles, so this file needs no version bumps when those change.
#
# What the finished image contains:
#   /src                  the full build: server, web interface (with node_modules), Data Node, storage modules
#   ~/.m2                 every dependency and plugin, so `./mvnw -o` works offline
#   /opt/graylog          the server distribution produced by this build (target/assembly)
#   /opt/opensearch       an OpenSearch (min distribution) of the version the Data Node bundles
#   mongod                MongoDB from the official repository
#   graylog-services      starts MongoDB, OpenSearch and the Graylog server: http://127.0.0.1:9000, admin/admin
#   graylog-test-hosts    adds the /etc/hosts entries the unit tests need offline (example.com)
#   /opt/graylog-tests-needing-docker.txt   unit tests that need Docker, as a surefire excludesFile
#
# Neither the build nor the unit tests use MongoDB or OpenSearch; they are here only so graylog-services can run a
# live server to test against, and threat_model.md puts them out of scope.
FROM eclipse-temurin:21-jdk-noble

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

ARG MONGODB_VERSION=8.0

ENV DEBIAN_FRONTEND=noninteractive \
    MAVEN_ARGS="--batch-mode --no-transfer-progress"

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl git gnupg procps \
    && rm -rf /var/lib/apt/lists/*

# MongoDB, Graylog's configuration and metadata store.
RUN curl -fsSL "https://pgp.mongodb.com/server-${MONGODB_VERSION}.asc" \
      | gpg --dearmor -o /usr/share/keyrings/mongodb-server.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/mongodb-server.gpg] https://repo.mongodb.org/apt/ubuntu noble/mongodb-org/${MONGODB_VERSION} multiverse" \
      > /etc/apt/sources.list.d/mongodb-org.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends mongodb-org-server \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /var/lib/mongodb /var/log/mongodb

# OpenSearch, the search backend, at the version the Data Node bundles. The min distribution has no security plugin,
# so it serves plain HTTP on localhost, as Graylog's own CI runs it. It cannot run as root.
COPY data-node/pom.xml /tmp/data-node-pom.xml
RUN version="$(sed -n 's:.*<opensearch.latest.version>\(.*\)</opensearch.latest.version>.*:\1:p' /tmp/data-node-pom.xml)" \
    && case "$(uname -m)" in x86_64) arch=x64 ;; aarch64) arch=arm64 ;; *) echo "unsupported: $(uname -m)" >&2; exit 1 ;; esac \
    && curl -fsSL --retry 3 --retry-all-errors \
         "https://artifacts.opensearch.org/releases/core/opensearch/${version}/opensearch-min-${version}-linux-${arch}.tar.gz" \
         -o /tmp/opensearch.tar.gz \
    && mkdir -p /opt/opensearch /var/lib/opensearch /var/log/opensearch \
    && tar -xzf /tmp/opensearch.tar.gz -C /opt/opensearch --strip-components=1 \
    && rm /tmp/opensearch.tar.gz /tmp/data-node-pom.xml \
    && printf '%s\n' 'cluster.name: graylog' 'node.name: scanner' 'network.host: 127.0.0.1' 'http.port: 9200' \
         'discovery.type: single-node' 'action.auto_create_index: false' \
         'path.data: /var/lib/opensearch' 'path.logs: /var/log/opensearch' \
         > /opt/opensearch/config/opensearch.yml \
    && useradd --system --home-dir /opt/opensearch --shell /usr/sbin/nologin opensearch \
    && chown -R opensearch:opensearch /opt/opensearch /var/lib/opensearch /var/log/opensearch

# Scanner contract: the checkout lives inside the image, at /src.
COPY . /src
WORKDIR /src

# Build and install every module the way the CI "Artifacts" job does, including the web interface and the server
# tarball. -DskipTests still compiles the test classes, so test code is available to the audit agent. Failsafe in
# graylog2-server still runs the *IT classes under -DskipTests; they need Docker (Testcontainers), which is not
# available during docker build, so -DskipITs skips them explicitly.
# The web interface's "check-production-build" step (bound to verify) opens the bundle in Puppeteer's Chrome, which
# has no linux-arm64 build and whose shared libraries this image lacks. It is a smoke test only, so the yarn script is
# stubbed for the build and package.json is restored afterwards, leaving /src as upstream has it.
RUN pkg=graylog2-web-interface/package.json \
    && cp "$pkg" /tmp/package.json.orig \
    && sed -i 's/"check-production-build": "[^"]*"/"check-production-build": "true"/' "$pkg" \
    && grep -q '"check-production-build": "true"' "$pkg" \
    && PUPPETEER_SKIP_DOWNLOAD=true MAVEN_OPTS="-Xmx6g" NODE_OPTIONS="--max-old-space-size=8192" \
       ./mvnw -Dspotbugs.skip -Dmaven.javadoc.skip=true -DskipTests -DskipITs install \
    && mv /tmp/package.json.orig "$pkg"

# Run the graylog2-server unit tests so the image is known to work and everything surefire needs is cached for
# offline use, but do not let a failure abort the build. The `test` phase stops before failsafe, so the *IT classes
# (which need Docker) do not run. Unit tests that start containers through Testcontainers (MongoDBExtension, MongoDB
# fixtures, Azurite) cannot run without Docker either; they are found by what their source refers to, listed in
# /opt/graylog-tests-needing-docker.txt and excluded. Some tests make a file unreadable and expect reading it to fail
# (HttpConfigurationTest.tlsValidationFailsIf*IsUnreadable, ConfigurationTest.testNodeIdFilePermissions); root reads
# it anyway through CAP_DAC_OVERRIDE and CAP_DAC_READ_SEARCH, so setpriv drops those two capabilities for the run. It
# stays root, so it can still write target/ and ~/.m2. The full output exceeds BuildKit's 2 MiB log limit, so it goes to
# /var/log/graylog-unit-tests.log and only Maven's results section is printed. Further tests, offline (see
# threat_model.md):
#   ./mvnw -o test -pl graylog2-server -Dskip.web.build=true -Dtest=<TestClass>
RUN cd graylog2-server/src/test/java \
    && grep -rlE 'MongoDBExtension|MongoDBTestService|MongoDBFixtures|org\.testcontainers|AzuriteContainer' \
         --include='*.java' . | sed 's|^\./||' | sort > /opt/graylog-tests-needing-docker.txt \
    && test -s /opt/graylog-tests-needing-docker.txt \
    && echo "Excluding $(wc -l < /opt/graylog-tests-needing-docker.txt) test sources that need Docker"
RUN { setpriv --bounding-set=-dac_override,-dac_read_search -- ./mvnw -pl graylog2-server -Dskip.web.build=true -Dspotbugs.skip -Dmaven.javadoc.skip=true \
        -Dsurefire.excludesFile=/opt/graylog-tests-needing-docker.txt test \
        > /var/log/graylog-unit-tests.log 2>&1 \
      || echo "WARNING: graylog2-server unit tests reported failures, see /var/log/graylog-unit-tests.log"; } \
    && { sed -n '/Results:/,$p' /var/log/graylog-unit-tests.log | cut -c1-300 || true; }

# --- runtime: the built server, configured against the local MongoDB and OpenSearch ---
RUN mkdir -p /opt/graylog /etc/graylog/server /var/lib/graylog /var/log/graylog \
    && tar -xzf "$(ls -t /src/target/assembly/graylog-*.tar.gz | head -1)" -C /opt/graylog \
    && printf '%s\n' \
         'is_leader = true' \
         'node_id_file = /var/lib/graylog/node-id' \
         "password_secret = $(head -c 48 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 64)" \
         'root_username = admin' \
         '# sha256 of "admin": a throwaway credential for this offline image' \
         'root_password_sha2 = 8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918' \
         'bin_dir = /opt/graylog/bin' \
         'data_dir = /var/lib/graylog' \
         'plugin_dir = /opt/graylog/plugin' \
         'message_journal_dir = /var/lib/graylog/journal' \
         'http_bind_address = 127.0.0.1:9000' \
         'elasticsearch_hosts = http://127.0.0.1:9200' \
         'mongodb_uri = mongodb://127.0.0.1:27017/graylog' \
         > /etc/graylog/server/server.conf

# Docker rewrites /etc/hosts when a container starts, so entries that offline containers lack are added at run time
# by the scripts below rather than at build time.
#
# graylog-test-hosts: HttpConfigurationTest.testHttpBindAddress* resolve example.com, which fails offline.
RUN printf '%s\n' '#!/bin/bash' \
      '# Adds the /etc/hosts entries the unit tests need with no network. Safe to run again.' \
      'set -euo pipefail' \
      'getent hosts example.com >/dev/null || echo "127.0.0.1 example.com" >> /etc/hosts' \
      > /usr/local/bin/graylog-test-hosts \
    && chmod +x /usr/local/bin/graylog-test-hosts

# graylog-services: starts MongoDB, OpenSearch and the Graylog server; the scanner runs no init system. OpenSearch
# needs a resolvable hostname.
RUN printf '%s\n' '#!/bin/bash' \
      '# Starts MongoDB, OpenSearch and the Graylog server built from /src. Safe to run again.' \
      'set -euo pipefail' \
      'getent hosts "$(hostname)" >/dev/null || echo "127.0.0.1 $(hostname)" >> /etc/hosts' \
      'pgrep -x mongod >/dev/null || mongod --fork --bind_ip 127.0.0.1 --dbpath /var/lib/mongodb --logpath /var/log/mongodb/mongod.log --wiredTigerCacheSizeGB 0.5' \
      'curl -sf http://127.0.0.1:9200 >/dev/null || su opensearch -s /bin/bash -c "cd /opt/opensearch && OPENSEARCH_JAVA_OPTS=\"-Xms1g -Xmx1g\" ./bin/opensearch -d -p /tmp/opensearch.pid"' \
      'for _ in $(seq 120); do curl -sf "http://127.0.0.1:9200/_cluster/health?wait_for_status=yellow&timeout=1s" >/dev/null && break; sleep 1; done' \
      'curl -sf http://127.0.0.1:9200 >/dev/null || { echo "OpenSearch did not start, see /var/log/opensearch/" >&2; exit 1; }' \
      'if ! curl -sf http://127.0.0.1:9000/api/system/lbstatus >/dev/null; then' \
      '  (cd /opt/graylog && nohup java -Dlog4j2.formatMsgNoLookups=true -Djdk.tls.acknowledgeCloseNotify=true -Xms1g -Xmx1g -XX:+UseG1GC -XX:-OmitStackTraceInFastThrow \' \
      '     -jar graylog.jar server -f /etc/graylog/server/server.conf -p /tmp/graylog.pid >> /var/log/graylog/server.log 2>&1 &)' \
      'fi' \
      'for _ in $(seq 300); do' \
      '  if curl -sf http://127.0.0.1:9000/api/system/lbstatus >/dev/null; then' \
      '    echo "Graylog is up: http://127.0.0.1:9000 (REST API under /api), user admin, password admin. Log: /var/log/graylog/server.log"; exit 0' \
      '  fi; sleep 1' \
      'done' \
      'echo "Graylog did not start, see /var/log/graylog/server.log" >&2; exit 1' \
      > /usr/local/bin/graylog-services \
    && chmod +x /usr/local/bin/graylog-services

WORKDIR /src

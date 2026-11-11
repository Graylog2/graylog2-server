# Build Graylog (github.com/Graylog2/graylog2-server) for Anthropic's OSS Scanner.
# The scanner uses the repository root as the build context and audits /src offline.
#
# Uses the same toolchain as the "Open Build" CI workflow (.github/workflows/build.yml): Temurin JDK 21 and the
# Maven wrapper. The frontend-maven-plugin installs the Node.js and Yarn versions pinned in pom.xml, so this file
# needs no version bumps when those change.
#
# What the finished image contains:
#   /src                  the full build: server, web interface (with node_modules), Data Node, storage modules
#   ~/.m2                 every dependency and plugin, so `./mvnw -o` works offline
#   graylog-test-hosts    adds the /etc/hosts entries the unit tests need offline (example.com)
#   /opt/graylog-tests-needing-docker.txt   unit tests that need Docker, as a surefire excludesFile
FROM eclipse-temurin:21-jdk-noble

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

ENV DEBIAN_FRONTEND=noninteractive \
    MAVEN_ARGS="--batch-mode --no-transfer-progress"

# The Maven wrapper downloads Maven with curl.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl git \
    && rm -rf /var/lib/apt/lists/*

# Scanner contract: the checkout lives inside the image, at /src.
COPY . /src
WORKDIR /src

# Compile and install every module, including the web interface (with node_modules, for its Jest tests) and the Data
# Node. `install` puts the module jars into ~/.m2, so a single module can be tested on its own (`-pl data-node` needs
# the graylog2-server jars). -DskipTests still compiles the test classes, so test code is available to the audit
# agent. Failsafe in graylog2-server still runs the *IT classes under -DskipTests; they need Docker (Testcontainers),
# which is not available during docker build, so -DskipITs skips them explicitly.
# Release packaging is left out because the audit does not use it: the OpenSearch builds the Data Node bundles (four
# downloads, used only for its package and integration tests), the server and Data Node tarballs, and the SBOMs.
# The web interface's "check-production-build" step (bound to verify) loads the built bundle in a headless Chrome to
# check that it starts. That is a release check for CI and adds nothing to a security audit, so it is left out on
# purpose: no browser is installed (PUPPETEER_SKIP_DOWNLOAD), the yarn script is stubbed for the build, and
# package.json is restored afterwards, leaving /src as upstream has it.
RUN pkg=graylog2-web-interface/package.json \
    && cp "$pkg" /tmp/package.json.orig \
    && sed -i 's/"check-production-build": "[^"]*"/"check-production-build": "true"/' "$pkg" \
    && grep -q '"check-production-build": "true"' "$pkg" \
    && PUPPETEER_SKIP_DOWNLOAD=true MAVEN_OPTS="-Xmx6g" NODE_OPTIONS="--max-old-space-size=8192" \
       ./mvnw -Dspotbugs.skip -Dmaven.javadoc.skip=true -DskipTests -DskipITs \
         -Dopensearch.unpack.compat.skip=true -Dopensearch.unpack.latest.skip=true \
         -Dassembly.skipAssembly=true -Dcyclonedx.skip install \
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

# graylog-test-hosts: HttpConfigurationTest.testHttpBindAddress* resolve example.com, which fails offline. Docker
# rewrites /etc/hosts when a container starts, so the entry is added at run time rather than at build time.
RUN printf '%s\n' '#!/bin/bash' \
      '# Adds the /etc/hosts entries the unit tests need with no network. Safe to run again.' \
      'set -euo pipefail' \
      'getent hosts example.com >/dev/null || echo "127.0.0.1 example.com" >> /etc/hosts' \
      > /usr/local/bin/graylog-test-hosts \
    && chmod +x /usr/local/bin/graylog-test-hosts

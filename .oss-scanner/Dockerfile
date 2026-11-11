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
#   graylog-test-hosts    adds the /etc/hosts entries the unit tests need offline
#   /opt/graylog-tests-needing-docker.txt   graylog2-server unit tests that need Docker, as a surefire excludesFile
FROM eclipse-temurin:21-jdk-noble

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

ENV DEBIAN_FRONTEND=noninteractive \
    MAVEN_ARGS="--batch-mode --no-transfer-progress"

# The build reads the commit through git (buildnumber-maven-plugin). The base image already has curl and CA
# certificates, which the Maven wrapper needs.
RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

# Scanner contract: the checkout lives inside the image, at /src.
COPY . /src
WORKDIR /src

# Compile and install every module, including the web interface (with node_modules, for its Jest tests) and the Data
# Node. `install` puts the module jars into ~/.m2, so one module can be tested on its own (`-pl data-node` needs the
# graylog2-server jars). -DskipTests still compiles the test classes. Failsafe still runs the *IT classes under
# -DskipTests, and they need Docker, so -DskipITs skips them explicitly.
# Release packaging is left out because the audit does not use it: the four OpenSearch builds the Data Node bundles,
# the server and Data Node tarballs, and the SBOMs. So is the web interface's "check-production-build" step, which
# loads the bundle in a headless Chrome: its yarn script is stubbed for the build (no browser is downloaded) and
# package.json is restored afterwards, leaving /src as upstream has it.
RUN pkg=graylog2-web-interface/package.json \
    && cp "$pkg" /tmp/package.json.orig \
    && sed -i 's/"check-production-build": "[^"]*"/"check-production-build": "true"/' "$pkg" \
    && grep -q '"check-production-build": "true"' "$pkg" \
    && PUPPETEER_SKIP_DOWNLOAD=true MAVEN_OPTS="-Xmx6g" NODE_OPTIONS="--max-old-space-size=8192" \
       ./mvnw -DskipTests -DskipITs \
         -Dopensearch.unpack.compat.skip=true -Dopensearch.unpack.latest.skip=true \
         -Dassembly.skipAssembly=true -Dcyclonedx.skip install \
    && mv /tmp/package.json.orig "$pkg"

# graylog-test-hosts: some unit tests resolve example.com (HttpConfigurationTest) or the container's own hostname
# (NetFlowCodecTest), which fails offline. Docker rewrites /etc/hosts when a container starts, so the entries are
# added at run time.
RUN printf '%s\n' '#!/bin/bash' \
      '# Adds the /etc/hosts entries the unit tests need with no network. Safe to run again.' \
      'set -euo pipefail' \
      'getent hosts example.com >/dev/null || echo "127.0.0.1 example.com" >> /etc/hosts' \
      'getent hosts "$(hostname)" >/dev/null || echo "127.0.0.1 $(hostname)" >> /etc/hosts' \
      > /usr/local/bin/graylog-test-hosts \
    && chmod +x /usr/local/bin/graylog-test-hosts

# Run the graylog2-server unit tests, so the image is known to work and everything surefire needs is cached for
# offline use, without failing the build on test failures. Tests that start containers through Testcontainers need
# Docker; they are found by what their source refers to and excluded. Some tests check that an unreadable file cannot
# be read; setpriv drops root's CAP_DAC_OVERRIDE and CAP_DAC_READ_SEARCH so they behave as for a normal user. The
# output exceeds BuildKit's 2 MiB log limit, so it goes to a file and only Maven's results are printed.
RUN cd graylog2-server/src/test/java \
    && grep -rlE 'MongoDBExtension|MongoDBTestService|MongoDBFixtures|org\.testcontainers|AzuriteContainer' \
         --include='*.java' . | sed 's|^\./||' | sort > /opt/graylog-tests-needing-docker.txt \
    && test -s /opt/graylog-tests-needing-docker.txt \
    && echo "Excluding $(wc -l < /opt/graylog-tests-needing-docker.txt) test sources that need Docker"
RUN { setpriv --bounding-set=-dac_override,-dac_read_search -- ./mvnw -pl graylog2-server -Dskip.web.build=true \
        -Dsurefire.excludesFile=/opt/graylog-tests-needing-docker.txt test \
        > /var/log/graylog-unit-tests.log 2>&1 \
      || echo "WARNING: graylog2-server unit tests reported failures, see /var/log/graylog-unit-tests.log"; } \
    && { sed -n '/Results:/,$p' /var/log/graylog-unit-tests.log | cut -c1-300 || true; }

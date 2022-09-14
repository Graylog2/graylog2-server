/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.testing.elasticsearch;

import com.google.common.io.Resources;
import org.graylog2.shared.utilities.StringUtils;
import org.graylog2.storage.SearchVersion;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * This rule starts an Elasticsearch instance and provides a configured {@link Client}.
 */
public abstract class TestableSearchServerInstance extends ExternalResource implements SearchServerInstance {
    private static final Logger LOG = LoggerFactory.getLogger(TestableSearchServerInstance.class);

    private static final Map<SearchVersion, GenericContainer<?>> containersByVersion = new HashMap<>();

    protected static final int ES_PORT = 9200;
    protected static final String NETWORK_ALIAS = "elasticsearch";

    private final SearchVersion version;
    private String heapSize;
    protected final GenericContainer<?> container;

    @Override
    public abstract Client client();

    @Override
    public abstract FixtureImporter fixtureImporter();

    protected TestableSearchServerInstance(String image, SearchVersion version, Network network, String heapSize) {
        this.version = version;
        this.heapSize = heapSize;
        this.container = createContainer(image, version, network);
    }

    @Override
    public GenericContainer<?> createContainer(String image, SearchVersion version, Network network) {
        if (!containersByVersion.containsKey(version)) {
            GenericContainer<?> container = buildContainer(image, network);
            container.start();
            if (LOG.isDebugEnabled()) {
                container.followOutput(new Slf4jLogConsumer(LOG));
            }
            containersByVersion.put(version, container);
        }
        return containersByVersion.get(version);
    }

    @Override
    public GenericContainer<?> buildContainer(String image, Network network) {
        return new ElasticsearchContainer(DockerImageName.parse(image).asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"))
                // Avoids reuse warning on Jenkins (we don't want reuse in our CI environment)
                .withReuse(isNull(System.getenv("BUILD_ID")))
                .withEnv("ES_JAVA_OPTS", getEsJavaOpts())
                .withEnv("discovery.type", "single-node")
                .withEnv("action.auto_create_index", "false")
                .withEnv("cluster.info.update.interval", "10s")
                .withNetwork(network)
                .withNetworkAliases(NETWORK_ALIAS)
                .waitingFor(Wait.forHttp("/").forPort(ES_PORT));
    }

    protected String getEsJavaOpts() {
        return StringUtils.f("-Xms%s -Xmx%s -Dlog4j2.formatMsgNoLookups=true", heapSize, heapSize);
    }

    @Override
    protected void after() {
        cleanUp();
    }

    @Override
    public void cleanUp() {
        client().cleanUp();
    }

    @Override
    public void close() {
        container.close();
        final List<SearchVersion> version = containersByVersion.keySet().stream().filter(k -> container == containersByVersion.get(k)).collect(Collectors.toList());
        version.forEach(containersByVersion::remove);
    }

    @Override
    public String internalUri() {
        return String.format(Locale.US, "http://%s:%d", NETWORK_ALIAS, ES_PORT);
    }

    @Override
    public SearchVersion version() {
        return version;
    }

    @Override
    public void importFixtureResource(String resourcePath, Class<?> testClass) {
        boolean isFullResourcePath = Paths.get(resourcePath).getNameCount() > 1;

        @SuppressWarnings("UnstableApiUsage")
        final URL fixtureResource = isFullResourcePath
                ? Resources.getResource(resourcePath)
                : Resources.getResource(testClass, resourcePath);

        fixtureImporter().importResource(fixtureResource);

        // Make sure the data we just imported is visible
        client().refreshNode();
    }


    @Override
    public String getHttpHostAddress() {
        return this.container.getHost() + ":" + this.container.getMappedPort(9200);
    }

}

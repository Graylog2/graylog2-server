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

import com.github.zafarkhaja.semver.Version;
import com.google.common.io.Resources;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * This rule starts an Elasticsearch instance and provides a configured {@link Client}.
 */
public abstract class ElasticsearchInstance extends ExternalResource {
    private static final Map<Version, ElasticsearchContainer> containersByVersion = new HashMap<>();

    private static final String DEFAULT_IMAGE_OSS = "docker.elastic.co/elasticsearch/elasticsearch-oss";

    private static final int ES_PORT = 9200;
    private static final String NETWORK_ALIAS = "elasticsearch";

    private final Version version;
    protected final ElasticsearchContainer container;

    protected abstract Client client();

    protected abstract FixtureImporter fixtureImporter();

    protected static String imageNameFrom(Version version) {
        return DEFAULT_IMAGE_OSS + ":" + version.toString();
    }

    protected ElasticsearchInstance(String image, Version version, Network network) {
        this.version = version;

        this.container = createContainer(image, version, network);
    }

    private ElasticsearchContainer createContainer(String image, Version version, Network network) {
        if (!containersByVersion.containsKey(version)) {
            ElasticsearchContainer container = buildContainer(image, network);
            container.start();
            containersByVersion.put(version, container);
        }
        return containersByVersion.get(version);
    }

    private ElasticsearchContainer buildContainer(String image, Network network) {
        return new ElasticsearchContainer(DockerImageName.parse(image).asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"))
                // Avoids reuse warning on Jenkins (we don't want reuse in our CI environment)
                .withReuse(isNull(System.getenv("BUILD_ID")))
                .withEnv("ES_JAVA_OPTS", "-Xms2g -Xmx2g")
                .withEnv("discovery.type", "single-node")
                .withEnv("action.auto_create_index", "false")
                .withEnv("cluster.info.update.interval", "10s")
                .withNetwork(network)
                .withNetworkAliases(NETWORK_ALIAS)
                .waitingFor(Wait.forHttp("/").forPort(ES_PORT));
    }

    @Override
    protected void after() {
        cleanUp();
    }

    public void cleanUp() {
        client().cleanUp();
    }

    public static String internalUri() {
        return String.format(Locale.US, "http://%s:%d", NETWORK_ALIAS, ES_PORT);
    }

    public Version version() {
        return version;
    }

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
}

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
package org.graylog.storage.opensearch2.testing;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableList;
import org.graylog.shaded.opensearch2.org.apache.http.impl.client.BasicCredentialsProvider;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog.storage.opensearch2.RestHighLevelClientProvider;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.elasticsearch.Adapters;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.FixtureImporter;
import org.graylog.testing.elasticsearch.TestableSearchServerInstance;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.storage.SearchVersion;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.opensearch.testcontainers.OpensearchContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import static java.util.Objects.isNull;

public class OpenSearchInstance extends TestableSearchServerInstance {
    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchInstance.class);

    public static final SearchServer OPENSEARCH_VERSION = SearchServer.DEFAULT_OPENSEARCH_VERSION;

    private final OpenSearchClient openSearchClient;
    private final Client client;
    private final FixtureImporter fixtureImporter;
    private final Adapters adapters;

    public OpenSearchInstance(final SearchVersion version, final Network network, final String heapSize, final List<String> featureFlags) {
        super(version, network, heapSize);

        RestHighLevelClient restHighLevelClient = buildRestClient();
        this.openSearchClient = new OpenSearchClient(restHighLevelClient, false, new ObjectMapperProvider().get());
        this.client = new ClientOS2(this.openSearchClient, featureFlags);
        this.fixtureImporter = new FixtureImporterOS2(this.openSearchClient);
        adapters = new AdaptersOS2(openSearchClient);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public static OpenSearchInstance create() {
        return OpenSearchInstanceBuilder.builder().instantiate();
    }

    @Override
    protected String imageName() {
        return String.format(Locale.ROOT, "opensearchproject/opensearch:%s", version().version());
    }

    @Override
    public SearchServer searchServer() {
        return OPENSEARCH_VERSION;
    }

    private RestHighLevelClient buildRestClient() {
        return new RestHighLevelClientProvider(
                new GracefulShutdownService(),
                ImmutableList.of(URI.create("http://" + this.getHttpHostAddress())),
                Duration.seconds(60),
                Duration.seconds(60),
                Duration.seconds(60),
                1,
                1,
                1,
                false,
                false,
                null,
                Duration.seconds(60),
                "http",
                false,
                false,
                new BasicCredentialsProvider(),
                null,
                false,
                false,
                "maybe_want_jwt_here")
                .get();
    }

    @Override
    public Client client() {
        return this.client;
    }

    @Override
    public FixtureImporter fixtureImporter() {
        return this.fixtureImporter;
    }

    public OpenSearchClient openSearchClient() {
        return this.openSearchClient;
    }

    @Override
    public GenericContainer<?> buildContainer(String image, Network network) {
        var container = new OpensearchContainer(DockerImageName.parse(image))
                // Avoids reuse warning on Jenkins (we don't want reuse in our CI environment)
                .withReuse(isNull(System.getenv("BUILD_ID")))
                .withEnv("OPENSEARCH_JAVA_OPTS", getEsJavaOpts())
                .withEnv("cluster.info.update.interval", "10s")
                .withEnv("cluster.routing.allocation.disk.reroute_interval", "5s")
                .withEnv("action.auto_create_index", "false")
                .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
                .withEnv("START_PERF_ANALYZER", "false")
                .withNetwork(network)
                .withNetworkAliases(NETWORK_ALIAS);

        // disabling the performance plugin in 2.0.1 consistently created errors during CI runs, but keeping it running
        // in later versions sometimes created errors on CI, too.
        if(version().satisfies(SearchVersion.Distribution.OPENSEARCH, "^2.7.0")) {
            return container.withCommand("sh", "-c", "opensearch-plugin remove opensearch-performance-analyzer && ./opensearch-docker-entrypoint.sh");
        } else {
            return container;
        }
    }

    @Override
    public Adapters adapters() {
        return this.adapters;
    }

    @Override
    public String getLogs() {
        return this.container.getLogs();
    }
}

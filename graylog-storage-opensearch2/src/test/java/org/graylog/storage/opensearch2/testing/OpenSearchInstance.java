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
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableList;
import org.graylog.shaded.opensearch2.org.apache.http.impl.client.BasicCredentialsProvider;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog.storage.opensearch2.RestHighLevelClientProvider;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.FixtureImporter;
import org.graylog.testing.elasticsearch.TestableSearchServerInstance;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.storage.SearchVersion;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.Locale;

import static java.util.Objects.isNull;

public class OpenSearchInstance extends TestableSearchServerInstance {
    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchInstance.class);

    protected static final String OS_VERSION = "2.0.1";
    public static final String DEFAULT_HEAP_SIZE = "2g";

    private final RestHighLevelClient restHighLevelClient;
    private final OpenSearchClient openSearchClient;
    private final Client client;
    private final FixtureImporter fixtureImporter;

    protected OpenSearchInstance(String image, SearchVersion version, Network network, String heapSize) {
        super(image, version, network, heapSize);
        this.restHighLevelClient = buildRestClient();
        this.openSearchClient = new OpenSearchClient(this.restHighLevelClient, false, new ObjectMapperProvider().get());
        this.client = new ClientOS2(this.openSearchClient);
        this.fixtureImporter = new FixtureImporterOS2(this.openSearchClient);
    }
    protected OpenSearchInstance(String image, SearchVersion version, Network network) {
        this(image, version, network, DEFAULT_HEAP_SIZE);
    }

    @Override
    public SearchServer searchServer() {
        return SearchServer.OS2;
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
                null,
                Duration.seconds(60),
                "http",
                false,
                false,
                new BasicCredentialsProvider())
                .get();
    }

    public static OpenSearchInstance create() {
        return create(SearchVersion.opensearch(OS_VERSION), Network.newNetwork(), DEFAULT_HEAP_SIZE);
    }

    public static OpenSearchInstance create(String heapSize) {
        return create(SearchVersion.opensearch(OS_VERSION), Network.newNetwork(), heapSize);
    }

    // Caution, do not change this signature. It's required by our container matrix tests. See SearchServerInstanceFactoryByVersion
    public static OpenSearchInstance create(SearchVersion searchVersion, Network network) {
        return create(searchVersion, network, DEFAULT_HEAP_SIZE);
    }

    private static OpenSearchInstance create(SearchVersion searchVersion, Network network, String heapSize) {
        final String image = imageNameFrom(searchVersion.version());

        LOG.debug("Creating instance {}", image);

        return new OpenSearchInstance(image, searchVersion, network, heapSize);
    }

    protected static String imageNameFrom(Version version) {
        return String.format(Locale.ROOT, "opensearchproject/opensearch:%s", version.toString());
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

    public RestHighLevelClient restHighLevelClient() {
        return this.restHighLevelClient;
    }

    @Override
    public GenericContainer<?> buildContainer(String image, Network network) {
        return new OpensearchContainer(DockerImageName.parse(image))
                // Avoids reuse warning on Jenkins (we don't want reuse in our CI environment)
                .withReuse(isNull(System.getenv("BUILD_ID")))
                .withEnv("OPENSEARCH_JAVA_OPTS", getEsJavaOpts())
                .withEnv("discovery.type", "single-node")
                .withEnv("action.auto_create_index", "false")
                .withEnv("plugins.security.ssl.http.enabled", "false")
                .withEnv("plugins.security.disabled", "true")
                .withEnv("action.auto_create_index", "false")
                .withEnv("cluster.info.update.interval", "10s")
                .withNetwork(network)
                .withNetworkAliases(NETWORK_ALIAS)
                .waitingFor(Wait.forHttp("/").forPort(ES_PORT));
    }
}

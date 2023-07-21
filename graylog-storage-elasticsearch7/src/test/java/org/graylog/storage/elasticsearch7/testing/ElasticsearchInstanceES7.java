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
package org.graylog.storage.elasticsearch7.testing;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableList;
import org.graylog.shaded.elasticsearch7.org.apache.http.impl.client.BasicCredentialsProvider;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.RestHighLevelClientProvider;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.elasticsearch.Adapters;
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
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.List;

import static java.util.Objects.isNull;

public class ElasticsearchInstanceES7 extends TestableSearchServerInstance {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchInstanceES7.class);
    private static final String DEFAULT_IMAGE_OSS = "docker.elastic.co/elasticsearch/elasticsearch-oss";

    private final RestHighLevelClient restHighLevelClient;
    private final ElasticsearchClient elasticsearchClient;
    private final Client client;
    private final FixtureImporter fixtureImporter;
    private Adapters adapters;

    public ElasticsearchInstanceES7(final SearchVersion version, final Network network, final String heapSize, final List<String> featureFlags) {
        super(DEFAULT_IMAGE_OSS + ":" + version.version(), version, network, heapSize, featureFlags);
        LOG.debug("Creating instance {}", DEFAULT_IMAGE_OSS + ":" + version.version());
        // stop the instance when the JVM shuts down. Otherwise, they will keep running forever and slowly eat the whole machine
        this.restHighLevelClient = buildRestClient();
        this.elasticsearchClient = new ElasticsearchClient(this.restHighLevelClient, false, new ObjectMapperProvider().get());
        this.client = new ClientES7(this.elasticsearchClient);
        this.fixtureImporter = new FixtureImporterES7(this.elasticsearchClient);
        this.adapters = new AdaptersES7(elasticsearchClient);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public static ElasticsearchInstanceES7 create() {
        return Elasticsearch7InstanceBuilder.builder().build();
    }

    public static ElasticsearchInstanceES7 create(final String heapSize) {
        return (ElasticsearchInstanceES7) Elasticsearch7InstanceBuilder.builder().heapSize(heapSize).build();
    }

    @Override
    public SearchServer searchServer() {
        return SearchServer.ES7;
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
                new BasicCredentialsProvider())
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

    @Override
    public Adapters adapters() {
        return this.adapters;
    }

    @Override
    public String getLogs() {
        return this.container.getLogs();
    }

    public ElasticsearchClient elasticsearchClient() {
        return this.elasticsearchClient;
    }

    public RestHighLevelClient restHighLevelClient() {
        return this.restHighLevelClient;
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
                .waitingFor(Wait.forHttp("/").forPort(OPENSEARCH_PORT));
    }
}

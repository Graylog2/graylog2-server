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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import static java.util.Objects.isNull;

public class DatanodeInstance extends TestableSearchServerInstance {
    public static final SearchServer DATANODE_VERSION = SearchServer.DATANODE_DEV;

    private final OpenSearchClient openSearchClient;
    private final Client client;
    private final FixtureImporter fixtureImporter;
    private final Adapters adapters;

    public DatanodeInstance(final SearchVersion version, final Network network, final String heapSize, final List<String> featureFlags) {
        super(version, network, heapSize);

        RestHighLevelClient restHighLevelClient = buildRestClient();
        this.openSearchClient = new OpenSearchClient(restHighLevelClient, false, new ObjectMapperProvider().get());
        this.client = new ClientOS2(this.openSearchClient, featureFlags);
        this.fixtureImporter = new FixtureImporterOS2(this.openSearchClient);
        adapters = new AdaptersOS2(openSearchClient);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    @Override
    protected String imageName() {
        return String.format(Locale.ROOT, "graylog/graylog-datanode:%s", "5.2-dev");
    }

    @Override
    public SearchServer searchServer() {
        return DATANODE_VERSION;
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
                null)
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
        return new GenericContainer<>(DockerImageName.parse(image))
                .withImagePullPolicy(PullPolicy.alwaysPull())
                // Avoids reuse warning on Jenkins (we don't want reuse in our CI environment)
                .withReuse(isNull(System.getenv("BUILD_ID")))
                .withEnv("OPENSEARCH_JAVA_OPTS", getEsJavaOpts())
                .withEnv("GRAYLOG_DATANODE_PASSWORD_SECRET", "<password-secret>")
                .withEnv("GRAYLOG_DATANODE_ROOT_PASSWORD_SHA2", "<root-pw-sha2>")
                .withEnv("GRAYLOG_DATANODE_MONGODB_URI", "mongodb://mongodb:27017/graylog")
                .withEnv("GRAYLOG_DATANODE_SINGLE_NODE_ONLY", "true")
                .withEnv("GRAYLOG_DATANODE_CONFIG_LOCATION", "/usr/share/graylog/datanode/bin/config")
                .withEnv("GRAYLOG_DATANODE_INSECURE_STARTUP", "true")
                .withEnv("GRAYLOG_DATANODE_OPENSEARCH_LOCATION", "/usr/share/graylog/datanode/dist")
                .withExposedPorts(9200, 9300)
                .withNetwork(network)
                .withNetworkAliases(NETWORK_ALIAS)
                .waitingFor(Wait.forHttp("/_cluster/health").forPort(OPENSEARCH_PORT).forStatusCode(200).forResponsePredicate(s -> s.contains("\"status\":\"green\"")));
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

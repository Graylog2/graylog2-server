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
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.ImmutableList;
import org.graylog.shaded.opensearch2.org.apache.http.impl.client.BasicCredentialsProvider;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.IndicesOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.GetIndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.GetIndexResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.PutComposableIndexTemplateRequest;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.ComposableIndexTemplate;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.Template;
import org.graylog.shaded.opensearch2.org.opensearch.common.settings.Settings;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog.storage.opensearch2.OpenSearchClientProvider;
import org.graylog.storage.opensearch2.RestClientProvider;
import org.graylog.storage.opensearch2.RestHighLevelClientProvider;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.elasticsearch.Adapters;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.FixtureImporter;
import org.graylog.testing.elasticsearch.TestableSearchServerInstance;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.storage.SearchVersion;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpensearchContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;

public class OpenSearchInstance extends TestableSearchServerInstance {
    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchInstance.class);

    public static final SearchServer OPENSEARCH_VERSION = SearchServer.DEFAULT_OPENSEARCH_VERSION;

    private OpenSearchClient openSearchClient;
    private Client client;
    private FixtureImporter fixtureImporter;
    private Adapters adapters;
    private List<String> featureFlags;

    public OpenSearchInstance(final SearchVersion version, final String hostname, final Network network, final String heapSize, final List<String> featureFlags) {
        super(version, hostname, network, heapSize);
        this.featureFlags = featureFlags;
    }

    @Override
    public OpenSearchInstance init() {
        super.init();
        RestHighLevelClient restHighLevelClient = buildRestHighLevelClient();
        final var objectMapper = new ObjectMapperProvider().get();
        final var restClient = buildRestClient();
        this.openSearchClient = new OpenSearchClient(restHighLevelClient, new OpenSearchClientProvider(restClient, objectMapper).get(), objectMapper);
        this.client = new ClientOS2(this.openSearchClient, featureFlags);
        this.fixtureImporter = new FixtureImporterOS2(this.openSearchClient);
        adapters = new AdaptersOS2(openSearchClient);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        if (isFirstContainerStart) {
            afterContainerCreated();
        }
        return this;
    }

    private RestClient buildRestClient() {
        return new RestClientProvider(
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
                false,
                new org.apache.http.impl.client.BasicCredentialsProvider(),
                null,
                false,
                false,
                null)
                .get();
    }

    public static OpenSearchInstance create() {
        return OpenSearchInstanceBuilder.builder().instantiate();
    }

    protected void afterContainerCreated() {
        if (version().satisfies(SearchVersion.Distribution.OPENSEARCH, "2.9.0")) {
            fixNumberOfReplicaForMlPlugin();
        }
        if (version().isOpenSearch()) {
            fixDefaultNumberOfReplicasForIsmConfigs();
        }
    }

    private void fixDefaultNumberOfReplicasForIsmConfigs() {
        PutComposableIndexTemplateRequest request = new PutComposableIndexTemplateRequest();
        request.name("ism-zero-replica-template");
        request.indexTemplate(new ComposableIndexTemplate(List.of(".opendistro-ism-config"),
                new Template(Settings.builder().put("number_of_replicas", 0).build(), null, null),
                null, Long.MAX_VALUE, null, null));
        openSearchClient().execute((client, requestOptions) -> client.indices().putIndexTemplate(request, requestOptions));
    }

    /**
     * TODO: this is a workaround and should be removed once we can configure the default number of replicas for ML and SAP plugins or
     * opensearch fixes their code so it sets number_of_replicas to 0 when the cluster is running in the single-node mode.
     *
     * @see <a href="https://github.com/opensearch-project/security/issues/3130">https://github.com/opensearch-project/security/issues/3130</a>
     * When opensearch starts in a single-node mode, the ML and security analysis plugins still configure its
     * indices to require replicas. We can't change this behaviour currently, so we have to adapt the setting
     * after the indices are created and before the tests can start.
     */
    private void fixNumberOfReplicaForMlPlugin() {
        openSearchClient().execute((client, requestOptions) -> {
            try {
                waitForIndices(client, requestOptions, ".plugins-ml-config");
            } catch (ExecutionException | RetryException e) {
                throw new RuntimeException("Failed to wait for indices", e);
            }

            final UpdateSettingsRequest req = new UpdateSettingsRequest().indices(".plugins-ml-config", ".opensearch-sap-pre-packaged-rules-config", ".opensearch-sap-log-types-config");
            req.settings(Map.of("number_of_replicas", "0"));
            return client.indices().putSettings(req, requestOptions);
        });
    }

    private GetIndexResponse waitForIndices(RestHighLevelClient client, RequestOptions requestOptions, String... indices) throws ExecutionException, RetryException {
        return RetryerBuilder.<GetIndexResponse>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(30))
                .retryIfResult(input -> !new HashSet<>(Arrays.asList(input.getIndices())).containsAll(Arrays.asList(indices)))
                .build()
                .call(() -> {
                    final GetIndexRequest request = new GetIndexRequest("*");
                    request.indicesOptions(IndicesOptions.lenientExpandOpen());
                    return client.indices().get(request, requestOptions);
                });
    }

    @Override
    protected String imageName() {
        return String.format(Locale.ROOT, "opensearchproject/opensearch:%s", version().version());
    }

    @Override
    public SearchServer searchServer() {
        return OPENSEARCH_VERSION;
    }

    private RestHighLevelClient buildRestHighLevelClient() {
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
        var container = new OpensearchContainer(DockerImageName.parse(image))
                // Avoids reuse warning on Jenkins (we don't want reuse in our CI environment)
                .withReuse(isNull(System.getenv("CI")))
                .withEnv("OPENSEARCH_JAVA_OPTS", getEsJavaOpts())
                .withEnv("cluster.info.update.interval", "10s")
                .withEnv("cluster.routing.allocation.disk.reroute_interval", "5s")
                .withEnv("action.auto_create_index", "false")
                .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
                .withEnv("START_PERF_ANALYZER", "false")
                .withNetwork(network)
                .withNetworkAliases(hostname);

        // disabling the performance plugin in 2.0.1 consistently created errors during CI runs, but keeping it running
        // in later versions sometimes created errors on CI, too.
        if (version().satisfies(SearchVersion.Distribution.OPENSEARCH, "^2.7.0")) {
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

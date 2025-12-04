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
package org.graylog.storage.opensearch3.testing;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import org.graylog.shaded.opensearch2.org.apache.http.auth.AuthScope;
import org.graylog.shaded.opensearch2.org.apache.http.auth.UsernamePasswordCredentials;
import org.graylog.shaded.opensearch2.org.apache.http.impl.client.BasicCredentialsProvider;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.IndicesOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.GetIndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.GetIndexResponse;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.OfficialOpensearchClientProvider;
import org.graylog.storage.opensearch3.OpenSearchClient;
import org.graylog.storage.opensearch3.RestClientProvider;
import org.graylog.testing.elasticsearch.Adapters;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.ContainerCacheKey;
import org.graylog.testing.elasticsearch.FixtureImporter;
import org.graylog.testing.elasticsearch.TestableSearchServerInstance;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.graylog2.security.TrustAllX509TrustManager;
import org.graylog2.security.TrustManagerAndSocketFactoryProvider;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.storage.SearchVersion;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OpenSearchInstance extends TestableSearchServerInstance {

    private static final String DEFAULT_INITIAL_PASSWORD = "_ad0m#Ns_";

    private final OpensearchSecurity security;

    @Deprecated
    private OpenSearchClient openSearchClient;
    private OfficialOpensearchClient officialOpensearchClient;
    private Client client;
    private FixtureImporter fixtureImporter;
    private Adapters adapters;
    private List<String> featureFlags;
    private static Map<ContainerCacheKey, OpenSearchClient> openSearchClients = new HashMap<>();
    private static Map<ContainerCacheKey, OfficialOpensearchClient> officialOpensearchClients = new HashMap<>();

    public OpenSearchInstance(final boolean cachedInstance, final SearchVersion version, final String hostname, final Network network, final String heapSize, final List<String> featureFlags) {
        this(cachedInstance, version, hostname, network, heapSize, featureFlags, Map.of(), OpensearchSecurity.DISABLED);
    }

    public OpenSearchInstance(final boolean cachedInstance, final SearchVersion version, final String hostname, final Network network, final String heapSize, final List<String> featureFlags, Map<String, String> env, OpensearchSecurity security) {
        super(cachedInstance, version, hostname, network, heapSize, env);
        this.featureFlags = featureFlags;
        this.security = security;
    }

    @Override
    public OpenSearchInstance init() {
        super.init();
        if (Objects.nonNull(cacheKey)) {
            if (openSearchClients.containsKey(cacheKey)) {
                this.openSearchClient = openSearchClients.get(cacheKey);
                this.officialOpensearchClient = officialOpensearchClients.get(cacheKey);
            } else {
                this.openSearchClient = buildClient();
                this.officialOpensearchClient = buildOfficialClient();
                openSearchClients.put(cacheKey, openSearchClient);
                officialOpensearchClients.put(cacheKey, officialOpensearchClient);
            }
        } else {
            this.openSearchClient = buildClient();
            this.officialOpensearchClient = buildOfficialClient();
        }
        this.client = new ClientOS(officialOpensearchClient, featureFlags);
        this.fixtureImporter = new FixtureImporterOS2(this.openSearchClient);
        adapters = new AdaptersOS(openSearchClient, officialOpensearchClient, featureFlags);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        if (isFirstContainerStart) {
            afterContainerCreated();
        }
        return this;
    }

    @Override
    public void close() {
        super.close();
        if (officialOpensearchClient != null) {
            officialOpensearchClient.close();
        }
        openSearchClients.remove(cacheKey);
        officialOpensearchClients.remove(cacheKey);
    }

    private OpenSearchClient buildClient() {
        RestHighLevelClient restHighLevelClient = buildRestClient();
        return new OpenSearchClient(restHighLevelClient, new ObjectMapperProvider().get());
    }

    private OfficialOpensearchClient buildOfficialClient() {
        final String protocol = isSecuredInstance() ? "https://" : "http://";
        return new OfficialOpensearchClientProvider(
                ImmutableList.of(URI.create(protocol + this.getHttpHostAddress())),
                IndexerJwtAuthToken.disabled(),
                createCredentialsProvider(),
                getElasticsearchClientConfiguration()
        ).get();
    }

    private org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider createCredentialsProvider() {
        final org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider credentialsProvider = new org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider();
        if(isSecuredInstance()) {
            final org.apache.hc.client5.http.auth.AuthScope anyScope = new org.apache.hc.client5.http.auth.AuthScope(null, -1);
            credentialsProvider.setCredentials(anyScope, new org.apache.hc.client5.http.auth.UsernamePasswordCredentials("admin", DEFAULT_INITIAL_PASSWORD.toCharArray()));
        }
        return credentialsProvider;
    }

    @Deprecated
    private RestHighLevelClient buildRestClient() {

        final ElasticsearchClientConfiguration config = getElasticsearchClientConfiguration();

        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        if (isSecuredInstance()) {
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", DEFAULT_INITIAL_PASSWORD));
        }

        final String protocol = isSecuredInstance() ? "https://" : "http://";

        final TrustManagerAndSocketFactoryProvider trustFactory = getTrustManagerAndSocketFactoryProvider();
        return new RestClientProvider(
                new GracefulShutdownService(),
                ImmutableList.of(URI.create(protocol + this.getHttpHostAddress())),
                config,
                credentialsProvider,
                trustFactory,
                IndexerJwtAuthToken.disabled(),
                Collections.emptySet(),
                Collections.emptySet())
                .get();
    }

    private boolean isSecuredInstance() {
        return security == OpensearchSecurity.ENABLED;
    }

    @Nullable
    private TrustManagerAndSocketFactoryProvider getTrustManagerAndSocketFactoryProvider() {
        if (isSecuredInstance()) {
            try {
                return new TrustManagerAndSocketFactoryProvider(new TrustAllX509TrustManager());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    private ElasticsearchClientConfiguration getElasticsearchClientConfiguration() {
        return buildconfig(Map.of(
                "elasticsearch_connect_timeout", "60s",
                "elasticsearch_socket_timeout", "60s",
                "elasticsearch_max_total_connections", "1",
                "elasticsearch_max_total_connections_per_route", "1",
                "elasticsearch_use_expect_continue", "false"
        ));
    }

    private ElasticsearchClientConfiguration buildconfig(Map<String, String> properties) {
        final ElasticsearchClientConfiguration opensearchConfig = new ElasticsearchClientConfiguration();
        final JadConfig config = new JadConfig(new InMemoryRepository(properties), opensearchConfig);
        try {
            config.process();
            return opensearchConfig;
        } catch (RepositoryException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    public static OpenSearchInstance create() {
        return OpenSearchInstanceBuilder.builder().instantiate();
    }

    public static OpenSearchInstance createSecured() {
        final OpenSearchInstanceBuilder builder = OpenSearchInstanceBuilder.builder().withEnabledSecurity();
        // TODO: without disabling caching, we get an unsecured instance. The cache is not differentiating based on configuration of the container
        builder.cachedInstance(false);
        return builder.instantiate();
    }

    protected void afterContainerCreated() {
        if (version().satisfies(SearchVersion.Distribution.OPENSEARCH, "2.9.0")) {
            fixNumberOfReplicaForMlPlugin();
        }
        if (version().satisfies(SearchVersion.Distribution.OPENSEARCH, "2.19.3")) {
            fixDefaultNumberOfReplicasForIsmConfigs();
        }
    }

    private void fixDefaultNumberOfReplicasForIsmConfigs() {
        // changes default number of replicas for some system managed indices in 2.19
        // (see http://github.com/opensearch-project/OpenSearch/issues/9438)
        openSearchClient().execute((client, requestOptions) -> {
            final ClusterUpdateSettingsRequest req = new ClusterUpdateSettingsRequest();
            req.persistentSettings(Map.of("cluster.default_number_of_replicas", "0"));
            return client.cluster().putSettings(req, requestOptions);
        });
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
    public Client client() {
        return this.client;
    }

    @Override
    public FixtureImporter fixtureImporter() {
        return this.fixtureImporter;
    }

    @Deprecated
    public OpenSearchClient openSearchClient() {
        return this.openSearchClient;
    }

    public OfficialOpensearchClient getOfficialOpensearchClient() {
        return officialOpensearchClient;
    }

    @Override
    public GenericContainer<?> buildContainer(String image, Network network) {
        OpenSearchContainer container = new OpenSearchContainer(DockerImageName.parse(image));

        container.withEnv("OPENSEARCH_JAVA_OPTS", getEsJavaOpts())
                .withEnv("cluster.info.update.interval", "10s")
                .withEnv("cluster.routing.allocation.disk.reroute_interval", "5s")
                .withEnv("action.auto_create_index", "false")
                .withEnv("START_PERF_ANALYZER", "false")
                .withNetwork(network)
                .withNetworkAliases(hostname);

        if (isSecuredInstance()) {
            container.withSecurityEnabled();
            container.withEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", DEFAULT_INITIAL_PASSWORD);
        } else {
            container.withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true");
        }

        getContainerEnv().forEach(container::withEnv);

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

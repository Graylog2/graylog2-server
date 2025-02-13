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
package org.graylog.datanode.opensearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.inject.Inject;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.exec.ExecuteException;
import org.apache.http.client.utils.URIBuilder;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.opensearch.cli.OpensearchCommandLineProcess;
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;
import org.graylog.datanode.opensearch.rest.OpensearchRestClient;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;
import org.graylog.datanode.periodicals.ClusterStateResponse;
import org.graylog.datanode.process.ProcessInformation;
import org.graylog.datanode.process.ProcessListener;
import org.graylog.shaded.opensearch2.org.opensearch.OpenSearchStatusException;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.ClusterClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.common.settings.Settings;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog2.datanode.DataNodeLifecycleEvent;
import org.graylog2.datanode.DataNodeLifecycleTrigger;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.CustomCAX509TrustManager;
import org.graylog2.security.TrustManagerAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OpensearchProcessImpl implements OpensearchProcess, ProcessListener {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessImpl.class);


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<OpensearchConfiguration> opensearchConfiguration = Optional.empty();
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<RestHighLevelClient> restClient = Optional.empty();
    private Optional<OpenSearchClient> openSearchClient = Optional.empty();

    private final OpensearchStateMachine processState;

    private final DatanodeConfiguration datanodeConfiguration;

    private OpensearchCommandLineProcess commandLineProcess;

    private final Queue<String> stdout;
    private final Queue<String> stderr;
    private final CustomCAX509TrustManager trustManager;
    private final Configuration configuration;
    private final ObjectMapper objectMapper;
    private final String nodeName;
    private final NodeId nodeId;
    private final EventBus eventBus;


    static final String CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING = "cluster.routing.allocation.exclude._name";
    boolean allocationExcludeChecked = false;
    ScheduledExecutorService executorService;

    @Inject
    OpensearchProcessImpl(DatanodeConfiguration datanodeConfiguration, final CustomCAX509TrustManager trustManager,
                          final Configuration configuration,
                          ObjectMapper objectMapper, OpensearchStateMachine processState, NodeId nodeId, EventBus eventBus) {
        this.datanodeConfiguration = datanodeConfiguration;
        this.processState = processState;
        this.stdout = new CircularFifoQueue<>(datanodeConfiguration.processLogsBufferSize());
        this.stderr = new CircularFifoQueue<>(datanodeConfiguration.processLogsBufferSize());
        this.trustManager = trustManager;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.nodeName = configuration.getDatanodeNodeName();
        this.nodeId = nodeId;
        this.eventBus = eventBus;
    }

    private RestHighLevelClient createRestClient(OpensearchConfiguration configuration) {

        final TrustManager trustManager = createAggregatedTrustManager(configuration.trustStore());

        return OpensearchRestClient.build(configuration, datanodeConfiguration, trustManager);
    }

    /**
     * We have to combine the system-wide trust manager with a manager that trusts certificates used to secure
     * the datanode's opensearch process.
     *
     * @param truststore truststore containing certificates used to secure datanode's opensearch
     * @return combined trust manager
     */
    @Nonnull
    private X509TrustManager createAggregatedTrustManager(KeyStore truststore) {
        return new TrustManagerAggregator(List.of(this.trustManager, TrustManagerAggregator.trustManagerFromKeystore(truststore)));
    }

    @Override
    public List<String> stdOutLogs() {
        return stdout.stream().toList();
    }

    @Override
    public List<String> stdErrLogs() {
        return stderr.stream().toList();
    }

    public Optional<RestHighLevelClient> restClient() {
        return restClient;
    }

    public Optional<OpenSearchClient> openSearchClient() {
        return openSearchClient;
    }

    public OpensearchInfo processInfo() {
        return new OpensearchInfo(configuration.getDatanodeNodeName(), processState.getState(), getOpensearchBaseUrl().toString(), commandLineProcess != null ? commandLineProcess.processInfo() : ProcessInformation.empty());
    }

    @Override
    public URI getOpensearchBaseUrl() {
        final String baseUrl = opensearchConfiguration.map(OpensearchConfiguration::getRestBaseUrl)
                .map(httpHost -> new URIBuilder()
                        .setHost(httpHost.getHostName())
                        .setPort(httpHost.getPort())
                        .setScheme(httpHost.getSchemeName()).toString())
                .orElse(""); // Empty address will cause problems for opensearch clients. Has to be filtered out in IndexerDiscoveryProvider
        return URI.create(baseUrl);
    }

    @Override
    public String getOpensearchClusterUrl() {
        return configuration.getDatanodeNodeName() + ":" + configuration.getOpensearchTransportPort();
    }

    @Override
    public String getDatanodeRestApiUrl() {
        final boolean secured = opensearchConfiguration.flatMap(OpensearchConfiguration::httpCertificate).isPresent();
        String protocol = secured ? "https" : "http";
        String host = configuration.getHostname();
        final int port = configuration.getDatanodeHttpPort();
        return String.format(Locale.ROOT, "%s://%s:%d", protocol, host, port);
    }

    public void onEvent(OpensearchEvent event) {
        LOG.debug("Process event: " + event);
        this.processState.fire(event);
    }

    public boolean isInState(OpensearchState expectedState) {
        return this.processState.getState().equals(expectedState);
    }

    @Override
    public void configure(OpensearchConfiguration configuration) {
        this.opensearchConfiguration = Optional.of(configuration);
        configure();
    }

    private void configure() {
        opensearchConfiguration.ifPresentOrElse(
                (config -> {
                    // refresh TM if the SSL certs changed
                    trustManager.refresh();
                }),
                () -> {throw new IllegalArgumentException("Opensearch configuration required but not supplied!");}
        );
    }

    @Override
    public synchronized void start() {
        opensearchConfiguration.ifPresentOrElse(
                (config -> {
                    boolean startedPreviously = Objects.nonNull(commandLineProcess) && commandLineProcess.processInfo().alive();
                    if (startedPreviously) {
                        stop();
                    }

                    commandLineProcess = new OpensearchCommandLineProcess(config, this);
                    commandLineProcess.start();

                    restClient = Optional.of(createRestClient(config));
                    openSearchClient = restClient.map(c -> new OpenSearchClient(c, objectMapper));
                }),
                () -> {throw new IllegalArgumentException("Opensearch configuration required but not supplied!");}
        );
    }

    /**
     * reset allocation exclude status on restart to allow removed nodes to rejoin the cluster
     */
    private void checkAllocationEnabledStatus() {
        if (restClient().isPresent()) {
            ClusterClient clusterClient = restClient().get().cluster();
            try {
                final ClusterGetSettingsResponse settings =
                        clusterClient.getSettings(new ClusterGetSettingsRequest(), RequestOptions.DEFAULT);
                final String setting = settings.getSetting(CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING);
                if (nodeName.equals(setting)) {
                    ClusterUpdateSettingsRequest updateSettings = new ClusterUpdateSettingsRequest();
                    updateSettings.transientSettings(Settings.builder()
                            .putNull(CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING)
                            .build());
                    clusterClient.putSettings(updateSettings, RequestOptions.DEFAULT);
                }
                allocationExcludeChecked = true;
            } catch (IOException e) {
                throw new RuntimeException("Error getting cluster settings from OpenSearch", e);
            }
        }
    }


    @Override
    public synchronized void stop() {
        stopProcess();
        stopRestClient();
    }

    private void stopRestClient() {
        restClient().ifPresent(client -> {
            try {
                client.close();
            } catch (IOException e) {
                LOG.warn("Failed to close rest client", e);
            }
        });
    }

    private void stopProcess() {
        if (this.commandLineProcess != null) {
            commandLineProcess.close();
        }
    }

    @Override
    public void remove() {
        LOG.info("Starting removal of OpenSearch node");
        restClient().ifPresent(client -> {
            final ClusterClient clusterClient = client.cluster();
            ClusterUpdateSettingsRequest settings = new ClusterUpdateSettingsRequest();
            settings.transientSettings(Settings.builder()
                    .put(CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING, nodeName)
                    .build());
            try {
                final ClusterUpdateSettingsResponse response =
                        clusterClient.putSettings(settings, RequestOptions.DEFAULT);
                if (response.isAcknowledged()) {
                    allocationExcludeChecked = false; // reset to rejoin cluster in case of failure
                    executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("datanode-removal").build());
                    executorService.scheduleAtFixedRate(this::checkRemovalStatus, 10, 10, TimeUnit.SECONDS);
                } else {
                    throw new RuntimeException("Failed to exclude node from cluster allocation");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to exclude node from cluster allocation", e);
            }
        });
    }

    /**
     * started by onRemove() to check if all shards have been relocated
     */
    void checkRemovalStatus() {
        final Optional<RestHighLevelClient> restClient = restClient();
        if (restClient.isPresent()) {
            try {
                final ClusterClient clusterClient = restClient.get().cluster();
                final ClusterHealthResponse health = clusterClient
                        .health(new ClusterHealthRequest(), RequestOptions.DEFAULT);
                if (health.getRelocatingShards() == 0) {
                    onEvent(OpensearchEvent.PROCESS_STOPPED);
                    executorService.shutdown();
                    eventBus.post(DataNodeLifecycleEvent.create(nodeId.getNodeId(), DataNodeLifecycleTrigger.REMOVED));
                }
            } catch (IOException | OpenSearchStatusException e) {
                throw new RuntimeException("Error checking removal status", e);
            }
        }
    }

    @Override
    public void reset() {
        stop();
        configure();
        start();
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStdOut(String line) {
        LOG.info(line);
        stdout.offer(line);
    }

    @Override
    public void onStdErr(String line) {
        LOG.warn(line);
        stderr.offer(line);
    }

    @Override
    public void onProcessComplete(int exitValue) {
        LOG.info("Opensearch process completed with exit code {}", exitValue);
        onEvent(OpensearchEvent.PROCESS_TERMINATED);
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        LOG.warn("Opensearch process failed", e);
        onEvent(OpensearchEvent.PROCESS_TERMINATED);
    }


    @Override
    public void available() {
        if (!allocationExcludeChecked) {
            this.checkAllocationEnabledStatus();
        }
    }

    @Override
    public boolean isManagerNode() {
        return restClient()
                .flatMap(this::requestClusterState)
                .map(r -> r.nodes().get(r.clusterManagerNode()))
                .map(managerNode -> configuration.getDatanodeNodeName().equals(managerNode.name()))
                .orElse(false);
    }

    private Optional<ClusterStateResponse> requestClusterState(RestHighLevelClient client) {
        try {
            final Response response = client.getLowLevelClient().performRequest(new Request("GET", "_cluster/state/"));
            final ClusterStateResponse state = objectMapper.readValue(response.getEntity().getContent(), ClusterStateResponse.class);
            return Optional.of(state);
        } catch (IOException e) {
            LOG.warn("Failed to obtain cluster state response", e);
            return Optional.empty();
        }
    }

}

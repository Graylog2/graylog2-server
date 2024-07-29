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
package org.graylog.plugins.views.storage.migration.state.actions;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineContext;
import org.graylog.security.certutil.CaKeystore;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.datanode.DataNodeCommandService;
import org.graylog2.datanode.DatanodeStartType;
import org.graylog2.indexer.datanode.RemoteReindexRequest;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.resources.datanodes.DatanodeResolver;
import org.graylog2.rest.resources.datanodes.DatanodeRestApiProxy;
import org.graylog2.shared.utilities.StringUtils;
import org.graylog2.storage.providers.ElasticsearchVersionProvider;
import org.graylog2.system.processing.control.ClusterProcessingControl;
import org.graylog2.system.processing.control.ClusterProcessingControlFactory;
import org.graylog2.system.processing.control.RemoteProcessingControlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MigrationActionsImpl implements MigrationActions {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationActionsImpl.class);

    private final ClusterConfigService clusterConfigService;
    private final ClusterProcessingControlFactory clusterProcessingControlFactory;
    private final NodeService<DataNodeDto> nodeService;
    private final CaKeystore caKeystore;
    private final PreflightConfigService preflightConfigService;

    private final MigrationStateMachineContext stateMachineContext;
    private final DataNodeCommandService dataNodeCommandService;

    private final RemoteReindexingMigrationAdapter migrationService;
    private final MetricRegistry metricRegistry;

    private final DatanodeRestApiProxy datanodeProxy;
    private final ElasticsearchVersionProvider searchVersionProvider;
    private final List<URI> elasticsearchHosts;

    private final Version graylogVersion = Version.CURRENT_CLASSPATH;

    @Inject
    public MigrationActionsImpl(@Assisted MigrationStateMachineContext stateMachineContext,
                                final ClusterConfigService clusterConfigService, NodeService<DataNodeDto> nodeService,
                                final CaKeystore caKeystore, DataNodeCommandService dataNodeCommandService,
                                RemoteReindexingMigrationAdapter migrationService,
                                final ClusterProcessingControlFactory clusterProcessingControlFactory,
                                final PreflightConfigService preflightConfigService,
                                final MetricRegistry metricRegistry,
                                final DatanodeRestApiProxy datanodeProxy,
                                ElasticsearchVersionProvider searchVersionProvider,
                                @Named("elasticsearch_hosts") List<URI> elasticsearchHosts) {
        this.stateMachineContext = stateMachineContext;
        this.clusterConfigService = clusterConfigService;
        this.nodeService = nodeService;
        this.caKeystore = caKeystore;
        this.dataNodeCommandService = dataNodeCommandService;
        this.clusterProcessingControlFactory = clusterProcessingControlFactory;
        this.migrationService = migrationService;
        this.preflightConfigService = preflightConfigService;
        this.metricRegistry = metricRegistry;
        this.datanodeProxy = datanodeProxy;
        this.searchVersionProvider = searchVersionProvider;
        this.elasticsearchHosts = elasticsearchHosts;
    }


    @Override
    public void runDirectoryCompatibilityCheck() {
        final Collection<CompatibilityResult> results = datanodeProxy.remoteInterface(DatanodeResolver.ALL_NODES_KEYWORD, DatanodeDirectoryCompatibilityCheckResource.class, DatanodeDirectoryCompatibilityCheckResource::compatibility).values();
        stateMachineContext.addExtendedState(MigrationStateMachineContext.KEY_COMPATIBILITY_CHECK_PASSED,
                results.stream().allMatch(r -> r.compatibilityErrors().isEmpty()));
        stateMachineContext.setResponse(results);
    }

    @Override
    public boolean isOldClusterStopped() {
        // TODO: add real test
        return true;
    }

    @Override
    public void rollingUpgradeSelected() {
        Counter traffic = (Counter) metricRegistry.getMetrics().get(GlobalMetricNames.INPUT_TRAFFIC);
        stateMachineContext.addExtendedState(TrafficSnapshot.TRAFFIC_SNAPSHOT, new TrafficSnapshot(traffic.getCount()));
    }

    @Override
    public boolean directoryCompatibilityCheckOk() {
        return stateMachineContext.getExtendedState(MigrationStateMachineContext.KEY_COMPATIBILITY_CHECK_PASSED, Boolean.class).orElse(false);
    }

    @Override
    public void reindexUpgradeSelected() {

    }

    @Override
    public void stopMessageProcessing() {
        final String authToken = (String) stateMachineContext.getExtendedState(MigrationStateMachineContext.AUTH_TOKEN_KEY);
        final ClusterProcessingControl<RemoteProcessingControlResource> control = clusterProcessingControlFactory.create(authToken);
        LOG.info("Attempting to pause processing on all nodes...");
        control.pauseProcessing();
        LOG.info("Done pausing processing on all nodes.");
        LOG.info("Waiting for output buffer to drain on all nodes...");
        control.waitForEmptyBuffers();
        LOG.info("Done waiting for output buffer to drain on all nodes.");
    }

    @Override
    public void startMessageProcessing() {
        final String authToken = (String) stateMachineContext.getExtendedState(MigrationStateMachineContext.AUTH_TOKEN_KEY);
        final ClusterProcessingControl<RemoteProcessingControlResource> control = clusterProcessingControlFactory.create(authToken);
        LOG.info("Resuming message processing.");
        control.resumeGraylogMessageProcessing();
    }

    @Override
    public boolean caDoesNotExist() {
        return !caKeystore.exists();
    }

    @Override
    public boolean renewalPolicyDoesNotExist() {
        return this.clusterConfigService.get(RenewalPolicy.class) == null;
    }

    @Override
    public boolean caAndRenewalPolicyExist() {
        return !caDoesNotExist() && !renewalPolicyDoesNotExist();
    }

    @Override
    public boolean compatibleDatanodesRunning() {
        Map<String, DataNodeDto> nodes = nodeService.allActive();
        return !nodes.isEmpty() && nodes.values().stream()
                .allMatch(node -> node.getDatanodeVersion() != null &&
                        graylogVersion.compareTo(new Version(com.github.zafarkhaja.semver.Version.valueOf(node.getDatanodeVersion()))) == 0);
    }

    @Override
    public void provisionDataNodes() {
        // if we start provisioning DataNodes via the migration, Preflight is definitely done/no option anymore
        var preflight = preflightConfigService.getPreflightConfigResult();
        if (preflight == null || !preflight.equals(PreflightConfigResult.PREPARED)) {
            preflightConfigService.setConfigResult(PreflightConfigResult.PREPARED);
        }
        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();
        activeDataNodes.values().stream()
                .filter(node -> node.getDataNodeStatus() != DataNodeStatus.AVAILABLE)
                .forEach(nodeDto -> triggerCSR(nodeDto, DatanodeStartType.MANUALLY));
    }

    private void triggerCSR(DataNodeDto nodeDto, DatanodeStartType startType) {
            try {
                dataNodeCommandService.triggerCertificateSigningRequest(nodeDto.getNodeId(), startType);
            } catch (NodeNotFoundException e) {
                throw new RuntimeException(e);
            }
    }

    @Override
    public void provisionAndStartDataNodes() {
        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();
        activeDataNodes.values().stream()
                .filter(node -> node.getDataNodeStatus() != DataNodeStatus.AVAILABLE)
                .forEach(nodeDto -> triggerCSR(nodeDto, DatanodeStartType.AUTOMATICALLY));
    }

    @Override
    public boolean provisioningFinished() {
        return nodeService.allActive().values().stream().allMatch(node -> node.getDataNodeStatus() == DataNodeStatus.AVAILABLE);
    }

    @Override
    public boolean allDatanodesPrepared() {
        return nodeService.allActive().values().stream().allMatch(node -> node.getDataNodeStatus() == DataNodeStatus.PREPARED);
    }

    @Override
    public void startDataNodes() {
        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();
        activeDataNodes.values().forEach(this::startDataNode);
    }

    private void startDataNode(DataNodeDto node) {
        try {
            dataNodeCommandService.startNode(node.getNodeId());
        } catch (NodeNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean dataNodeStartupFinished() {
        boolean dataNodesAvailable = nodeService.allActive().values().stream().allMatch(node -> node.getDataNodeStatus() == DataNodeStatus.AVAILABLE);
        if (dataNodesAvailable) { // set preflight config to FINISHED to be sure that a Graylog restart will connect to the data nodes
            var preflight = preflightConfigService.getPreflightConfigResult();
            if (preflight == null || !preflight.equals(PreflightConfigResult.FINISHED)) {
                preflightConfigService.setConfigResult(PreflightConfigResult.FINISHED);
            }
        }
        return dataNodesAvailable;
    }

    @Override
    public void startRemoteReindex() {
        final String allowlist = stateMachineContext.getActionArgumentOpt("allowlist", String.class).orElse(null);
        String host = StringUtils.requireNonBlank(stateMachineContext.getActionArgument("hostname", String.class), "hostname has to be provided");
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        final URI hostname = URI.create(host);
        final String user = stateMachineContext.getActionArgumentOpt("user", String.class).orElse(null);
        final String password = stateMachineContext.getActionArgumentOpt("password", String.class).orElse(null);
        final List<String> indices = stateMachineContext.getActionArgumentOpt("indices", List.class).orElse(Collections.emptyList()); // todo: generics!
        final boolean trustUnknownCerts = stateMachineContext.getActionArgumentOpt("trust_unknown_certs", Boolean.class).orElse(false);
        final int threadsCount = stateMachineContext.getActionArgumentOpt("threads", Integer.class).orElse(4);
        final String migrationID = migrationService.start(new RemoteReindexRequest(allowlist, hostname, user, password, indices, threadsCount, trustUnknownCerts));
        stateMachineContext.addExtendedState(MigrationStateMachineContext.KEY_MIGRATION_ID, migrationID);
    }

    @Override
    public void requestMigrationStatus() {
        stateMachineContext.getExtendedState(MigrationStateMachineContext.KEY_MIGRATION_ID, String.class)
                .map(migrationService::status)
                .ifPresent(status -> stateMachineContext.setResponse(status));
    }

    @Override
    public void calculateTrafficEstimate() {
        Counter currentTraffic = (Counter) metricRegistry.getMetrics().get(GlobalMetricNames.INPUT_TRAFFIC);
        MigrationStateMachineContext context = stateMachineContext;
        if (context.getExtendedState(TrafficSnapshot.ESTIMATED_TRAFFIC_PER_MINUTE) == null) {
            context.getExtendedState(TrafficSnapshot.TRAFFIC_SNAPSHOT, TrafficSnapshot.class)
                    .ifPresent(traffic -> context.addExtendedState(TrafficSnapshot.ESTIMATED_TRAFFIC_PER_MINUTE, traffic.calculateEstimatedTrafficPerMinute(currentTraffic.getCount())));
        }
    }

    @Override
    public void verifyRemoteIndexerConnection() {
        final URI hostname = Objects.requireNonNull(URI.create(stateMachineContext.getActionArgument("hostname", String.class)), "hostname has to be provided");
        final String user = stateMachineContext.getActionArgumentOpt("user", String.class).orElse(null);
        final String password = stateMachineContext.getActionArgumentOpt("password", String.class).orElse(null);
        final boolean trustUnknownCerts = stateMachineContext.getActionArgumentOpt("trust_unknown_certs", Boolean.class).orElse(false);
        final String allowlist = stateMachineContext.getActionArgumentOpt("allowlist", String.class).orElse(null);
        stateMachineContext.setResponse(migrationService.checkConnection(hostname, user, password, allowlist, trustUnknownCerts));
    }

    @Override
    public boolean isCompatibleInPlaceMigrationVersion() {
        return !searchVersionProvider.get().isElasticsearch();
    }

    @Override
    public void getElasticsearchHosts() {
        stateMachineContext.setResponse(Map.of(
                "elasticsearch_hosts", elasticsearchHosts.stream().map(URI::toString).collect(Collectors.joining(",")),
                "allowlist_hosts", elasticsearchHosts.stream().map(host -> host.getHost() + ":" + host.getPort()).collect(Collectors.joining(","))
        ));
    }

    @Override
    public boolean isRemoteReindexingFinished() {
        return Optional.ofNullable(stateMachineContext)
                .flatMap(ctx -> ctx.getExtendedState(MigrationStateMachineContext.KEY_MIGRATION_ID, String.class))
                .map(migrationService::status)
                .filter(m -> m.status() == RemoteReindexingMigrationAdapter.Status.FINISHED)
                .isPresent();
    }
}

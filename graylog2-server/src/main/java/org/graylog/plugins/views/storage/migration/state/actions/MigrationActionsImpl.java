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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineContext;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.indexer.datanode.RemoteReindexRequest;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.indexer.migration.RemoteReindexMigration;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.system.processing.control.ClusterProcessingControl;
import org.graylog2.system.processing.control.ClusterProcessingControlFactory;
import org.graylog2.system.processing.control.RemoteProcessingControlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class MigrationActionsImpl implements MigrationActions {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationActionsImpl.class);

    private final ClusterConfigService clusterConfigService;
    private final ClusterProcessingControlFactory clusterProcessingControlFactory;
    private final NodeService<DataNodeDto> nodeService;
    private final CaService caService;
    private final PreflightConfigService preflightConfigService;

    private MigrationStateMachineContext stateMachineContext;
    private final DataNodeProvisioningService dataNodeProvisioningService;

    private final RemoteReindexingMigrationAdapter migrationService;
    private final MetricRegistry metricRegistry;

    @Inject
    public MigrationActionsImpl(final ClusterConfigService clusterConfigService, NodeService<DataNodeDto> nodeService,
                                final CaService caService, DataNodeProvisioningService dataNodeProvisioningService,
                                RemoteReindexingMigrationAdapter migrationService,
                                final ClusterProcessingControlFactory clusterProcessingControlFactory,
                                final PreflightConfigService preflightConfigService,
                                final MetricRegistry metricRegistry) {
        this.clusterConfigService = clusterConfigService;
        this.nodeService = nodeService;
        this.caService = caService;
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.clusterProcessingControlFactory = clusterProcessingControlFactory;
        this.migrationService = migrationService;
        this.preflightConfigService = preflightConfigService;
        this.metricRegistry = metricRegistry;
    }


    @Override
    public boolean runDirectoryCompatibilityCheck() {
        // TODO: add real test
        return true;
    }

    @Override
    public boolean isOldClusterStopped() {
        // TODO: add real test
        return true;
    }

    @Override
    public void rollingUpgradeSelected() {
        Counter traffic = (Counter) metricRegistry.getMetrics().get(GlobalMetricNames.INPUT_TRAFFIC);
        getStateMachineContext().addExtendedState(TrafficSnapshot.TRAFFIC_SNAPSHOT, new TrafficSnapshot(traffic.getCount()));
    }

    @Override
    public boolean directoryCompatibilityCheckOk() {
        // TODO: add real test
        return true;
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
        try {
            return this.caService.get() == null;
        } catch (KeyStoreStorageException e) {
            return true;
        }
    }

    @Override
    public boolean removalPolicyDoesNotExist() {
        return this.clusterConfigService.get(RenewalPolicy.class) == null;
    }

    @Override
    public boolean caAndRemovalPolicyExist() {
        return !caDoesNotExist() && !removalPolicyDoesNotExist();
    }

    @Override
    public void provisionDataNodes() {
        // if we start provisioning DataNodes via the migration, Preflight is definitely done/no option anymore
        var preflight = preflightConfigService.getPreflightConfigResult();
        if (preflight == null || !preflight.equals(PreflightConfigResult.PREPARED)) {
            preflightConfigService.setConfigResult(PreflightConfigResult.PREPARED);
        }
        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();
        activeDataNodes.values().forEach(node -> dataNodeProvisioningService.changeState(node.getNodeId(), DataNodeProvisioningConfig.State.CONFIGURED));
    }

    @Override
    public void provisionAndStartDataNodes() {
        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();
        activeDataNodes.values().forEach(node -> dataNodeProvisioningService.changeState(node.getNodeId(), DataNodeProvisioningConfig.State.CONFIGURED));
    }

    @Override
    public boolean provisioningFinished() {
        return nodeService.allActive().values().stream().allMatch(node -> dataNodeProvisioningService.getPreflightConfigFor(node.getNodeId())
                .map(dn -> dn.state() == DataNodeProvisioningConfig.State.STARTUP_PREPARED)
                .orElse(false));
    }

    @Override
    public void startDataNodes() {
        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();
        activeDataNodes.values().forEach(node -> dataNodeProvisioningService.changeState(node.getNodeId(), DataNodeProvisioningConfig.State.STARTUP_TRIGGER));
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
        final URI hostname = Objects.requireNonNull(URI.create(getStateMachineContext().getActionArgument("hostname", String.class)), "hostname has to be provided");
        final String user = getStateMachineContext().getActionArgumentOpt("user", String.class).orElse(null);
        final String password = getStateMachineContext().getActionArgumentOpt("password", String.class).orElse(null);
        final List<String> indices = getStateMachineContext().getActionArgumentOpt("indices", List.class).orElse(Collections.emptyList()); // todo: generics!
        final int threadsCount = getStateMachineContext().getActionArgumentOpt("threads", Integer.class).orElse(4);
        final RemoteReindexMigration migration = migrationService.start(new RemoteReindexRequest(hostname, user, password, indices, threadsCount));
        final String migrationID = migration.id();
        getStateMachineContext().addExtendedState(MigrationStateMachineContext.KEY_MIGRATION_ID, migrationID);
    }

    @Override
    public void requestMigrationStatus() {
        getStateMachineContext().getExtendedState(MigrationStateMachineContext.KEY_MIGRATION_ID, String.class)
                .map(migrationService::status)
                .ifPresent(status -> getStateMachineContext().setResponse(status));
    }

    @Override
    public void calculateTrafficEstimate() {
        Counter currentTraffic = (Counter) metricRegistry.getMetrics().get(GlobalMetricNames.INPUT_TRAFFIC);
        MigrationStateMachineContext context = getStateMachineContext();
        if (context.getExtendedState(TrafficSnapshot.ESTIMATED_TRAFFIC_PER_MINUTE) == null) {
            context.getExtendedState(TrafficSnapshot.TRAFFIC_SNAPSHOT, TrafficSnapshot.class)
                    .ifPresent(traffic -> context.addExtendedState(TrafficSnapshot.ESTIMATED_TRAFFIC_PER_MINUTE, traffic.calculateEstimatedTrafficPerMinute(currentTraffic.getCount())));
        }
    }

    @Override
    public void verifyRemoteIndexerConnection() {
        final URI hostname = Objects.requireNonNull(URI.create(getStateMachineContext().getActionArgument("hostname", String.class)), "hostname has to be provided");
        final String user = getStateMachineContext().getActionArgumentOpt("user", String.class).orElse(null);
        final String password = getStateMachineContext().getActionArgumentOpt("password", String.class).orElse(null);
        getStateMachineContext().setResponse(migrationService.checkConnection(hostname, user, password));
    }

    @Override
    public boolean isRemoteReindexingFinished() {
        return Optional.ofNullable(getStateMachineContext())
                .flatMap(ctx -> ctx.getExtendedState(MigrationStateMachineContext.KEY_MIGRATION_ID, String.class))
                .map(migrationService::status)
                .filter(m -> m.status() == RemoteReindexingMigrationAdapter.Status.FINISHED)
                .isPresent();
    }

    @Override
    public void setStateMachineContext(MigrationStateMachineContext context) {
        this.stateMachineContext = context;
    }

    @Override
    public MigrationStateMachineContext getStateMachineContext() {
        return stateMachineContext;
    }
}

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
package org.graylog.datanode.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.bootstrap.preflight.DatanodeDirectoriesLockfileCheck;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.OpensearchConfigurationProvider;
import org.graylog.datanode.metrics.ConfigureMetricsIndexSettings;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.ProcessStateMachine;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.cluster.preflight.DataNodeProvisioningStateChangeEvent;
import org.graylog2.datanode.DataNodeLifecycleEvent;
import org.graylog2.datanode.RemoteReindexAllowlistEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.CustomCAX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OpensearchProcessService extends AbstractIdleService implements Provider<OpensearchProcess> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessService.class);

    private static final int WATCHDOG_RESTART_ATTEMPTS = 3;
    private final OpensearchProcess process;
    private final OpensearchConfigurationProvider configurationProvider;
    private final EventBus eventBus;
    private final NodeId nodeId;
    private final DataNodeProvisioningService dataNodeProvisioningService;
    private final IndexFieldTypesService indexFieldTypesService;
    private final ClusterEventBus clusterEventBus;
    private final DatanodeDirectoriesLockfileCheck lockfileCheck;
    private final PreflightConfigService preflightConfigService;
    private final Configuration configuration;


    @Inject
    public OpensearchProcessService(final DatanodeConfiguration datanodeConfiguration,
                                    final OpensearchConfigurationProvider configurationProvider,
                                    final EventBus eventBus,
                                    final CustomCAX509TrustManager trustManager,
                                    final NodeService<DataNodeDto> nodeService,
                                    final Configuration configuration,
                                    final DataNodeProvisioningService dataNodeProvisioningService,
                                    final NodeId nodeId,
                                    final IndexFieldTypesService indexFieldTypesService,
                                    final ObjectMapper objectMapper,
                                    final ProcessStateMachine processStateMachine,
                                    final ClusterEventBus clusterEventBus,
                                    final DatanodeDirectoriesLockfileCheck lockfileCheck,
                                    final PreflightConfigService preflightConfigService) {
        this.configurationProvider = configurationProvider;
        this.configuration = configuration;
        this.eventBus = eventBus;
        this.nodeId = nodeId;
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.indexFieldTypesService = indexFieldTypesService;
        this.clusterEventBus = clusterEventBus;
        this.lockfileCheck = lockfileCheck;
        this.preflightConfigService = preflightConfigService;
        this.process = createOpensearchProcess(datanodeConfiguration, trustManager, configuration, nodeService, objectMapper, processStateMachine);
        eventBus.register(this);
    }

    private OpensearchProcess createOpensearchProcess(final DatanodeConfiguration datanodeConfiguration, final CustomCAX509TrustManager trustManager, final Configuration configuration,
                                                      final NodeService<DataNodeDto> nodeService, final ObjectMapper objectMapper, final ProcessStateMachine processStateMachine) {
        final OpensearchProcessImpl process = new OpensearchProcessImpl(datanodeConfiguration, datanodeConfiguration.processLogsBufferSize(), trustManager, configuration, nodeService, objectMapper, processStateMachine);
        final ProcessWatchdog watchdog = new ProcessWatchdog(process, WATCHDOG_RESTART_ATTEMPTS);
        process.addStateMachineTracer(watchdog);
        process.addStateMachineTracer(new StateMachineTransitionLogger());
        process.addStateMachineTracer(new OpensearchRemovalTracer(process, configuration.getDatanodeNodeName(), nodeId, clusterEventBus));
        process.addStateMachineTracer(new ConfigureMetricsIndexSettings(process, configuration, indexFieldTypesService, objectMapper, nodeService));
        process.addStateMachineTracer(new ClusterNodeStateTracer(nodeService, nodeId));
        return process;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleRemoteReindexAllowlistEvent(RemoteReindexAllowlistEvent event) {
        switch (event.action()) {
            case ADD -> {
                this.process.stop();
                this.configurationProvider.setTransientConfiguration("reindex.remote.allowlist", event.allowlist());
                configure(); // , "action.auto_create_index", "false"));
                this.process.start();
            }
            case REMOVE -> {
                this.process.stop();
                configure();
                this.process.start();
            }
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handlePreflightConfigEvent(DataNodeProvisioningStateChangeEvent event) {
        switch (event.state()) {
            case STARTUP_REQUESTED -> this.process.start();
            case STORED -> {
                configure();
                dataNodeProvisioningService.changeState(event.nodeId(), DataNodeProvisioningConfig.State.STARTUP_PREPARED);
            }
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleNodeLifecycleEvent(DataNodeLifecycleEvent event) {
        if (nodeId.getNodeId().equals(event.nodeId())) {
            switch (event.trigger()) {
                case REMOVE -> process.onRemove();
                case RESET -> process.onReset();
                case STOP -> this.shutDown();
                case START -> this.startUp();
            }
        }
    }

    private void checkWritePreflightFinishedOnInsecureStartup() {
        if(configuration.isInsecureStartup()) {
            var preflight = preflightConfigService.getPreflightConfigResult();
            if (preflight == null || !preflight.equals(PreflightConfigResult.FINISHED)) {
                preflightConfigService.setConfigResult(PreflightConfigResult.FINISHED);
            }
        }
    }

    @Override
    protected void startUp() {
        final OpensearchConfiguration config = configurationProvider.get();
        configure();
        if (config.securityConfigured()) {
            checkWritePreflightFinishedOnInsecureStartup();
            try {
                lockfileCheck.checkDatanodeLock(config.datanodeDirectories().getDataTargetDir());
                this.process.start();
            } catch (Exception e) {
                LOG.error("Could not start up data node", e);
            }

        }
    }

    private void configure() {
        final OpensearchConfiguration config = configurationProvider.get();
        if (config.securityConfigured()) {
            this.process.configure(config);
        } else {
            String noConfigMessage = """
                    \n
                    ========================================================================================================
                    It seems you are starting Data node for the first time. The current configuration is not sufficient to
                    start the indexer process because a security configuration is missing. You have to either provide http
                    and transport SSL certificates or use the Graylog preflight interface to configure this Data node remotely.
                    ========================================================================================================
                    """;
            LOG.info(noConfigMessage);
        }
        eventBus.post(new OpensearchConfigurationChangeEvent(config));
    }


    @Override
    protected void shutDown() {
        this.process.stop();
    }

    @Override
    public OpensearchProcess get() {
        return process;
    }
}

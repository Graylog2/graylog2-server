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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.bootstrap.preflight.DatanodeDirectoriesLockfileCheck;
import org.graylog.datanode.configuration.OpensearchConfigurationProvider;
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.cluster.preflight.DataNodeProvisioningStateChangeEvent;
import org.graylog2.datanode.DataNodeLifecycleEvent;
import org.graylog2.datanode.RemoteReindexAllowlistEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.plugin.system.NodeId;
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
    private final DatanodeDirectoriesLockfileCheck lockfileCheck;
    private final PreflightConfigService preflightConfigService;
    private final Configuration configuration;

    private final OpensearchStateMachine stateMachine;


    @Inject
    public OpensearchProcessService(
            final OpensearchConfigurationProvider configurationProvider,
            final EventBus eventBus,
            final Configuration configuration,
            final DataNodeProvisioningService dataNodeProvisioningService,
            final NodeId nodeId,
            final IndexFieldTypesService indexFieldTypesService,
            final ClusterEventBus clusterEventBus,
            final DatanodeDirectoriesLockfileCheck lockfileCheck,
            final PreflightConfigService preflightConfigService,
            final OpensearchProcess process, OpensearchStateMachine stateMachine) {
        this.configurationProvider = configurationProvider;
        this.configuration = configuration;
        this.eventBus = eventBus;
        this.nodeId = nodeId;
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.lockfileCheck = lockfileCheck;
        this.preflightConfigService = preflightConfigService;
        this.process = process;
        this.stateMachine = stateMachine;
        eventBus.register(this);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleRemoteReindexAllowlistEvent(RemoteReindexAllowlistEvent event) {
        switch (event.action()) {
            case ADD -> {
                stateMachine.fire(OpensearchEvent.PROCESS_STOPPED);
                this.configurationProvider.setTransientConfiguration("reindex.remote.allowlist", event.allowlist());
                configure(); // , "action.auto_create_index", "false"));
                stateMachine.fire(OpensearchEvent.PROCESS_STARTED);
            }
            case REMOVE -> {
                stateMachine.fire(OpensearchEvent.PROCESS_STOPPED);
                configure();
                stateMachine.fire(OpensearchEvent.PROCESS_STARTED);
            }
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handlePreflightConfigEvent(DataNodeProvisioningStateChangeEvent event) {
        switch (event.state()) {
            case STARTUP_REQUESTED -> {
                LOG.info("Startup requested by Graylog server");
                stateMachine.fire(OpensearchEvent.PROCESS_STARTED);
            }
            case STORED -> {
                LOG.info("Provisioning ready, configuring and starting OpenSearch");
                configure();
                dataNodeProvisioningService.changeState(event.nodeId(), DataNodeProvisioningConfig.State.STARTUP_PREPARED);
                stateMachine.fire(OpensearchEvent.PROCESS_PREPARED);
            }
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleNodeLifecycleEvent(DataNodeLifecycleEvent event) {
        if (nodeId.getNodeId().equals(event.nodeId())) {
            switch (event.trigger()) {
                case REMOVE -> stateMachine.fire(OpensearchEvent.PROCESS_REMOVE);
                case RESET -> stateMachine.fire(OpensearchEvent.RESET);
                case STOP -> this.shutDown();
                case START -> stateMachine.fire(OpensearchEvent.PROCESS_STARTED);
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

    /**
     * triggered when starting the service
     */
    @Override
    protected void startUp() {
        final OpensearchConfiguration config = configurationProvider.get();
        configure();
        if (config.securityConfigured()) {
            LOG.info("OpenSearch starting up");
            checkWritePreflightFinishedOnInsecureStartup();
            try {
                lockfileCheck.checkDatanodeLock(config.datanodeDirectories().getDataTargetDir());
                stateMachine.fire(OpensearchEvent.PROCESS_STARTED);
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
        eventBus.post(new OpensearchConfigurationChangeEvent(config)); //refresh jersey
    }


    @Override
    protected void shutDown() {
        stateMachine.fire(OpensearchEvent.PROCESS_STOPPED);
    }

    @Override
    public OpensearchProcess get() {
        return process;
    }
}

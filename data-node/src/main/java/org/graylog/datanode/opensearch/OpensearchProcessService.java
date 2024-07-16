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
import org.graylog.datanode.configuration.OpensearchConfigurationService;
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.datanode.DataNodeLifecycleEvent;
import org.graylog2.datanode.RemoteReindexAllowlistEvent;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OpensearchProcessService extends AbstractIdleService implements Provider<OpensearchProcess> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessService.class);

    private final OpensearchProcess process;
    private final OpensearchConfigurationService configurationProvider;
    private final NodeId nodeId;
    private final DatanodeDirectoriesLockfileCheck lockfileCheck;
    private final PreflightConfigService preflightConfigService;
    private final Configuration configuration;

    private final OpensearchStateMachine stateMachine;
    private final CsrRequester csrRequester;
    private boolean processAutostart = true;


    @Inject
    public OpensearchProcessService(
            final OpensearchConfigurationService configurationProvider,
            final EventBus eventBus,
            final Configuration configuration,
            final NodeId nodeId,
            final DatanodeDirectoriesLockfileCheck lockfileCheck,
            final PreflightConfigService preflightConfigService,
            final OpensearchProcess process, CsrRequester csrRequester, OpensearchStateMachine stateMachine) {
        this.configurationProvider = configurationProvider;
        this.configuration = configuration;
        this.nodeId = nodeId;
        this.lockfileCheck = lockfileCheck;
        this.preflightConfigService = preflightConfigService;
        this.process = process;
        this.csrRequester = csrRequester;
        this.stateMachine = stateMachine;
        eventBus.register(this);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleRemoteReindexAllowlistEvent(RemoteReindexAllowlistEvent event) {
        switch (event.action()) {
            case ADD -> this.configurationProvider.setAllowlist(event.allowlist(), event.trustedCertificates());
            case REMOVE -> this.configurationProvider.removeAllowlist();
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
                case REQUEST_CSR -> {
                    this.processAutostart = false;
                    csrRequester.triggerCertificateSigningRequest();
                }
                case REQUEST_CSR_WITH_AUTOSTART -> {
                    this.processAutostart = true;
                    csrRequester.triggerCertificateSigningRequest();
                }
            }
        }
    }

    private void checkWritePreflightFinishedOnInsecureStartup() {
        if (configuration.isInsecureStartup()) {
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

    }

    @Subscribe
    public void onConfigurationChangeEvent(OpensearchConfigurationChangeEvent event) {
        onConfigurationChange(event.config());
    }

    private void onConfigurationChange(OpensearchConfiguration config) {
        configure(config);
        if (config.securityConfigured()) {
            LOG.info("OpenSearch starting up");
            checkWritePreflightFinishedOnInsecureStartup();
            try {
                lockfileCheck.checkDatanodeLock(config.datanodeDirectories().getDataTargetDir());
                if (stateMachine.isInState(OpensearchState.WAITING_FOR_CONFIGURATION) && !this.processAutostart) {
                    stateMachine.fire(OpensearchEvent.PROCESS_PREPARED);
                    this.processAutostart = true; // reset to default
                } else {
                    stateMachine.fire(OpensearchEvent.PROCESS_STARTED);
                }
            } catch (Exception e) {
                LOG.error("Could not start up data node", e);
            }
        }
    }

    private void configure(OpensearchConfiguration config) {
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

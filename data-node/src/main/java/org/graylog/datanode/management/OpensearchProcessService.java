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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningStateChangeEvent;
import org.graylog2.datanode.DataNodeLifecycleEvent;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.CustomCAX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class OpensearchProcessService extends AbstractIdleService implements Provider<OpensearchProcess> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessService.class);

    private static final int WATCHDOG_RESTART_ATTEMPTS = 3;
    private final OpensearchProcess process;
    private final Provider<OpensearchConfiguration> configurationProvider;
    private final EventBus eventBus;
    private final NodeId nodeId;

    @Inject
    public OpensearchProcessService(final DatanodeConfiguration datanodeConfiguration,
                                    final Provider<OpensearchConfiguration> configurationProvider,
                                    final EventBus eventBus,
                                    final CustomCAX509TrustManager trustManager,
                                    final NodeService<DataNodeDto> nodeService,
                                    final Configuration configuration,
                                    final NodeId nodeId) {
        this.configurationProvider = configurationProvider;
        this.eventBus = eventBus;
        this.nodeId = nodeId;
        this.process = createOpensearchProcess(datanodeConfiguration, trustManager, configuration, nodeService);
        eventBus.register(this);
    }

    private OpensearchProcess createOpensearchProcess(final DatanodeConfiguration datanodeConfiguration, final CustomCAX509TrustManager trustManager, final Configuration configuration, final NodeService<DataNodeDto> nodeService) {
        final OpensearchProcessImpl process = new OpensearchProcessImpl(datanodeConfiguration, datanodeConfiguration.processLogsBufferSize(), trustManager, configuration, nodeService);
        final ProcessWatchdog watchdog = new ProcessWatchdog(process, WATCHDOG_RESTART_ATTEMPTS);
        process.addStateMachineTracer(watchdog);
        process.addStateMachineTracer(new StateMachineTransitionLogger());
        process.addStateMachineTracer(new OpensearchRemovalTracer(process, configuration.getDatanodeNodeName()));
        return process;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handlePreflightConfigEvent(DataNodeProvisioningStateChangeEvent event) {
        switch (event.state()) {
            case STORED -> startWithConfig();
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

    @Override
    protected void startUp() {
        startWithConfig();
    }

    private void startWithConfig() {
        final OpensearchConfiguration config = configurationProvider.get();
        if (config.securityConfigured()) {
            this.process.startWithConfig(config);
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

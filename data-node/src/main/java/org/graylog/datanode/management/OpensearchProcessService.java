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
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.metrics.ConfigureMetricsIndexSettings;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.cluster.preflight.DataNodeProvisioningStateChangeEvent;
import org.graylog2.datanode.DataNodeLifecycleEvent;
import org.graylog2.datanode.RemoteReindexAllowlistEvent;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.CustomCAX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class OpensearchProcessService extends AbstractIdleService implements Provider<OpensearchProcess> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessService.class);

    private static final int WATCHDOG_RESTART_ATTEMPTS = 3;
    private final OpensearchProcess process;
    private final Provider<OpensearchConfiguration> configurationProvider;
    private final EventBus eventBus;
    private final NodeId nodeId;
    private final DataNodeProvisioningService dataNodeProvisioningService;
    private final IndexFieldTypesService indexFieldTypesService;
    private final ObjectMapper objectMapper;

    @Inject
    public OpensearchProcessService(final DatanodeConfiguration datanodeConfiguration,
                                    final Provider<OpensearchConfiguration> configurationProvider,
                                    final EventBus eventBus,
                                    final CustomCAX509TrustManager trustManager,
                                    final NodeService<DataNodeDto> nodeService,
                                    final Configuration configuration,
                                    final DataNodeProvisioningService dataNodeProvisioningService,
                                    final NodeId nodeId,
                                    final IndexFieldTypesService indexFieldTypesService,
                                    final ObjectMapper objectMapper) {
        this.configurationProvider = configurationProvider;
        this.eventBus = eventBus;
        this.nodeId = nodeId;
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.objectMapper = objectMapper;
        this.indexFieldTypesService = indexFieldTypesService;
        this.process = createOpensearchProcess(datanodeConfiguration, trustManager, configuration, nodeService, objectMapper);
        eventBus.register(this);
    }

    private OpensearchProcess createOpensearchProcess(final DatanodeConfiguration datanodeConfiguration, final CustomCAX509TrustManager trustManager, final Configuration configuration,
                                                      final NodeService<DataNodeDto> nodeService, final ObjectMapper objectMapper) {
        final OpensearchProcessImpl process = new OpensearchProcessImpl(datanodeConfiguration, datanodeConfiguration.processLogsBufferSize(), trustManager, configuration, nodeService, objectMapper);
        final ProcessWatchdog watchdog = new ProcessWatchdog(process, WATCHDOG_RESTART_ATTEMPTS);
        process.addStateMachineTracer(watchdog);
        process.addStateMachineTracer(new StateMachineTransitionLogger());
        process.addStateMachineTracer(new OpensearchRemovalTracer(process, configuration.getDatanodeNodeName()));
        process.addStateMachineTracer(new ConfigureMetricsIndexSettings(process, configuration, indexFieldTypesService, objectMapper));
        return process;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleRemoteReindexAllowlistEvent(RemoteReindexAllowlistEvent event) {
        switch (event.action()) {
            case ADD -> {
                this.process.stop();
                configure(Map.of("reindex.remote.whitelist", event.host())); // , "action.auto_create_index", "false"));
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
            case STARTUP_REQUESTED -> startUp();
            case STORED -> {
                configure();
                dataNodeProvisioningService.changeState(event.nodeId(), DataNodeProvisioningConfig.State.STARTUP_REQUESTED);
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

    @Override
    protected void startUp() {
        final OpensearchConfiguration config = configurationProvider.get();
        if (config.securityConfigured()) {
            this.configure();
            this.process.start();
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

    protected void configure() {
        this.configure(Map.of());
    }

    private void configure(Map<String, String> additionalConfig) {
        final OpensearchConfiguration original = configurationProvider.get();

        final var finalAdditionalConfig = new HashMap<String, String>();
        finalAdditionalConfig.putAll(original.additionalConfiguration());
        finalAdditionalConfig.putAll(additionalConfig);

        final var config = new OpensearchConfiguration(
                original.opensearchDistribution(),
                original.datanodeDirectories(),
                original.bindAddress(),
                original.hostname(),
                original.httpPort(),
                original.transportPort(),
                original.clusterName(),
                original.nodeName(),
                original.nodeRoles(),
                original.discoverySeedHosts(),
                original.opensearchSecurityConfiguration(),
                finalAdditionalConfig);

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

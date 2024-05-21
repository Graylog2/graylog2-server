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
package org.graylog2.cluster.preflight;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.oxo42.stateless4j.triggers.TriggerWithParameters1;
import jakarta.annotation.Nonnull;
import jakarta.inject.Provider;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class DatanodePreflightStateMachineProvider implements Provider<StateMachine<DataNodeProvisioningConfig.State, DatanodeProvisioningEvent>> {

    public static final TriggerWithParameters1<String, DatanodeProvisioningEvent> TRIGGER_CERTIFICATE_RECEIVED = new TriggerWithParameters1<>(DatanodeProvisioningEvent.CERTIFICATE_RECEIVED, String.class);
    public static final DataNodeProvisioningConfig.State INITIAL_STATE = DataNodeProvisioningConfig.State.UNCONFIGURED;
    private final NodeId nodeId;
    private final DatanodePreflightStateService dataNodeProvisioningService;
    private final DatanodeProvisioningActions datanodePreflightActions;

    public DatanodePreflightStateMachineProvider(NodeId nodeId, DatanodePreflightStateService dataNodeProvisioningService, DatanodeProvisioningActions datanodePreflightActions) {
        this.nodeId = nodeId;
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.datanodePreflightActions = datanodePreflightActions;
    }

    private static final Logger LOG = LoggerFactory.getLogger(DatanodePreflightStateMachineProvider.class);

    @Override
    public StateMachine<DataNodeProvisioningConfig.State, DatanodeProvisioningEvent> get() {
        StateMachineConfig<DataNodeProvisioningConfig.State, DatanodeProvisioningEvent> config = new StateMachineConfig<>();

        config.configure(DataNodeProvisioningConfig.State.UNCONFIGURED)
                .permit(DatanodeProvisioningEvent.CREATE_PRIVATE_KEY,
                        DataNodeProvisioningConfig.State.CONFIGURED,
                        datanodePreflightActions::generateAndStorePrivateKey
                );

        config.configure(DataNodeProvisioningConfig.State.CSR)
                .onEntry(() -> LOG.info("This state is not needed and ignored"));

        config.configure(DataNodeProvisioningConfig.State.CONFIGURED)
                .onEntry(datanodePreflightActions::generateCsrEvent)
                .permit(DatanodeProvisioningEvent.CERTIFICATE_RECEIVED, DataNodeProvisioningConfig.State.STARTUP_PREPARED);

        config.configure(DataNodeProvisioningConfig.State.STORED)
                .onEntry(() -> LOG.info("This state is not needed and ignored"));

        config.setTriggerParameters(DatanodeProvisioningEvent.CERTIFICATE_RECEIVED, String.class);
        config.configure(DataNodeProvisioningConfig.State.STARTUP_PREPARED)
                .onEntryFrom(TRIGGER_CERTIFICATE_RECEIVED, datanodePreflightActions::onCertificateReceivedEvent)
                .permit(DatanodeProvisioningEvent.STARTUP_REQUESTED, DataNodeProvisioningConfig.State.CONNECTING);

        config.configure(DataNodeProvisioningConfig.State.CONNECTING)
                .onEntry(() -> LOG.info("connection check triggered"))
                .permit(DatanodeProvisioningEvent.CONNECTING_FAILED, DataNodeProvisioningConfig.State.ERROR)
                .permit(DatanodeProvisioningEvent.CONNECTING_SUCCEEDED, DataNodeProvisioningConfig.State.CONNECTED);

        config.configure(DataNodeProvisioningConfig.State.ERROR) // terminal state
                .onEntry(() -> LOG.error("Shit happended"));

        config.configure(DataNodeProvisioningConfig.State.CONNECTED) // terminal state
                .onEntry(() -> LOG.error("Everything went well"));

        return new StateMachine<>(INITIAL_STATE, this::getCurrentState, this::persistState, config);
    }


    private void persistState(DataNodeProvisioningConfig.State state) {
        if (dataNodeProvisioningService.getPreflightConfigFor(nodeId.getNodeId()).isEmpty()) {
            writeInitialConfig();
        }
        dataNodeProvisioningService.changeState(nodeId.getNodeId(), state);
    }

    @Nonnull
    private DataNodeProvisioningConfig.State getCurrentState() {
        final Optional<DataNodeProvisioningConfig> preflightConfig = dataNodeProvisioningService.getPreflightConfigFor(nodeId.getNodeId());
        return preflightConfig.map(DataNodeProvisioningConfig::state).orElse(INITIAL_STATE);
    }

    private void writeInitialConfig() {
        dataNodeProvisioningService.save(DataNodeProvisioningConfig.builder()
                .nodeId(nodeId.getNodeId())
                .state(INITIAL_STATE)
                .build());
    }
}

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
package org.graylog2.inputs;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.shared.inputs.InputLauncher;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.PersistedInputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InputEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(InputEventListener.class);
    private final InputLauncher inputLauncher;
    private final InputRegistry inputRegistry;
    private final LeaderElectionService leaderElectionService;
    private final PersistedInputs persistedInputs;
    private final ServerStatus serverStatus;

    @Inject
    public InputEventListener(InputLauncher inputLauncher,
                              InputRegistry inputRegistry,
                              LeaderElectionService leaderElectionService,
                              PersistedInputs persistedInputs,
                              ServerStatus serverStatus) {
        this.inputLauncher = inputLauncher;
        this.inputRegistry = inputRegistry;
        this.leaderElectionService = leaderElectionService;
        this.persistedInputs = persistedInputs;
        this.serverStatus = serverStatus;
    }

    public void inputCreated(String inputId) {
        LOG.debug("Input created: {}", inputId);

        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputId);
        if (inputState != null) {
            inputRegistry.remove(inputState);
        }

        inputLauncher.launch(inputId);
    }

    public void inputUpdated(String inputId) {
        LOG.debug("Input updated: {}", inputId);

        final boolean startInput;
        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputId);
        if (inputState != null) {
            startInput = inputState.getState() == IOState.Type.RUNNING || inputState.getState() == IOState.Type.SETUP;
            inputRegistry.remove(inputState);
        } else {
            startInput = false;
        }

        if (startInput) {
            inputLauncher.launch(inputId);
        }
    }

    public void inputDeleted(String inputId) {
        LOG.debug("Input deleted: {}", inputId);
        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputId);
        if (inputState != null) {
            inputRegistry.remove(inputState);
        }
    }

    public void inputSetup(String inputId) {
        LOG.info("Input setup: {}", inputId);
        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputId);
        if (inputState != null) {
            inputRegistry.setup(inputState);
        } else {
            LOG.debug("Input created for setup: {}", inputId);
            inputLauncher.launch(inputId);
        }
    }

    public void leaderChanged() {
        if (serverStatus.getLifecycle() == Lifecycle.STARTING) {
            LOG.debug("Ignoring LeaderChangedEvent during server startup.");
            return;
        }
        if (leaderElectionService.isLeader()) {
            for (MessageInput input : persistedInputs) {
                final IOState<MessageInput> inputState = inputRegistry.getInputState(input.getId());
                if (input.onlyOnePerCluster() && input.isGlobal() && (inputState == null || inputState.canBeStarted())
                        && inputLauncher.shouldStartAutomatically(input)) {
                    LOG.info("Got leader role. Starting input {}", input.toIdentifier());
                    inputLauncher.launch(input.getId());
                }
            }
        } else {
            inputRegistry.getRunningInputs().stream()
                    .map(IOState::getStoppable)
                    .filter(input -> input.isGlobal() && input.onlyOnePerCluster())
                    .forEach(input -> {
                        LOG.info("Lost leader role. Stopping input {}", input.toIdentifier());
                        inputDeleted(input.getId());
                    });
        }
    }
}

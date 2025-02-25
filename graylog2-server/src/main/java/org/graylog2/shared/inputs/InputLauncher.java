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
package org.graylog2.shared.inputs;

import jakarta.inject.Inject;
import org.graylog2.Configuration;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.f;

public class InputLauncher {
    private static final Logger LOG = LoggerFactory.getLogger(InputLauncher.class);
    private final IOState.Factory<MessageInput> inputStateFactory;
    private final InputBuffer inputBuffer;
    private final PersistedInputs persistedInputs;
    private final InputRegistry inputRegistry;
    private final Configuration configuration;
    private final LeaderElectionService leaderElectionService;
    private final FeatureFlags featureFlags;

    @Inject
    public InputLauncher(IOState.Factory<MessageInput> inputStateFactory, InputBuffer inputBuffer, PersistedInputs persistedInputs,
                         InputRegistry inputRegistry, Configuration configuration, LeaderElectionService leaderElectionService,
                         FeatureFlags featureFlags) {
        this.inputStateFactory = inputStateFactory;
        this.inputBuffer = inputBuffer;
        this.persistedInputs = persistedInputs;
        this.inputRegistry = inputRegistry;
        this.configuration = configuration;
        this.leaderElectionService = leaderElectionService;
        this.featureFlags = featureFlags;
    }

    public IOState<MessageInput> launch(final MessageInput input) {
        checkNotNull(input);

        final IOState<MessageInput> inputState;
        if (inputRegistry.getInputState(input.getId()) == null) {
            if (featureFlags.isOn("SETUP_MODE") && input.getDesiredState() == IOState.Type.SETUP) {
                inputState = inputStateFactory.create(input, IOState.Type.SETUP);
            } else {
                inputState = inputStateFactory.create(input);
            }
            inputRegistry.add(inputState);
        } else {
            inputState = requireNonNull(inputRegistry.getInputState(input.getId()), f("inputState for input %s cannot be null", input.toString()));
            switch (inputState.getState()) {
                case RUNNING, STARTING, FAILING -> {
                    return inputState;
                }
            }
            inputState.setStoppable(input);
        }

        // Do not launch if currently in setup mode
        if (inputState.getState() == IOState.Type.SETUP) {
            return inputState;
        }

        LOG.debug("Starting [{}] input {}", input.getClass().getCanonicalName(), input.toIdentifier());
        try {
            input.checkConfiguration();
            inputState.setState(IOState.Type.STARTING);
            input.launch(inputBuffer, new InputFailureRecorder(inputState));
            inputState.setState(IOState.Type.RUNNING);
            String msg = "Completed starting [" + input.getClass().getCanonicalName() + "] input " + input.toIdentifier();
            LOG.debug(msg);
        } catch (Exception e) {
            handleLaunchException(e, inputState);
        }

        return inputState;
    }

    protected void handleLaunchException(Throwable e, IOState<MessageInput> inputState) {
        final MessageInput input = inputState.getStoppable();
        StringBuilder msg = new StringBuilder("The [" + input.getClass().getCanonicalName() + "] input " + input.toIdentifier() + " misfired. Reason: ");

        String causeMsg = ExceptionUtils.getRootCauseMessage(e);

        msg.append(causeMsg);

        LOG.error(msg.toString(), e);

        // Clean up.
        //cleanInput(input);

        inputState.setState(IOState.Type.FAILED, causeMsg);
    }

    public void launchAllPersisted() {
        for (MessageInput input : persistedInputs) {
            if (leaderStatusInhibitsLaunch(input)) {
                LOG.info("Not launching 'onlyOnePerCluster' input {} because this node is not the leader.",
                        input.toIdentifier());
                continue;
            }
            if (shouldStartAutomatically(input)) {
                LOG.info("Launching input {} - desired state is {}",
                        input.toIdentifier(), input.getDesiredState());
                input.initialize();
                launch(input);
            } else if (input.getDesiredState().equals(IOState.Type.SETUP)) {
                launch(input);
            } else {
                LOG.info("Not auto-starting input {} - desired state is {}",
                        input.toIdentifier(), input.getDesiredState());
            }
        }
    }


    public boolean shouldStartAutomatically(MessageInput input) {
        return configuration.getAutoRestartInputs() || input.getDesiredState().equals(IOState.Type.RUNNING);
    }

    public boolean leaderStatusInhibitsLaunch(MessageInput input) {
        return input.onlyOnePerCluster() && input.isGlobal() && !leaderElectionService.isLeader();
    }
}

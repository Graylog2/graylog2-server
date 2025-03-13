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

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.inject.Inject;
import org.graylog2.Configuration;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.database.NotFoundException;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

public class InputLauncher {
    private static final Logger LOG = LoggerFactory.getLogger(InputLauncher.class);
    private final IOState.Factory<MessageInput> inputStateFactory;
    private final InputBuffer inputBuffer;
    private final PersistedInputs persistedInputs;
    private final InputRegistry inputRegistry;
    private final ExecutorService executor;
    private final Configuration configuration;
    private final LeaderElectionService leaderElectionService;
    private final FeatureFlags featureFlags;
    private final InputService inputService;
    private final NodeId nodeId;

    @Inject
    public InputLauncher(IOState.Factory<MessageInput> inputStateFactory, InputBuffer inputBuffer, PersistedInputs persistedInputs,
                         InputRegistry inputRegistry, MetricRegistry metricRegistry, Configuration configuration, LeaderElectionService leaderElectionService,
                         FeatureFlags featureFlags, InputService inputService, NodeId nodeId) {
        this.inputStateFactory = inputStateFactory;
        this.inputBuffer = inputBuffer;
        this.persistedInputs = persistedInputs;
        this.inputRegistry = inputRegistry;
        this.executor = executorService(metricRegistry);
        this.configuration = configuration;
        this.leaderElectionService = leaderElectionService;
        this.featureFlags = featureFlags;
        this.inputService = inputService;
        this.nodeId = nodeId;
    }

    private ExecutorService executorService(final MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("inputs-%d").build();
        return new InstrumentedExecutorService(
                Executors.newCachedThreadPool(threadFactory),
                metricRegistry,
                name(this.getClass(), "executor-service"));
    }

    public void launch(final String inputId) {
        requireNonBlank(inputId, "inputId cannot be blank");

        final Input input;
        try {
            input = inputService.find(inputId);
        } catch (NotFoundException e) {
            LOG.warn("Could not find input {}", inputId, e);
            return;
        }

        try {
            launch(inputService.getMessageInput(input));
        } catch (NoSuchInputTypeException e) {
            LOG.warn("Input {} is of invalid type {}", input.toIdentifier(), input.getType(), e);
        }
    }

    public void launch(final MessageInput messageInput) {
        requireNonNull(messageInput, "messageInput cannot be null");

        // TODO: Why do we have to add inputs with desired state of SETUP to the registry? (was like that in the old code)
        if (!EnumSet.of(IOState.Type.RUNNING, IOState.Type.SETUP).contains(messageInput.getDesiredState())) {
            LOG.debug("Not starting input {} - desired state is {}", messageInput.toIdentifier(), messageInput.getDesiredState());
            return;
        }

        if (!shouldRunOnThisNode(messageInput)) {
            LOG.debug("Input {} is not global and shouldn't run on node <{}>", messageInput.toIdentifier(), nodeId);
            return;
        }
        if (leaderStatusInhibitsLaunch(messageInput)) {
            return;
        }

        final IOState<MessageInput> inputState = inputRegistry.add(messageInput);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                LOG.debug("Starting input {}", messageInput.toIdentifier());
                inputState.triggerStart();
            }
        });
    }

    public void launchAllPersisted() {
        for (MessageInput input : persistedInputs) {
            if (leaderStatusInhibitsLaunch(input)) {
                continue;
            }
            if (shouldStartAutomatically(input)) {
                LOG.info("Launching input {} - desired state is {}",
                        input.toIdentifier(), input.getDesiredState());
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

    public boolean shouldRunOnThisNode(MessageInput input) {
        return input.isGlobal() || nodeId.getNodeId().equals(input.getNodeId());
    }

    private boolean leaderStatusInhibitsLaunch(MessageInput input) {
        final var noLaunch = input.onlyOnePerCluster() && input.isGlobal() && !leaderElectionService.isLeader();
        if (noLaunch) {
            LOG.info("Not launching 'onlyOnePerCluster' input {} because this node is not the leader.", input.toIdentifier());
        }
        return noLaunch;
    }
}

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
import org.graylog2.Configuration;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkNotNull;

public class InputLauncher {
    private static final Logger LOG = LoggerFactory.getLogger(InputLauncher.class);
    private final IOState.Factory<MessageInput> inputStateFactory;
    private final InputBuffer inputBuffer;
    private final PersistedInputs persistedInputs;
    private final InputRegistry inputRegistry;
    private final ExecutorService executor;
    private final Configuration configuration;
    private final LeaderElectionService leaderElectionService;

    @Inject
    public InputLauncher(IOState.Factory<MessageInput> inputStateFactory, InputBuffer inputBuffer, PersistedInputs persistedInputs,
                         InputRegistry inputRegistry, MetricRegistry metricRegistry, Configuration configuration, LeaderElectionService leaderElectionService) {
        this.inputStateFactory = inputStateFactory;
        this.inputBuffer = inputBuffer;
        this.persistedInputs = persistedInputs;
        this.inputRegistry = inputRegistry;
        this.executor = executorService(metricRegistry);
        this.configuration = configuration;
        this.leaderElectionService = leaderElectionService;
    }

    private ExecutorService executorService(final MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("inputs-%d").build();
        return new InstrumentedExecutorService(
                Executors.newCachedThreadPool(threadFactory),
                metricRegistry,
                name(this.getClass(), "executor-service"));
    }

    public IOState<MessageInput> launch(final MessageInput input) {
        checkNotNull(input);

        final IOState<MessageInput> inputState;
        if (inputRegistry.getInputState(input.getId()) == null) {
            inputState = inputStateFactory.create(input);
            inputRegistry.add(inputState);
        } else {
            inputState = inputRegistry.getInputState(input.getId());
            if (inputState.getState() == IOState.Type.RUNNING || inputState.getState() == IOState.Type.STARTING) {
                return inputState;
            }
            inputState.setStoppable(input);
        }

        executor.submit(new Runnable() {
            @Override
            public void run() {
                LOG.debug("Starting [{}] input with ID <{}>", input.getClass().getCanonicalName(), input.getId());
                try {
                    input.checkConfiguration();
                    inputState.setState(IOState.Type.STARTING);
                    input.launch(inputBuffer);
                    inputState.setState(IOState.Type.RUNNING);
                    String msg = "Completed starting [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + ">";
                    LOG.debug(msg);
                } catch (Exception e) {
                    handleLaunchException(e, inputState);
                }
            }
        });

        return inputState;
    }

    protected void handleLaunchException(Throwable e, IOState<MessageInput> inputState) {
        final MessageInput input = inputState.getStoppable();
        StringBuilder msg = new StringBuilder("The [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + "> misfired. Reason: ");

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
                LOG.info("Not launching 'onlyOnePerCluster' input [{}/{}/{}] because this node is not the leader.",
                        input.getName(), input.getTitle(), input.getId());
                continue;
            }
            if (shouldStartAutomatically(input)) {
                LOG.info("Launching input [{}/{}/{}] - desired state is {}",
                        input.getName(), input.getTitle(), input.getId(), input.getDesiredState());
                input.initialize();
                launch(input);
            } else {
                LOG.info("Not auto-starting input [{}/{}/{}] - desired state is {}",
                        input.getName(), input.getTitle(), input.getId(), input.getDesiredState());
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

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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import org.graylog2.cluster.leader.LeaderChangedEvent;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputSetup;
import org.graylog2.rest.models.system.inputs.responses.InputUpdated;
import org.graylog2.shared.inputs.InputLauncher;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.inputs.PersistedInputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InputEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(InputEventListener.class);
    protected static final int EVENT_QUEUE_POLL_PERIOD_MS = 100;
    private final InputLauncher inputLauncher;
    private final InputRegistry inputRegistry;
    private final InputService inputService;
    private final NodeId nodeId;
    private final LeaderElectionService leaderElectionService;
    private final PersistedInputs persistedInputs;
    private final ServerStatus serverStatus;
    private final ScheduledExecutorService daemonScheduler = Executors.newSingleThreadScheduledExecutor();
    private final LinkedBlockingQueue<QueuedEvent> eventQueue = new LinkedBlockingQueue<>(EVENT_QUEUE_POLL_PERIOD_MS);

    private record QueuedEvent(Object receivedEvent) {}

    @Inject
    public InputEventListener(EventBus eventBus,
                              InputLauncher inputLauncher,
                              InputRegistry inputRegistry,
                              InputService inputService,
                              NodeId nodeId,
                              LeaderElectionService leaderElectionService,
                              PersistedInputs persistedInputs,
                              ServerStatus serverStatus) {
        this.inputLauncher = inputLauncher;
        this.inputRegistry = inputRegistry;
        this.inputService = inputService;
        this.nodeId = nodeId;
        this.leaderElectionService = leaderElectionService;
        this.persistedInputs = persistedInputs;
        this.serverStatus = serverStatus;
        initializeEventQueueTask();
        eventBus.register(this);
    }

    // TODO: add shutdown behavior
    private void initializeEventQueueTask() {
        daemonScheduler.scheduleAtFixedRate(() -> {
            try {
                final var queuedEvent = eventQueue.poll();
                if (queuedEvent != null) {
                    final Object receivedEvent = queuedEvent.receivedEvent;
                    LOG.debug("Processing event: {}", receivedEvent);
                    if (receivedEvent instanceof InputCreated) {
                        doInputCreated(((InputCreated) receivedEvent).id());
                    } else if (receivedEvent instanceof InputDeleted) {
                        doInputDeleted(((InputDeleted) receivedEvent).id());
                    } else if (receivedEvent instanceof InputSetup) {
                        doInputSetup(((InputSetup) receivedEvent).id());
                    } else if (receivedEvent instanceof InputUpdated) {
                        doInputUpdated(((InputUpdated) receivedEvent).id());
                    } else if (receivedEvent instanceof LeaderChangedEvent) {
                        doLeaderChanged();
                    } else {
                        throw new IllegalArgumentException("Unexpected event: " + queuedEvent.receivedEvent);
                    }
                }
            } catch (Exception e) {
                LOG.error("Caught exception while trying to process queued event", e);
            }
        }, 0, EVENT_QUEUE_POLL_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    @Subscribe
    public void inputCreated(InputCreated inputCreatedEvent) {
        final String inputId = inputCreatedEvent.id();
        LOG.debug("Input created: {}", inputId);
        eventQueue.add(new QueuedEvent(inputCreatedEvent));
    }

    private void doInputCreated(String inputId) {
        final Input input;
        try {
            input = inputService.find(inputId);
        } catch (NotFoundException e) {
            LOG.warn("Received InputCreated event but could not find input {}", inputId, e);
            return;
        }

        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputId);
        if (inputState != null) {
            inputRegistry.remove(inputState);
        }

        if (input.isGlobal() || this.nodeId.getNodeId().equals(input.getNodeId())) {
            startInput(input);
        }
    }

    @Subscribe
    public void inputUpdated(InputUpdated inputUpdatedEvent) {
        final String inputId = inputUpdatedEvent.id();
        LOG.debug("Input updated: {}", inputId);
        eventQueue.add(new QueuedEvent(inputUpdatedEvent));
    }

    private void doInputUpdated(final String inputId) {
        final Input input;
        try {
            input = inputService.find(inputId);
        } catch (NotFoundException e) {
            LOG.warn("Received InputUpdated event but could not find input {}", inputId, e);
            return;
        }

        final boolean startInput;
        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputId);
        if (inputState != null) {
            startInput = inputState.getState() == IOState.Type.RUNNING || inputState.getState() == IOState.Type.SETUP;
            inputRegistry.remove(inputState);
        } else {
            startInput = false;
        }

        if (startInput && (input.isGlobal() || this.nodeId.getNodeId().equals(input.getNodeId()))) {
            startInput(input);
        }
    }

    private void startInput(Input input) {
        final MessageInput messageInput;
        try {
            messageInput = inputService.getMessageInput(input);
        } catch (NoSuchInputTypeException e) {
            LOG.warn("Input {} is of invalid type {}", input.toIdentifier(), input.getType(), e);
            return;
        }
        if (!inputLauncher.leaderStatusInhibitsLaunch(messageInput)) {
            startMessageInput(messageInput);
        } else {
            LOG.info("Not launching 'onlyOnePerCluster' input {} because this node is not the leader.",
                    input.toIdentifier());
        }
    }

    private void startMessageInput(MessageInput messageInput) {
        messageInput.initialize();

        final IOState<MessageInput> newInputState = inputLauncher.launch(messageInput);
        inputRegistry.add(newInputState);
    }

    @Subscribe
    public void inputDeleted(InputDeleted inputDeletedEvent) {
        LOG.debug("Input deleted: {}", inputDeletedEvent.id());
        eventQueue.add(new QueuedEvent(inputDeletedEvent));
    }

    private void doInputDeleted(String inputId) {
        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputId);
        if (inputState != null) {
            inputRegistry.remove(inputState);
        }
    }

    @Subscribe
    public void inputSetup(InputSetup inputSetupEvent) {
        LOG.info("Input setup: {}", inputSetupEvent.id());
        eventQueue.add(new QueuedEvent(inputSetupEvent));
    }

    private void doInputSetup(String inputId) {
        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputId);
        if (inputState != null) {
            inputRegistry.setup(inputState);
        } else {
            LOG.debug("Input created for setup: {}", inputId);
            final Input input;
            try {
                input = inputService.find(inputId);
            } catch (NotFoundException e) {
                LOG.warn("Received InputSetupEvent event but could not find input {}", inputId, e);
                return;
            }

            if (input.isGlobal() || this.nodeId.getNodeId().equals(input.getNodeId())) {
                startInput(input);
            }
        }
    }

    @Subscribe
    public void leaderChanged(LeaderChangedEvent leaderChangedEvent) {
        if (serverStatus.getLifecycle() == Lifecycle.STARTING) {
            LOG.debug("Ignoring LeaderChangedEvent during server startup.");
            return;
        }
        eventQueue.add(new QueuedEvent(leaderChangedEvent));
    }

    private void doLeaderChanged() {
        if (leaderElectionService.isLeader()) {
            for (MessageInput input : persistedInputs) {
                final IOState<MessageInput> inputState = inputRegistry.getInputState(input.getId());
                if (input.onlyOnePerCluster() && input.isGlobal() && (inputState == null || inputState.canBeStarted())
                        && inputLauncher.shouldStartAutomatically(input)) {
                    LOG.info("Got leader role. Starting input {}", input.toIdentifier());
                    startMessageInput(input);
                }
            }
        } else {
            inputRegistry.getRunningInputs().stream()
                    .map(IOState::getStoppable)
                    .filter(input -> input.isGlobal() && input.onlyOnePerCluster())
                    .forEach(input -> {
                        LOG.info("Lost leader role. Stopping input {}", input.toIdentifier());
                        inputDeleted(InputDeleted.create(input.getId()));
                    });
        }
    }
}

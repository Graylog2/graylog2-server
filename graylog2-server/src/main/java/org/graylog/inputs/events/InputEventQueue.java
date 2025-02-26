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
package org.graylog.inputs.events;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.cluster.leader.LeaderChangedEvent;
import org.graylog2.inputs.InputEventListener;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputSetup;
import org.graylog2.rest.models.system.inputs.responses.InputUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Buffers and serializes input events to avoid concurrent execution of input events.
 */
@Singleton
public class InputEventQueue extends AbstractExecutionThreadService {
    private static final Logger LOG = LoggerFactory.getLogger(InputEventQueue.class);

    private final LinkedBlockingDeque<InputEvent> queue;
    private final EventBus eventBus;
    private final InputEventListener inputEventListener;

    @Inject
    public InputEventQueue(EventBus eventBus, InputEventListener inputEventListener) {
        this.inputEventListener = inputEventListener;
        this.queue = new LinkedBlockingDeque<>(512);
        this.eventBus = eventBus;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.debug("Starting input event queue");
        eventBus.register(this);
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.debug("Stopping input event queue");
        eventBus.unregister(this);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleInputCreated(InputCreated event) {
        triggerEvent(new InputEvent(EventType.CREATED, event.id()));
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleInputUpdated(InputUpdated event) {
        triggerEvent(new InputEvent(EventType.UPDATED, event.id()));
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleInputDeleted(InputDeleted event) {
        triggerEvent(new InputEvent(EventType.DELETED, event.id()));
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleInputSetup(InputSetup event) {
        triggerEvent(new InputEvent(EventType.SETUP, event.id()));
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleLeaderChanged(LeaderChangedEvent event) {
        triggerEvent(new InputEvent(EventType.LEADER_CHANGED, null));
    }

    private void triggerEvent(InputEvent event) {
        LOG.debug("Received event: {} (on thread {})", event, Thread.currentThread().getName());
        try {
            if (!queue.offer(event, 100, TimeUnit.MILLISECONDS)) {
                LOG.warn("Couldn't enqueue input event <{}> after 100 ms", event);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            final var event = queue.poll(200, TimeUnit.MILLISECONDS);
            if (event == null) {
                continue;
            }

            LOG.debug("Dispatch event: {} (on thread {})", event, Thread.currentThread().getName());

            switch (event.type()) {
                case CREATED -> inputEventListener.inputCreated(event.inputId());
                case UPDATED -> inputEventListener.inputUpdated(event.inputId());
                case SETUP -> inputEventListener.inputSetup(event.inputId());
                case DELETED -> inputEventListener.inputDeleted(event.inputId());
                case LEADER_CHANGED -> inputEventListener.leaderChanged();
                default -> throw new IllegalArgumentException("Unhandled event type: " + event.type());
            }
        }
    }

    private enum EventType {
        CREATED, UPDATED, SETUP, DELETED, LEADER_CHANGED
    }

    private record InputEvent(EventType type, @Nullable String inputId) {
    }
}

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
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.events.inputs.IOStateChangedEvent;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class InputStateListener {
    private static final Logger LOG = LoggerFactory.getLogger(InputStateListener.class);
    private NotificationService notificationService;
    private ActivityWriter activityWriter;
    private ServerStatus serverStatus;

    @Inject
    public InputStateListener(EventBus eventBus,
                              NotificationService notificationService,
                              ActivityWriter activityWriter,
                              ServerStatus serverStatus) {
        this.notificationService = notificationService;
        this.activityWriter = activityWriter;
        this.serverStatus = serverStatus;
        eventBus.register(this);
    }

    @Subscribe public void inputStateChanged(IOStateChangedEvent<MessageInput> event) {
        final IOState<MessageInput> state = event.changedState();
        final MessageInput input = state.getStoppable();
        switch (event.newState()) {
            case FAILED:
                activityWriter.write(new Activity(state.getDetailedMessage(), InputRegistry.class));
                Notification notification = notificationService.buildNow();
                notification.addType(Notification.Type.INPUT_FAILED_TO_START).addSeverity(Notification.Severity.NORMAL);
                notification.addNode(serverStatus.getNodeId().toString());
                notification.addDetail("input_id", input.getId());
                notification.addDetail("reason", state.getDetailedMessage());
                notificationService.publishIfFirst(notification);
                break;
            case RUNNING:
                notificationService.fixed(Notification.Type.NO_INPUT_RUNNING);
                // fall through
            default:
                final String msg = "Input [" + input.getName() + "/" + input.getId() + "] is now " + event.newState().toString();
                activityWriter.write(new Activity(msg, InputStateListener.class));
                break;
        }

        LOG.debug("Input State of [{}/{}] changed: {} -> {}", input.getTitle(), input.getId(), event.oldState(), event.newState());
        LOG.info("Input [{}/{}] is now {}", input.getName(), input.getId(), event.newState());
    }
}

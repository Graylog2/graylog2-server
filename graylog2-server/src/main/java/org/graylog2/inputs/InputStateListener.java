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
import org.apache.commons.lang3.ObjectUtils;
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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import static org.graylog2.plugin.IOState.Type.FAILING;
import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class InputStateListener {
    private static final Logger LOG = LoggerFactory.getLogger(InputStateListener.class);
    private final NotificationService notificationService;
    private final ActivityWriter activityWriter;
    private final ServerStatus serverStatus;

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

    @Subscribe
    public void inputStateChanged(IOStateChangedEvent<MessageInput> event) {
        final IOState<MessageInput> state = event.changedState();
        final MessageInput input = state.getStoppable();
        var msg = f("Input %s is in state %s", input.toIdentifier(), event.newState());
        if (state.getDetailedMessage() != null) {
            msg = f("Input %s is in state %s [%s]", input.toIdentifier(), event.newState(), state.getDetailedMessage());
        }
        switch (event.newState()) {
            case FAILED:
            case FAILING:
                activityWriter.write(new Activity(msg, InputRegistry.class));
                Notification notification = notificationService.buildNow();
                var type = event.newState().equals(FAILING) ? Notification.Type.INPUT_FAILING : Notification.Type.INPUT_FAILED_TO_START;
                notification.addType(type).addSeverity(Notification.Severity.NORMAL);
                notification.addKey(input.getId());
                notification.addNode(serverStatus.getNodeId().toString());
                notification.addDetail("input_id", input.toIdentifier());
                notification.addDetail("reason", ObjectUtils.defaultIfNull(state.getDetailedMessage(), ""));
                notificationService.publishIfFirst(notification);
                break;
            case RUNNING:
                notificationService.fixed(Notification.Type.NO_INPUT_RUNNING);
                notificationService.fixed(Notification.Type.INPUT_FAILING, input.getId());
                notificationService.fixed(Notification.Type.INPUT_FAILED_TO_START, input.getId());
                // fall through
            default:
                activityWriter.write(new Activity(msg, InputStateListener.class));
                break;
        }

        LOG.debug("Input State of {} changed: {} -> {}", input.toIdentifier(), event.oldState(), event.newState());
        LOG.info("Input {} is now {}", input.toIdentifier(), event.newState());
    }
}

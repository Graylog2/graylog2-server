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
import org.graylog2.inputs.persistence.InputStateService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.events.inputs.IOStateChangedEvent;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InputStateListenerTest {

    private static final String INPUT_ID = "6510deadbeefdeadbeefdead";

    @Mock
    private NotificationService notificationService;
    @Mock
    private ActivityWriter activityWriter;
    @Mock
    private ServerStatus serverStatus;
    @Mock
    private InputStateService inputStateService;

    private final MessageInput input = mock(MessageInput.class);
    private InputStateListener listener;

    @BeforeEach
    void setUp() {
        when(input.getId()).thenReturn(INPUT_ID);
        when(serverStatus.getNodeId()).thenReturn(new SimpleNodeId("00000000-0000-0000-0000-000000000000"));
        when(notificationService.buildNow()).thenReturn(mock(Notification.class, RETURNS_SELF));

        listener = new InputStateListener(mock(EventBus.class), notificationService, activityWriter, serverStatus,
                inputStateService);
    }

    /**
     * A failure that retrying cannot resolve arrives while the input is already FAILING, so only the message changes.
     * {@code publishIfFirst} will not overwrite the reason stored by the earlier transient failure, so the stale
     * notification has to be retired first or the denied action and resource never reach the notification at all -
     * the only surface a Cloud tenant can see.
     */
    @Test
    void retiresTheStaleNotificationWhenOnlyTheMessageChanged() {
        listener.inputStateChanged(eventFor(IOState.Type.FAILING, IOState.Type.FAILING));

        final var order = inOrder(notificationService);
        order.verify(notificationService).fixed(Notification.Type.INPUT_FAILING, INPUT_ID);
        order.verify(notificationService).publishIfFirst(any());
    }

    @Test
    void retiresTheStaleNotificationForARepeatedStartupFailure() {
        listener.inputStateChanged(eventFor(IOState.Type.FAILED, IOState.Type.FAILED));

        verify(notificationService).fixed(Notification.Type.INPUT_FAILED_TO_START, INPUT_ID);
        verify(notificationService).publishIfFirst(any());
    }

    /**
     * On a real transition there is nothing stale to retire, and retiring would throw away a notification another node
     * may have raised for the same input.
     */
    @Test
    void doesNotRetireAnythingOnAStateTransition() {
        listener.inputStateChanged(eventFor(IOState.Type.RUNNING, IOState.Type.FAILING));

        verify(notificationService, never()).fixed(any(Notification.Type.class), anyString());
        verify(notificationService).publishIfFirst(any());
    }

    private IOStateChangedEvent<MessageInput> eventFor(IOState.Type oldState, IOState.Type newState) {
        // A mock event bus, so setting the message here does not re-enter the listener under test.
        final IOState<MessageInput> state = new IOState<>(mock(EventBus.class), input, newState);
        state.setDetailedMessage("AWS authorization failure for Kinesis input <test-stream>.");
        return IOStateChangedEvent.create(oldState, newState, state);
    }
}

/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.converters;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Subscribe public void inputStateChanged(IOState<MessageInput> state) {
        LOG.debug("Input State of {} changed to: {}", state.getStoppable().getTitle(), state.getState());
        switch (state.getState()) {
            case RUNNING:
                notificationService.fixed(Notification.Type.NO_INPUT_RUNNING);
                String msg = "Completed starting [" + state.getStoppable().getClass().getCanonicalName() + "] input with ID <" + state.getStoppable().getId() + ">";
                activityWriter.write(new Activity(msg, InputRegistry.class));
                break;
            case FAILED:
                activityWriter.write(new Activity(state.getDetailedMessage(), InputRegistry.class));
                Notification notification = notificationService.buildNow();
                notification.addType(Notification.Type.INPUT_FAILED_TO_START).addSeverity(Notification.Severity.NORMAL);
                notification.addNode(serverStatus.getNodeId().toString());
                notification.addDetail("input_id", state.getStoppable().getId());
                notification.addDetail("reason", state.getDetailedMessage());
                notificationService.publishIfFirst(notification);
                break;
        }
    }
}

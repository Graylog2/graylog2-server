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
package org.graylog2.inputs;

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
        final MessageInput input = state.getStoppable();
        LOG.debug("Input State of {} changed to: {}", input.getTitle(), state.getState());
        switch (state.getState()) {
            case STARTING:
                final String startingMsg = "Launching existing input [" + input.getName() + "].";
                LOG.info(startingMsg);
                activityWriter.write(new Activity(startingMsg, InputStateListener.class));
                break;
            case RUNNING:
                notificationService.fixed(Notification.Type.NO_INPUT_RUNNING);
                final String runningMsg = "Completed starting [" + input.getClass().getCanonicalName() + "] input with ID <" + state.getStoppable().getId() + ">";
                LOG.info(runningMsg);
                activityWriter.write(new Activity(runningMsg, InputRegistry.class));
                break;
            case FAILED:
                activityWriter.write(new Activity(state.getDetailedMessage(), InputRegistry.class));
                Notification notification = notificationService.buildNow();
                notification.addType(Notification.Type.INPUT_FAILED_TO_START).addSeverity(Notification.Severity.NORMAL);
                notification.addNode(serverStatus.getNodeId().toString());
                notification.addDetail("input_id", input.getId());
                notification.addDetail("reason", state.getDetailedMessage());
                notificationService.publishIfFirst(notification);
                break;
            case STOPPING:
                final String stoppingMsg = "Stopping input [" + input.getName() + "].";
                LOG.info(stoppingMsg);
                activityWriter.write(new Activity(stoppingMsg, InputStateListener.class));
                break;
            case STOPPED:
                final String stoppedMessage = "Stopped input [" + input.getName() + "].";
                LOG.info(stoppedMessage);
                activityWriter.write(new Activity(stoppedMessage, InputStateListener.class));
                break;
        }
    }
}

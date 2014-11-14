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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import org.graylog2.database.NotFoundException;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ServerInputRegistry extends InputRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(ServerInputRegistry.class);
    protected final InputService inputService;
    protected final NotificationService notificationService;
    private final ServerStatus serverStatus;
    private final ActivityWriter activityWriter;

    public ServerInputRegistry(MessageInputFactory messageInputFactory,
                               ProcessBuffer processBuffer,
                               ServerStatus serverStatus,
                               ActivityWriter activityWriter,
                               InputService inputService,
                               NotificationService notificationService,
                               MetricRegistry metricRegistry) {
        super(messageInputFactory, processBuffer, metricRegistry);
        this.serverStatus = serverStatus;
        this.activityWriter = activityWriter;
        this.inputService = inputService;
        this.notificationService = notificationService;
    }

    protected List<MessageInput> getAllPersisted() {
        List<MessageInput> result = Lists.newArrayList();

        for (Input io : inputService.allOfThisNode(serverStatus.getNodeId().toString())) {
            MessageInput input = null;
            try {
                input = inputService.getMessageInput(io);
                result.add(input);
            } catch (NoSuchInputTypeException e) {
                LOG.warn("Cannot launch persisted input. No such type [{}].", io.getType());
            }
        }

        return result;
    }

    public void cleanInput(MessageInput messageInput) {
        try {
            final Input input = inputService.find(messageInput.getPersistId());
            inputService.destroy(input);
        } catch (NotFoundException e) {
            LOG.error("Unable to clean input <" + messageInput.getPersistId() + ">: ", e);
        }
    }

    @Override
    protected void finishedLaunch(InputState state) {
        switch (state.getState()) {
            case RUNNING:
                notificationService.fixed(Notification.Type.NO_INPUT_RUNNING);
                String msg = "Completed starting [" + state.getMessageInput().getClass().getCanonicalName() + "] input with ID <" + state.getMessageInput().getId() + ">";
                activityWriter.write(new Activity(msg, InputRegistry.class));
                break;
            case FAILED:
                activityWriter.write(new Activity(state.getDetailedMessage(), InputRegistry.class));
                Notification notification = notificationService.buildNow();
                notification.addType(Notification.Type.INPUT_FAILED_TO_START).addSeverity(Notification.Severity.NORMAL);
                notification.addNode(serverStatus.getNodeId().toString());
                notification.addDetail("input_id", state.getMessageInput().getId());
                notification.addDetail("reason", state.getDetailedMessage());
                notificationService.publishIfFirst(notification);
                break;
        }
    }

    @Override
    protected void finishedTermination(InputState state) {
        removeFromRunning(state);
    }

    @Override
    protected void finishedStop(InputState inputState) {
    }
}
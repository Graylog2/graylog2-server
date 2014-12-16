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
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.inputs.MessageInput;
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

    public ServerInputRegistry(IOState.Factory<MessageInput> inputStateFactory,
                               MessageInputFactory messageInputFactory,
                               InputBuffer inputBuffer,
                               ServerStatus serverStatus,
                               InputService inputService,
                               NotificationService notificationService,
                               MetricRegistry metricRegistry) {
        super(inputStateFactory, messageInputFactory, inputBuffer, metricRegistry);
        this.serverStatus = serverStatus;
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
            } catch (Throwable e) {
                LOG.warn("Cannot launch persisted input. Exception caught: ", e);
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
    protected void finishedTermination(IOState<MessageInput> state) {
        removeFromRunning(state);
    }
}
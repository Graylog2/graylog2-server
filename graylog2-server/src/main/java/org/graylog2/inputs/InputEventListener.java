/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.shared.inputs.InputLauncher;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class InputEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(InputEventListener.class);
    private final InputLauncher inputLauncher;
    private final InputRegistry inputRegistry;
    private final InputService inputService;
    private final NodeId nodeId;

    @Inject
    public InputEventListener(EventBus eventBus,
                              InputLauncher inputLauncher,
                              InputRegistry inputRegistry,
                              InputService inputService,
                              NodeId nodeId) {
        this.inputLauncher = inputLauncher;
        this.inputRegistry = inputRegistry;
        this.inputService = inputService;
        this.nodeId = nodeId;
        eventBus.register(this);
    }

    @Subscribe public void inputCreated(InputCreated inputCreatedEvent) {
        LOG.debug("Input created/changed: " + inputCreatedEvent.id());
        final Input input;
        try {
            input = inputService.find(inputCreatedEvent.id());
        } catch (NotFoundException e) {
            LOG.warn("Received InputCreated event but could not find Input: ", e);
            return;
        }

        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputCreatedEvent.id());
        if (inputState != null) {
            inputRegistry.remove(inputState);
        }

        if (!input.isGlobal() && !this.nodeId.toString().equals(input.getNodeId())) {
            return;
        }

        final MessageInput messageInput;
        try {
            messageInput = inputService.getMessageInput(input);
            messageInput.initialize();
        } catch (NoSuchInputTypeException e) {
            LOG.warn("Newly created input is of invalid type: " + input.getType(), e);
            return;
        }
        final IOState<MessageInput> newInputState = inputLauncher.launch(messageInput);
        inputRegistry.add(newInputState);
    }

    @Subscribe public void inputDeleted(InputDeleted inputDeletedEvent) {
        LOG.debug("Input deleted: " + inputDeletedEvent.id());
        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputDeletedEvent.id());
        if (inputState != null) {
            inputRegistry.remove(inputState);
        }
    }
}

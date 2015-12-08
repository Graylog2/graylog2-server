package org.graylog2.inputs;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.shared.inputs.InputLauncher;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class InputEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(InputEventListener.class);
    private final InputLauncher inputLauncher;
    private final InputRegistry inputRegistry;
    private final InputService inputService;
    private final MessageInputFactory messageInputFactory;
    private final NodeId nodeId;

    @Inject
    public InputEventListener(EventBus eventBus,
                              InputLauncher inputLauncher,
                              InputRegistry inputRegistry,
                              InputService inputService,
                              MessageInputFactory messageInputFactory,
                              NodeId nodeId) {
        this.inputLauncher = inputLauncher;
        this.inputRegistry = inputRegistry;
        this.inputService = inputService;
        this.messageInputFactory = messageInputFactory;
        this.nodeId = nodeId;
        eventBus.register(this);
    }

    @Subscribe public void inputCreated(InputCreated inputCreatedEvent) {
        LOG.info("Input created: " + inputCreatedEvent.id());
        final Input input;
        try {
            input = inputService.find(inputCreatedEvent.id());
        } catch (NotFoundException e) {
            e.printStackTrace();
            return;
        }

        if (!input.isGlobal() && !this.nodeId.toString().equals(input.getNodeId())) {
            return;
        }

        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputCreatedEvent.id());
        if (inputState != null) {
            inputRegistry.remove(inputState);
        }
        final MessageInput messageInput;
        try {
            messageInput = inputService.getMessageInput(input);
        } catch (NoSuchInputTypeException e) {
            e.printStackTrace();
            return;
        }
        final IOState<MessageInput> newInputState = inputLauncher.launch(messageInput);
        inputRegistry.add(newInputState);
    }

    @Subscribe public void inputDeleted(InputDeleted inputDeletedEvent) {
        LOG.info("Input deleted: " + inputDeletedEvent.id());
        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputDeletedEvent.id());
        if (inputState != null) {
            inputRegistry.remove(inputState);
        }
    }
}

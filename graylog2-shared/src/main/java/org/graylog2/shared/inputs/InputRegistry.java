/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

package org.graylog2.shared.inputs;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class InputRegistry {

    protected static final Logger LOG = LoggerFactory.getLogger(InputRegistry.class);
    protected static final Map<String, ClassLoader> classLoaders = Maps.newHashMap();
    protected final List<InputState> inputStates = Lists.newArrayList();
    protected final ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("inputs-%d").build()
    );
    private final MessageInputFactory messageInputFactory;
    private final ProcessBuffer processBuffer;

    public InputRegistry(MessageInputFactory messageInputFactory,
                         ProcessBuffer processBuffer) {
        this.messageInputFactory = messageInputFactory;
        this.processBuffer = processBuffer;
    }

    public MessageInput create(String inputClass) throws NoSuchInputTypeException {
        return messageInputFactory.create(inputClass);
    }

    public InputState launch(final MessageInput input, String id) {
        return launch(input, id, false);
    }

    protected abstract void finishedLaunch(InputState state);

    protected abstract void finishedTermination(InputState state);

    public InputState launch(final MessageInput input, String id, boolean register) {
        final InputState inputState = new InputState(input, id);
        inputStates.add(inputState);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                LOG.info("Starting [{}] input with ID <{}>", input.getClass().getCanonicalName(), input.getId());
                try {
                    input.checkConfiguration();
                    inputState.setState(InputState.InputStateType.STARTING);
                    input.launch(processBuffer);
                    inputState.setState(InputState.InputStateType.RUNNING);
                    String msg = "Completed starting [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + ">";
                    LOG.info(msg);
                } catch (MisfireException | Exception e) {
                    handleLaunchException(e, input, inputState);
                } finally {
                    finishedLaunch(inputState);
                }
            }
        });

        return inputState;
    }

    protected void handleLaunchException(Throwable e, MessageInput input, InputState inputState) {
        StringBuilder msg = new StringBuilder("The [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + "> misfired. Reason: ");

        String causeMsg = extractMessageCause(e);

        msg.append(causeMsg);

        LOG.error(msg.toString(), e);

        // Clean up.
        //cleanInput(input);

        inputState.setState(InputState.InputStateType.FAILED);
        inputState.setDetailedMessage(causeMsg);
    }

    private String extractMessageCause(Throwable e) {
        StringBuilder causeMsg = new StringBuilder(e.getMessage());

        // Go down the whole cause chain to build a message that provides as much information as possible.
        int maxLevel = 7; // ;)
        Throwable cause = e.getCause();
        for (int i = 0; i < maxLevel; i++) {
            if (cause == null) {
                break;
            }

            causeMsg.append(", ").append(cause.getMessage());
            cause = cause.getCause();
        }
        return causeMsg.toString();
    }

    public InputState launch(final MessageInput input) {
        return launch(input, UUID.randomUUID().toString());
    }

    public List<InputState> getInputStates() {
        return inputStates;
    }

    public List<InputState> getRunningInputs() {
        List<InputState> runningInputs = Lists.newArrayList();
        for (InputState inputState : inputStates) {
            if (inputState.getState() == InputState.InputStateType.RUNNING)
                runningInputs.add(inputState);
        }
        return inputStates;
    }

    public boolean hasTypeRunning(Class klazz) {
        for (InputState inputState : inputStates) {
            if (inputState.getMessageInput().getClass().equals(klazz)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, String> getAvailableInputs() {
        return messageInputFactory.getAvailableInputs();
    }

    public int runningCount() {
        return getRunningInputs().size();
    }

    public void removeFromRunning(MessageInput input) {
        // Remove from running list.
        InputState thisInputState = null;
        for (InputState inputState : inputStates) {
            if (inputState.getMessageInput().equals(input)) {
                thisInputState = inputState;
            }
        }
        inputStates.remove(thisInputState);
    }

    public InputState launchPersisted(MessageInput input) {
        return launch(input);
    }

    protected abstract List<MessageInput> getAllPersisted();

    public void launchAllPersisted() {
        for (MessageInput input : getAllPersisted()) {
            launchPersisted(input);
        }
    }

    public InputState terminate(MessageInput input) {
        InputState inputState = stop(input);

        if (inputState != null) {
            inputState.setState(InputState.InputStateType.TERMINATED);
            finishedTermination(inputState);
        }

        return inputState;
    }

    public InputState stop(MessageInput input) {
        InputState inputState = getRunningInputState(input.getId());

        if (inputState != null) {
            try {
                input.stop();
            } catch (Exception e) {
                LOG.warn("Stopping input <{}> failed, removing anyway: {}", input.getId(), e);
            }
            removeFromRunning(input);
            inputState.setState(InputState.InputStateType.STOPPED);
            finishedStop(inputState);
        }

        return inputState;
    }

    protected abstract void finishedStop(InputState inputState);

    public MessageInput getRunningInput(String inputId) {
        for (InputState inputState : inputStates) {
            if (inputState.getMessageInput().getId().equals(inputId))
                return inputState.getMessageInput();
        }

        return null;
    }

    public InputState getRunningInputState(String inputStateId) {
        for (InputState inputState : inputStates) {
            if (inputState.getMessageInput().getId().equals(inputStateId))
                return inputState;
        }

        return null;
    }

    public abstract void cleanInput(MessageInput input);

    public MessageInput getPersisted(String inputId) {
        for (MessageInput input : getAllPersisted()) {
            if (input.getId().equals(inputId))
                return input;
        }

        return null;
    }
}

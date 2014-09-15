/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.shared.inputs;


import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class InputRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(InputRegistry.class);
    protected final Set<InputState> inputStates = new HashSet<>();
    protected final ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("inputs-%d").build()
    );
    private final MessageInputFactory messageInputFactory;
    private final ProcessBuffer processBuffer;

    protected abstract void finishedLaunch(InputState state);

    protected abstract void finishedTermination(InputState state);

    protected abstract void finishedStop(InputState inputState);

    protected abstract List<MessageInput> getAllPersisted();

    public abstract void cleanInput(MessageInput input);

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

    public InputState launch(final MessageInput input, String id, boolean register) {
        final InputState inputState = new InputState(input, id);
        inputStates.add(inputState);

        return launch(input, inputState, register);
    }

    protected InputState launch(final MessageInput input, final InputState inputState, final boolean register) {
        if (input == null)
            throw new IllegalArgumentException("InputState has no MessageInput!");

        if (!inputState.getMessageInput().equals(input))
            throw new IllegalArgumentException("Supplied InputState already has Input which is not the one supplied.");

        if (inputState.getMessageInput() == null)
            inputState.setMessageInput(input);

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
                } catch (Exception e) {
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

    public InputState launch(final InputState inputState) {
        final MessageInput input = inputState.getMessageInput();

        return launch(input, inputState, false);
    }

    public Set<InputState> getInputStates() {
        return ImmutableSet.copyOf(inputStates);
    }

    public InputState getInputState(String inputId) {
        for (InputState inputState : inputStates) {
            if (inputState.getMessageInput().getPersistId().equals(inputId))
                return inputState;
        }

        return null;
    }

    public Set<InputState> getRunningInputs() {
        Set<InputState> runningInputs = new HashSet<>();
        for (InputState inputState : inputStates) {
            if (inputState.getState() == InputState.InputStateType.RUNNING)
                runningInputs.add(inputState);
        }
        return ImmutableSet.copyOf(runningInputs);
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

    public void removeFromRunning(InputState inputState) {
        inputStates.remove(inputState);
    }

    public InputState launchPersisted(MessageInput input) {
        return launch(input);
    }

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
            inputState.setState(InputState.InputStateType.STOPPED);
            finishedStop(inputState);
        }

        return inputState;
    }

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

    public MessageInput getPersisted(String inputId) {
        for (MessageInput input : getAllPersisted()) {
            if (input.getId().equals(inputId))
                return input;
        }

        return null;
    }
}

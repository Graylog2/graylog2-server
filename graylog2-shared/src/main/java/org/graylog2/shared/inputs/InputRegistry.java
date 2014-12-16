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
package org.graylog2.shared.inputs;


import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.InstrumentedThreadFactory;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public abstract class InputRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(InputRegistry.class);
    protected final Set<IOState<MessageInput>> inputStates = new HashSet<>();
    protected final ExecutorService executor;
    private IOState.Factory<MessageInput> inputStateFactory;
    private final MessageInputFactory messageInputFactory;
    private final InputBuffer inputBuffer;

    protected abstract List<MessageInput> getAllPersisted();

    public abstract void cleanInput(MessageInput input);

    public InputRegistry(IOState.Factory<MessageInput> inputStateFactory,
                         MessageInputFactory messageInputFactory,
                         InputBuffer inputBuffer,
                         MetricRegistry metricRegistry) {
        this.inputStateFactory = inputStateFactory;
        this.messageInputFactory = messageInputFactory;
        this.inputBuffer = inputBuffer;
        this.executor = executorService(metricRegistry);
    }

    private ExecutorService executorService(final MetricRegistry metricRegistry) {
        return new InstrumentedExecutorService(
                Executors.newCachedThreadPool(threadFactory(metricRegistry)), metricRegistry);
    }

    private ThreadFactory threadFactory(final MetricRegistry metricRegistry) {
        return new InstrumentedThreadFactory(
                new ThreadFactoryBuilder().setNameFormat("inputs-%d").build(),
                metricRegistry);
    }

    public MessageInput create(String inputClass, Configuration configuration) throws NoSuchInputTypeException {
        return messageInputFactory.create(inputClass, configuration);
    }

    public IOState<MessageInput> launch(final MessageInput input, String id) {
        return launch(input, id, false);
    }

    public IOState<MessageInput> launch(final MessageInput input, String id, boolean register) {
        final IOState<MessageInput> inputState = inputStateFactory.create(input, id);
        inputStates.add(inputState);

        return launch(input, inputState, register);
    }

    protected IOState<MessageInput> launch(final MessageInput input, final IOState<MessageInput> inputState, final boolean register) {
        if (input == null)
            throw new IllegalArgumentException("InputState has no MessageInput!");

        if (!inputState.getStoppable().equals(input))
            throw new IllegalArgumentException("Supplied InputState already has Input which is not the one supplied.");

        if (inputState.getStoppable() == null)
            inputState.setStoppable(input);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                LOG.info("Starting [{}] input with ID <{}>", input.getClass().getCanonicalName(), input.getId());
                try {
                    input.checkConfiguration();
                    inputState.setState(IOState.Type.STARTING);
                    input.launch(inputBuffer);
                    inputState.setState(IOState.Type.RUNNING);
                    String msg = "Completed starting [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + ">";
                    LOG.info(msg);
                } catch (Exception e) {
                    handleLaunchException(e, input, inputState);
                }
            }
        });

        return inputState;
    }

    protected void handleLaunchException(Throwable e, MessageInput input, IOState<MessageInput> inputState) {
        StringBuilder msg = new StringBuilder("The [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + "> misfired. Reason: ");

        String causeMsg = extractMessageCause(e);

        msg.append(causeMsg);

        LOG.error(msg.toString(), e);

        // Clean up.
        //cleanInput(input);

        inputState.setState(IOState.Type.FAILED);
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

    public IOState<MessageInput> launch(final MessageInput input) {
        return launch(input, UUID.randomUUID().toString());
    }

    public IOState<MessageInput> launch(final IOState<MessageInput> inputState) {
        final MessageInput input = inputState.getStoppable();

        return launch(input, inputState, false);
    }

    public Set<IOState<MessageInput>> getInputStates() {
        return ImmutableSet.copyOf(inputStates);
    }

    public IOState<MessageInput> getInputState(String inputId) {
        for (IOState<MessageInput> inputState : inputStates) {
            if (inputState.getStoppable().getPersistId().equals(inputId))
                return inputState;
        }

        return null;
    }

    public Set<IOState<MessageInput>> getRunningInputs() {
        Set<IOState<MessageInput>> runningInputs = new HashSet<>();
        for (IOState<MessageInput> inputState : inputStates) {
            if (inputState.getState() == IOState.Type.RUNNING)
                runningInputs.add(inputState);
        }
        return ImmutableSet.copyOf(runningInputs);
    }

    public boolean hasTypeRunning(Class klazz) {
        for (IOState<MessageInput> inputState : inputStates) {
            if (inputState.getStoppable().getClass().equals(klazz)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, InputDescription> getAvailableInputs() {
        return messageInputFactory.getAvailableInputs();
    }

    public int runningCount() {
        return getRunningInputs().size();
    }

    public void removeFromRunning(IOState<MessageInput> inputState) {
        inputStates.remove(inputState);
    }

    public IOState<MessageInput> launchPersisted(MessageInput input) {
        return launch(input);
    }

    public void launchAllPersisted() {
        for (MessageInput input : getAllPersisted()) {
            input.initialize();
            launchPersisted(input);
        }
    }

    public IOState<MessageInput> terminate(MessageInput input) {
        IOState<MessageInput> inputState = stop(input);

        if (inputState != null) {
            inputState.setState(IOState.Type.TERMINATED);
            removeFromRunning(inputState);
        }

        return inputState;
    }

    public IOState<MessageInput> stop(MessageInput input) {
        IOState<MessageInput> inputState = getRunningInputState(input.getId());

        if (inputState != null) {
            try {
                input.stop();
            } catch (Exception e) {
                LOG.warn("Stopping input <{}> failed, removing anyway: {}", input.getId(), e);
            }
            inputState.setState(IOState.Type.STOPPED);
        }

        return inputState;
    }

    public MessageInput getRunningInput(String inputId) {
        for (IOState<MessageInput> inputState : inputStates) {
            if (inputState.getStoppable().getId().equals(inputId))
                return inputState.getStoppable();
        }

        return null;
    }

    public IOState<MessageInput> getRunningInputState(String inputStateId) {
        for (IOState<MessageInput> inputState : inputStates) {
            if (inputState.getStoppable().getId().equals(inputStateId))
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
